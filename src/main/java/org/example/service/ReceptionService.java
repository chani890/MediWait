package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.*;
import org.example.model.*;
import org.example.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceptionService {
    
    private final PatientRepository patientRepository;
    private final ReceptionRepository receptionRepository;
    private final MedicalSurveyRepository medicalSurveyRepository;
    private final VitalSignRepository vitalSignRepository;
    private final PrescriptionRepository prescriptionRepository;
    private final RealtimeNotificationService realtimeNotificationService;
    private final WaitingQueueService waitingQueueService;
    
    @Transactional
    public ReceptionResponse registerPatient(PatientRegistrationRequest request) {
        log.info("환자 접수 시작: 이름={}, 전화번호={}", request.getName(), request.getPhoneNumber());
        
        // 환자 정보 조회 또는 생성
        Patient patient = patientRepository.findByNameAndPhoneNumber(request.getName(), request.getPhoneNumber())
            .orElse(null);
        
        boolean isNewPatient = (patient == null);
        
        if (isNewPatient) {
            patient = Patient.builder()
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .phoneNumber(request.getPhoneNumber())
                .build();
            patient = patientRepository.save(patient);
            log.info("신규 환자 등록: {}", patient.getName());
        } else {
            log.info("기존 환자 접수: {}", patient.getName());
        }
        
        // 접수 정보 생성 (기본값 설정)
        Reception reception = new Reception();
        reception.setPatient(patient);
        reception.setStatus(Reception.ReceptionStatus.PENDING);
        reception.setIsGuardian(false); // 기본값: 본인 접수
        reception.setSmsNotificationEnabled(true); // 기본값: SMS 알림 활성화
        reception.setSmsSent(false);
        reception.setCreatedAt(LocalDateTime.now());
        
        reception = receptionRepository.save(reception);
        log.info("접수 등록 완료: 환자 {} (접수 ID: {})", patient.getName(), reception.getId());

        // 문진표 데이터가 있으면 저장
        boolean hasSurvey = false;
        if (request.getVisitReason() != null || request.getSymptoms() != null || request.getAllergies() != null || request.getMedications() != null || request.getMedicalHistory() != null) {
            MedicalSurvey survey = MedicalSurvey.builder()
                .patient(patient)
                .reception(reception)
                .visitReason(request.getVisitReason())
                .symptoms(request.getSymptoms())
                .allergies(request.getAllergies())
                .medications(request.getMedications())
                .medicalHistory(request.getMedicalHistory())
                .build();
            medicalSurveyRepository.save(survey);
            hasSurvey = true;
            log.info("문진표 저장 완료: 환자 {} (접수 ID: {})", patient.getName(), reception.getId());
        }
        
        // 현재 대기 순번 계산
        int waitingPosition = calculateWaitingPosition(reception);
        
        // 실시간 알림 전송
        realtimeNotificationService.notifyNewReception(reception.getId(), patient.getName());
        
        return ReceptionResponse.builder()
            .id(reception.getId())
            .patientId(patient.getId())
            .patientName(patient.getName())
            .phoneNumber(patient.getPhoneNumber())
            .status(reception.getStatus())
            .isGuardian(reception.getIsGuardian())
            .createdAt(reception.getCreatedAt())
            .waitingPosition(waitingPosition)
            .isNewPatient(isNewPatient)
            .hasSurvey(hasSurvey)
            .build();
    }
    
    @Transactional
    public ReceptionResponse confirmReception(Long receptionId) {
        Reception reception = receptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("접수를 찾을 수 없습니다."));
        
        if (reception.getStatus() != Reception.ReceptionStatus.PENDING) {
            throw new RuntimeException("이미 처리된 접수입니다.");
        }
        
        reception.setStatus(Reception.ReceptionStatus.CONFIRMED);
        reception.setConfirmedAt(LocalDateTime.now());
        reception = receptionRepository.save(reception);
        
        log.info("접수 확인 완료: 환자 {} (접수 ID: {})", reception.getPatient().getName(), reception.getId());
        
        // 실시간 알림 전송
        realtimeNotificationService.notifyPatientStatusChange(reception.getId(), "PENDING", "CONFIRMED");
        
        // SMS 알림 체크 및 발송
        waitingQueueService.checkAndSendSmsNotifications();
        
        return convertToReceptionResponse(reception);
    }
    
    @Transactional
    public PatientInfoResponse updatePatientInfo(Long patientId, String name, String birthDateStr, String phoneNumber) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("환자를 찾을 수 없습니다."));
        if (name != null && !name.trim().isEmpty()) {
            patient.setName(name.trim());
        }
        if (birthDateStr != null && !birthDateStr.trim().isEmpty()) {
            try {
                LocalDate birthDate = LocalDate.parse(birthDateStr);
                patient.setBirthDate(birthDate);
            } catch (Exception e) {
                throw new RuntimeException("생년월일 형식이 올바르지 않습니다. (예: 1990-01-15)");
            }
        }
        if (phoneNumber != null) {
            patient.setPhoneNumber(phoneNumber.trim());
        }
        patientRepository.save(patient);
        return getPatientInfo(patientId);
    }
    
    @Transactional
    public PatientInfoResponse callNextPatient() {
        log.info("환자 호출 요청 시작");
        
        // 현재 호출된 환자가 있는지 확인
        int currentCalledCount = receptionRepository.countCalledReceptions();
        if (currentCalledCount > 0) {
            List<Reception> calledReceptions = receptionRepository.findCalledReceptions();
            String currentPatientNames = calledReceptions.stream()
                .map(r -> r.getPatient().getName())
                .collect(java.util.stream.Collectors.joining(", "));
            
            log.warn("이미 호출된 환자가 있습니다: {}", currentPatientNames);
            throw new RuntimeException("이미 호출된 환자가 있습니다: " + currentPatientNames + ". 현재 환자의 진료를 완료한 후 다음 환자를 호출해주세요.");
        }
        
        // 락을 사용하여 다음 호출 가능한 환자 조회
        List<Reception> confirmedReceptions = receptionRepository.findNextPatientToCallWithLock();
        
        if (confirmedReceptions.isEmpty()) {
            log.warn("호출할 환자가 없습니다.");
            throw new RuntimeException("호출할 환자가 없습니다.");
        }
        
        Reception reception = confirmedReceptions.get(0);
        Long receptionId = reception.getId();
        LocalDateTime calledAt = LocalDateTime.now();
        
        // 원자적으로 상태 변경 (CONFIRMED -> CALLED)
        int updatedRows = receptionRepository.updateStatusToCalledIfConfirmed(receptionId, calledAt);
        
        if (updatedRows == 0) {
            log.warn("환자 호출 실패: 이미 호출된 환자이거나 상태가 변경됨 (접수 ID: {})", receptionId);
            throw new RuntimeException("이미 호출된 환자이거나 상태가 변경되었습니다. 다시 시도해주세요.");
        }
        
        // 업데이트된 Reception 엔티티 다시 조회
        reception = receptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("접수 정보를 찾을 수 없습니다."));
        
        log.info("환자 호출 성공: {} (접수 ID: {})", reception.getPatient().getName(), reception.getId());
        
        // 실시간 알림 전송
        realtimeNotificationService.notifyDoctorCall(reception.getId(), reception.getPatient().getName());
        
        // SMS 상태 초기화 (다음 환자들의 SMS 발송을 위해)
        waitingQueueService.resetSmsStatusForReception(reception.getId());
        
        // 대기열 변경으로 인한 SMS 알림 체크
        waitingQueueService.checkAndSendSmsNotifications();
        
        // 환자 정보와 문진표, 과거 이력 조회
        PatientInfoResponse patientInfo = getPatientInfo(reception.getPatient().getId());
        
        // 업데이트된 접수 정보를 다시 조회하여 정확한 상태 반영
        Reception updatedReception = receptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("접수 정보를 찾을 수 없습니다."));
        
        // 호출된 접수 정보를 직접 설정 (JPA 지연 로딩 문제 해결)
        patientInfo.setCurrentReception(convertToReceptionResponse(updatedReception));
        patientInfo.setCurrentReceptionId(updatedReception.getId());
        
        return patientInfo;
    }
    
    @Transactional
    public ReceptionResponse completeReception(Long receptionId) {
        Reception reception = receptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("접수를 찾을 수 없습니다."));
        
        if (reception.getStatus() != Reception.ReceptionStatus.CALLED) {
            throw new RuntimeException("호출된 환자만 진료 완료 처리할 수 있습니다.");
        }
        
        reception.setStatus(Reception.ReceptionStatus.COMPLETED);
        reception.setCompletedAt(LocalDateTime.now());
        reception = receptionRepository.save(reception);
        
        log.info("진료 완료: 환자 {} (접수 ID: {})", reception.getPatient().getName(), reception.getId());
        
        // 실시간 알림 전송
        realtimeNotificationService.notifyPatientStatusChange(reception.getId(), "CALLED", "COMPLETED");
        
        // 대기열 업데이트 알림 전송 (의사 화면의 현재 환자 목록 업데이트용)
        realtimeNotificationService.notifyWaitingQueueUpdate();
        
        return convertToReceptionResponse(reception);
    }
    
    @Transactional(readOnly = true)
    public List<ReceptionResponse> getPendingReceptions() {
        List<Reception> pendingReceptions = receptionRepository.findByStatusOrderByCreatedAtAsc(Reception.ReceptionStatus.PENDING);
        return pendingReceptions.stream()
            .map(this::convertToReceptionResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReceptionResponse> getConfirmedReceptions() {
        List<Reception> confirmedReceptions = receptionRepository.findByStatusOrderByConfirmedAtAsc(Reception.ReceptionStatus.CONFIRMED);
        return confirmedReceptions.stream()
            .map(this::convertToReceptionResponse)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public ReceptionResponse deleteReception(Long receptionId) {
        Reception reception = receptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("접수를 찾을 수 없습니다."));
        
        // 이미 진료 완료된 접수는 삭제할 수 없음
        if (reception.getStatus() == Reception.ReceptionStatus.COMPLETED) {
            throw new RuntimeException("이미 진료 완료된 접수는 삭제할 수 없습니다.");
        }
        
        // 현재 진료 중인 환자는 삭제할 수 없음
        if (reception.getStatus() == Reception.ReceptionStatus.CALLED) {
            throw new RuntimeException("현재 진료 중인 환자는 삭제할 수 없습니다.");
        }
        
        // 삭제 전 응답 객체 생성
        ReceptionResponse response = convertToReceptionResponse(reception);
        
        // 관련 문진표 먼저 삭제
        medicalSurveyRepository.deleteByReceptionId(receptionId);
        
        // 관련 바이탈사인 삭제
        vitalSignRepository.deleteByReceptionId(receptionId);
        
        // 접수 삭제
        receptionRepository.delete(reception);
        
        log.info("접수 삭제 완료: 환자 {} (접수 ID: {})", reception.getPatient().getName(), reception.getId());
        
        // 실시간 알림 전송 (대기열 업데이트)
        realtimeNotificationService.notifyWaitingQueueUpdate();
        
        return response;
    }
    
    /**
     * 진료 중인 환자를 포함한 모든 접수 강제 삭제 (관리자용)
     */
    @Transactional
    public ReceptionResponse forceDeleteReception(Long receptionId) {
        Reception reception = receptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("접수를 찾을 수 없습니다."));
        
        // 이미 진료 완료된 접수는 삭제할 수 없음
        if (reception.getStatus() == Reception.ReceptionStatus.COMPLETED) {
            throw new RuntimeException("이미 진료 완료된 접수는 삭제할 수 없습니다.");
        }
        
        // 삭제 전 응답 객체 생성
        ReceptionResponse response = convertToReceptionResponse(reception);
        
        // 관련 처방전 먼저 삭제 (진료 중인 환자의 경우)
        if (reception.getStatus() == Reception.ReceptionStatus.CALLED) {
            prescriptionRepository.deleteByReceptionId(receptionId);
            log.info("관련 처방전 삭제 완료: 접수 ID {}", receptionId);
        }
        
        // 관련 문진표 삭제
        medicalSurveyRepository.deleteByReceptionId(receptionId);
        
        // 관련 바이탈사인 삭제
        vitalSignRepository.deleteByReceptionId(receptionId);
        
        // 접수 삭제
        receptionRepository.delete(reception);
        
        log.info("접수 강제 삭제 완료: 환자 {} (접수 ID: {}, 상태: {})", 
                reception.getPatient().getName(), reception.getId(), reception.getStatus());
        
        // 실시간 알림 전송 (대기열 업데이트)
        realtimeNotificationService.notifyWaitingQueueUpdate();
        
        return response;
    }
    
    @Transactional(readOnly = true)
    public List<PatientInfoResponse> getCalledPatients() {
        List<Reception> calledReceptions = receptionRepository.findCalledReceptions();
        return calledReceptions.stream()
            .map(reception -> getPatientInfo(reception.getPatient().getId()))
            .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public int getCurrentWaitingCount() {
        return (int) receptionRepository.countByStatus(Reception.ReceptionStatus.CONFIRMED);
    }
    
    @Transactional(readOnly = true)
    public int getWaitingPosition(Long receptionId) {
        Reception reception = receptionRepository.findById(receptionId).orElse(null);
        if (reception == null) {
            return 0;
        }
        
        // PENDING 상태인 경우: CONFIRMED 환자 수 + PENDING 목록에서의 순서
        if (reception.getStatus() == Reception.ReceptionStatus.PENDING) {
            // CONFIRMED된 환자 수 계산
            int confirmedCount = (int) receptionRepository.countByStatus(Reception.ReceptionStatus.CONFIRMED);
            
            // PENDING 목록에서의 순서 계산
            List<Reception> pendingReceptions = receptionRepository.findByStatusOrderByCreatedAtAsc(Reception.ReceptionStatus.PENDING);
            for (int i = 0; i < pendingReceptions.size(); i++) {
                if (pendingReceptions.get(i).getId().equals(receptionId)) {
                    return confirmedCount + i + 1;
                }
            }
        }
        
        // CONFIRMED 상태인 경우 확인된 순서대로 대기 순번 계산
        if (reception.getStatus() == Reception.ReceptionStatus.CONFIRMED) {
            List<Reception> confirmedReceptions = receptionRepository.findByStatusOrderByConfirmedAtAsc(Reception.ReceptionStatus.CONFIRMED);
            for (int i = 0; i < confirmedReceptions.size(); i++) {
                if (confirmedReceptions.get(i).getId().equals(receptionId)) {
                    return i + 1;
                }
            }
        }
        
        // CALLED 또는 COMPLETED 상태인 경우 대기 순번 없음
        return 0;
    }
    
    @Transactional(readOnly = true)
    public Reception findById(Long receptionId) {
        log.info("Finding reception by ID: {}", receptionId);
        Reception reception = receptionRepository.findByIdWithPatient(receptionId).orElse(null);
        if (reception != null) {
            log.info("Reception found with patient: {}", reception.getPatient().getName());
        } else {
            log.warn("Reception not found for ID: {}", receptionId);
        }
        return reception;
    }
    
    @Transactional(readOnly = true)
    public PatientInfoResponse getPatientInfo(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("환자를 찾을 수 없습니다."));
        
        // 현재 호출된 접수 조회 (CALLED 상태)
        Optional<Reception> currentReception = patient.getReceptions().stream()
            .filter(r -> r.getStatus() == Reception.ReceptionStatus.CALLED)
            .findFirst();
        
        // 현재 접수에 해당하는 문진표 조회 (우선순위)
        MedicalSurveyResponse latestSurvey = null;
        if (currentReception.isPresent()) {
            // 현재 접수에 해당하는 문진표가 있는지 확인
            Optional<MedicalSurvey> currentSurvey = medicalSurveyRepository.findByReception(currentReception.get());
            if (currentSurvey.isPresent()) {
                latestSurvey = convertToMedicalSurveyResponse(currentSurvey.get());
                log.info("현재 접수({})에 해당하는 문진표 조회 완료: 환자 {}", 
                        currentReception.get().getId(), patient.getName());
            }
        }
        
        // 현재 접수에 문진표가 없으면 가장 최근 문진표 조회
        if (latestSurvey == null) {
            List<MedicalSurvey> surveys = medicalSurveyRepository.findByPatientOrderByCreatedAtDesc(patient);
            if (!surveys.isEmpty()) {
                latestSurvey = convertToMedicalSurveyResponse(surveys.get(0));
                log.info("최근 문진표 조회 완료: 환자 {} (문진표 ID: {})", 
                        patient.getName(), surveys.get(0).getId());
            } else {
                log.info("문진표 없음: 환자 {}", patient.getName());
            }
        }
        
        // 과거 진료 이력 조회
        List<ReceptionResponse> pastReceptions = patient.getReceptions().stream()
            .filter(r -> r.getStatus() == Reception.ReceptionStatus.COMPLETED)
            .map(this::convertToReceptionResponse)
            .collect(Collectors.toList());
        
        return PatientInfoResponse.builder()
            .patientId(patient.getId())
            .name(patient.getName())
            .birthDate(patient.getBirthDate())
            .phoneNumber(patient.getPhoneNumber())
            .createdAt(patient.getCreatedAt())
            .currentReception(currentReception.map(this::convertToReceptionResponse).orElse(null))
            .currentReceptionId(currentReception.map(Reception::getId).orElse(null))
            .latestSurvey(latestSurvey)
            .pastReceptions(pastReceptions)
            .totalVisits(pastReceptions.size())
            .lastVisit(pastReceptions.isEmpty() ? null : pastReceptions.get(0).getCompletedAt())
            .build();
    }
    
    @Transactional(readOnly = true)
    public List<PatientInfoResponse> searchPatientsByName(String name) {
        List<Patient> patients = patientRepository.findByNameContainingOrderByCreatedAtDesc(name);
        return patients.stream()
                .map(this::convertToPatientInfoResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public VitalSignResponse saveVitalSign(VitalSignRequest request) {
        // 접수 확인
        Reception reception = receptionRepository.findById(request.getReceptionId())
                .orElseThrow(() -> new RuntimeException("접수를 찾을 수 없습니다."));
        
        // 기존 바이탈사인이 있는지 확인
        Optional<VitalSign> existingVitalSign = vitalSignRepository.findByReceptionId(request.getReceptionId());
        
        VitalSign vitalSign;
        if (existingVitalSign.isPresent()) {
            // 기존 바이탈사인 업데이트
            vitalSign = existingVitalSign.get();
            vitalSign.setBodyTemp(request.getBodyTemp());
            vitalSign.setBloodPressure(request.getBloodPressure());
            vitalSign.setPulse(request.getPulse());
            vitalSign.setSymptoms(request.getSymptoms());
            vitalSign.setOtherSymptoms(request.getOtherSymptoms());
            vitalSign.setMedicalHistory(request.getMedicalHistory());
            vitalSign.setNurseNotes(request.getNurseNotes());
            vitalSign.setNurseId(request.getNurseId());
        } else {
            // 새로운 바이탈사인 생성
            vitalSign = VitalSign.builder()
                    .reception(reception)
                    .bodyTemp(request.getBodyTemp())
                    .bloodPressure(request.getBloodPressure())
                    .pulse(request.getPulse())
                    .symptoms(request.getSymptoms())
                    .otherSymptoms(request.getOtherSymptoms())
                    .medicalHistory(request.getMedicalHistory())
                    .nurseNotes(request.getNurseNotes())
                    .nurseId(request.getNurseId())
                    .build();
        }
        
        vitalSign = vitalSignRepository.save(vitalSign);
        
        log.info("바이탈사인 저장 완료: 접수 ID {}, 환자 {}", request.getReceptionId(), reception.getPatient().getName());
        
        return convertToVitalSignResponse(vitalSign);
    }
    
    @Transactional(readOnly = true)
    public VitalSignResponse getVitalSign(Long receptionId) {
        Optional<VitalSign> vitalSignOpt = vitalSignRepository.findByReceptionId(receptionId);
        
        if (!vitalSignOpt.isPresent()) {
            // 바이탈 사인 데이터가 없을 때 빈 응답 반환
            Reception reception = receptionRepository.findById(receptionId)
                    .orElseThrow(() -> new RuntimeException("접수를 찾을 수 없습니다."));
            
            return VitalSignResponse.builder()
                    .id(null)
                    .receptionId(receptionId)
                    .patientName(reception.getPatient().getName())
                    .bodyTemp(null)
                    .bloodPressure(null)
                    .pulse(null)
                    .symptoms(null)
                    .otherSymptoms(null)
                    .medicalHistory(null)
                    .nurseNotes(null)
                    .nurseId(null)
                    .createdAt(null)
                    .build();
        }
        
        return convertToVitalSignResponse(vitalSignOpt.get());
    }
    
    private PatientInfoResponse convertToPatientInfoResponse(Patient patient) {
        // 최근 문진표 조회
        List<MedicalSurvey> surveys = medicalSurveyRepository.findByPatientOrderByCreatedAtDesc(patient);
        MedicalSurveyResponse latestSurvey = surveys.isEmpty() ? null : convertToMedicalSurveyResponse(surveys.get(0));
        
        // 과거 진료 이력 조회
        List<ReceptionResponse> pastReceptions = patient.getReceptions().stream()
            .filter(r -> r.getStatus() == Reception.ReceptionStatus.COMPLETED)
            .sorted((r1, r2) -> r2.getCompletedAt().compareTo(r1.getCompletedAt())) // 최신순 정렬
            .map(this::convertToReceptionResponse)
            .collect(Collectors.toList());
        
        return PatientInfoResponse.builder()
            .patientId(patient.getId())
            .name(patient.getName())
            .birthDate(patient.getBirthDate())
            .phoneNumber(patient.getPhoneNumber())
            .createdAt(patient.getCreatedAt())
            .currentReception(null) // 검색 시에는 현재 접수 정보 불필요
            .latestSurvey(latestSurvey)
            .pastReceptions(pastReceptions)
            .totalVisits(pastReceptions.size())
            .lastVisit(pastReceptions.isEmpty() ? null : pastReceptions.get(0).getCompletedAt())
            .build();
    }
    
    private boolean hasMedicalSurveyData(PatientRegistrationRequest request) {
        // 문진표 데이터는 별도 처리하므로 항상 false 반환
        return false;
    }
    
    private int calculateWaitingPosition(Reception reception) {
        // PENDING 상태인 경우: CONFIRMED 환자 수 + PENDING 목록에서의 순서
        if (reception.getStatus() == Reception.ReceptionStatus.PENDING) {
            // CONFIRMED된 환자 수 계산
            int confirmedCount = (int) receptionRepository.countByStatus(Reception.ReceptionStatus.CONFIRMED);
            
            // PENDING 목록에서의 순서 계산
            List<Reception> pendingReceptions = receptionRepository.findByStatusOrderByCreatedAtAsc(Reception.ReceptionStatus.PENDING);
            for (int i = 0; i < pendingReceptions.size(); i++) {
                if (pendingReceptions.get(i).getId().equals(reception.getId())) {
                    return confirmedCount + i + 1;
                }
            }
        }
        
        // CONFIRMED 상태인 경우 확인된 순서대로 대기 순번 계산
        if (reception.getStatus() == Reception.ReceptionStatus.CONFIRMED) {
            List<Reception> confirmedReceptions = receptionRepository.findByStatusOrderByConfirmedAtAsc(Reception.ReceptionStatus.CONFIRMED);
            for (int i = 0; i < confirmedReceptions.size(); i++) {
                if (confirmedReceptions.get(i).getId().equals(reception.getId())) {
                    return i + 1;
                }
            }
        }
        
        // CALLED 또는 COMPLETED 상태인 경우 대기 순번 없음
        return 0;
    }
    
    private ReceptionResponse convertToReceptionResponse(Reception reception) {
        return ReceptionResponse.builder()
            .id(reception.getId())
            .patientId(reception.getPatient().getId())
            .patientName(reception.getPatient().getName())
            .birthDate(reception.getPatient().getBirthDate()) // 생년월일 추가
            .phoneNumber(reception.getPatient().getPhoneNumber())
            .status(reception.getStatus())
            .isGuardian(reception.getIsGuardian())
            .createdAt(reception.getCreatedAt())
            .confirmedAt(reception.getConfirmedAt())
            .calledAt(reception.getCalledAt())
            .completedAt(reception.getCompletedAt())
            .waitingPosition(calculateWaitingPosition(reception))
            .build();
    }
    
    private MedicalSurveyResponse convertToMedicalSurveyResponse(MedicalSurvey survey) {
        return MedicalSurveyResponse.builder()
            .id(survey.getId())
            .patientId(survey.getPatient().getId())
            .receptionId(survey.getReception().getId())
            .symptoms(survey.getSymptoms())
            .allergies(survey.getAllergies())
            .medications(survey.getMedications())
            .medicalHistory(survey.getMedicalHistory())
            .visitReason(survey.getVisitReason())
            .createdAt(survey.getCreatedAt())
            .build();
    }
    
    private VitalSignResponse convertToVitalSignResponse(VitalSign vitalSign) {
        return VitalSignResponse.builder()
                .id(vitalSign.getId())
                .receptionId(vitalSign.getReception().getId())
                .patientName(vitalSign.getReception().getPatient().getName())
                .bodyTemp(vitalSign.getBodyTemp())
                .bloodPressure(vitalSign.getBloodPressure())
                .pulse(vitalSign.getPulse())
                .symptoms(vitalSign.getSymptoms())
                .otherSymptoms(vitalSign.getOtherSymptoms())
                .medicalHistory(vitalSign.getMedicalHistory())
                .nurseNotes(vitalSign.getNurseNotes())
                .nurseId(vitalSign.getNurseId())
                .createdAt(vitalSign.getCreatedAt())
                .build();
    }
    
    /**
     * SMS 알림 설정 업데이트
     */
    @Transactional
    public void updateSmsNotification(Long receptionId, Boolean enabled) {
        Reception reception = receptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("접수를 찾을 수 없습니다."));
        
        reception.setSmsNotificationEnabled(enabled);
        receptionRepository.save(reception);
        
        log.info("SMS 알림 설정 업데이트: 접수 ID {}, 활성화 여부 {}", receptionId, enabled);
        
        // SMS 설정 변경 후 대기열 체크
        if (Boolean.TRUE.equals(enabled)) {
            waitingQueueService.checkAndSendSmsNotifications();
        }
    }

    public Reception save(Reception reception) {
        return receptionRepository.save(reception);
    }
} 