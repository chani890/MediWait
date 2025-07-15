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
    private final RealtimeNotificationService realtimeNotificationService;
    private final SmsService smsService;
    
    @Transactional
    public ReceptionResponse registerPatient(PatientRegistrationRequest request) {
        // 기존 환자 확인 - 우선 이름과 생년월일로 검색
        Optional<Patient> existingPatient = patientRepository.findByNameAndBirthDate(
            request.getName(), request.getBirthDate());
        
        Patient patient;
        boolean isNewPatient = false;
        
        if (existingPatient.isPresent()) {
            patient = existingPatient.get();
            
            // 기존 환자의 전화번호 업데이트 (기존에 null이었고 새로 입력된 경우)
            if (patient.getPhoneNumber() == null && 
                request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty()) {
                patient.setPhoneNumber(request.getPhoneNumber());
                patient = patientRepository.save(patient);
                log.info("기존 환자 전화번호 업데이트: {}", patient.getName());
            }
            
            log.info("기존 환자 접수: {}", patient.getName());
        } else {
            // 신규 환자 등록
            patient = Patient.builder()
                .name(request.getName())
                .birthDate(request.getBirthDate())
                .phoneNumber(request.getPhoneNumber() != null && !request.getPhoneNumber().trim().isEmpty() 
                    ? request.getPhoneNumber() : null)
                .build();
            patient = patientRepository.save(patient);
            isNewPatient = true;
            log.info("신규 환자 등록: {}", patient.getName());
        }
        
        // 접수 생성
        Reception reception = Reception.builder()
            .patient(patient)
            .status(Reception.ReceptionStatus.PENDING)
            .isGuardian(request.getIsGuardian())
            .notifyEnabled(request.getNotifyEnabled())
            .notifyAt(request.getNotifyAt())
            .build();
        reception = receptionRepository.save(reception);
        
        // 신규 환자인 경우 문진표 저장
        boolean hasSurvey = false;
        if (isNewPatient && hasMedicalSurveyData(request)) {
            MedicalSurvey survey = MedicalSurvey.builder()
                .patient(patient)
                .reception(reception)
                .symptoms(request.getSymptoms())
                .allergies(request.getAllergies())
                .medications(request.getMedications())
                .medicalHistory(request.getMedicalHistory())
                .visitReason(request.getVisitReason())
                .build();
            medicalSurveyRepository.save(survey);
            hasSurvey = true;
            log.info("문진표 저장 완료: 환자 ID {}", patient.getId());
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
            .notifyAt(reception.getNotifyAt())
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
        
        // SMS 알림 발송 로직 (2팀 전에 알림)
        checkAndSendSmsNotifications();
        
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
        // 확인된 환자 중 가장 먼저 확인된 환자 호출
        List<Reception> confirmedReceptions = receptionRepository.findByStatusOrderByConfirmedAtAsc(Reception.ReceptionStatus.CONFIRMED);
        
        if (confirmedReceptions.isEmpty()) {
            throw new RuntimeException("호출할 환자가 없습니다.");
        }
        
        Reception reception = confirmedReceptions.get(0);
        reception.setStatus(Reception.ReceptionStatus.CALLED);
        reception.setCalledAt(LocalDateTime.now());
        reception = receptionRepository.save(reception);
        
        log.info("환자 호출: {} (접수 ID: {})", reception.getPatient().getName(), reception.getId());
        
        // 호출된 환자에게 SMS 발송
        sendCallSmsNotification(reception);
        
        // 실시간 알림 전송
        realtimeNotificationService.notifyDoctorCall(reception.getId(), reception.getPatient().getName());
        
        // 대기열 현황 페이지에 환자 호출 메시지 전송
        realtimeNotificationService.notifyPatientCall(reception.getPatient().getName());
        
        // 환자 호출 후 대기 순서 변경으로 인한 SMS 알림 발송
        checkAndSendSmsNotifications();
        
        // 환자 정보와 문진표, 과거 이력 조회
        return getPatientInfo(reception.getPatient().getId());
    }
    
    @Transactional
    public ReceptionResponse completeReception(Long receptionId) {
        Reception reception = receptionRepository.findById(receptionId)
            .orElseThrow(() -> new RuntimeException("접수를 찾을 수 없습니다."));
        
        if (reception.getStatus() != Reception.ReceptionStatus.CALLED) {
            throw new RuntimeException("호출된 환자만 진료 완료 처리할 수 있습니다.");
        }
        
        reception.setStatus(Reception.ReceptionStatus.DONE);
        reception.setCompletedAt(LocalDateTime.now());
        reception = receptionRepository.save(reception);
        
        log.info("진료 완료: 환자 {} (접수 ID: {})", reception.getPatient().getName(), reception.getId());
        
        // 실시간 알림 전송
        realtimeNotificationService.notifyPatientStatusChange(reception.getId(), "CALLED", "DONE");
        
        // 대기열 업데이트 알림 전송 (의사 화면의 현재 환자 목록 업데이트용)
        realtimeNotificationService.notifyWaitingQueueUpdate();
        
        // SMS 발송 기록 초기화
        smsService.resetSentStatus(reception.getId());
        
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
        if (reception.getStatus() == Reception.ReceptionStatus.DONE) {
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
        
        // SMS 발송 기록 초기화
        smsService.resetSentStatus(reception.getId());
        
        return response;
    }
    
    @Transactional(readOnly = true)
    public List<PatientInfoResponse> getCalledPatients() {
        List<Reception> calledReceptions = receptionRepository.findByStatusOrderByCalledAtAsc(Reception.ReceptionStatus.CALLED);
        return calledReceptions.stream()
            .map(reception -> {
                PatientInfoResponse patientInfo = getPatientInfo(reception.getPatient().getId());
                patientInfo.setCurrentReceptionId(reception.getId());
                return patientInfo;
            })
            .collect(Collectors.toList());
    }
    
    public int getCurrentWaitingCount() {
        return receptionRepository.findByStatusOrderByConfirmedAtAsc(Reception.ReceptionStatus.CONFIRMED).size();
    }
    
    @Transactional(readOnly = true)
    public int getWaitingPosition(Long receptionId) {
        Reception reception = receptionRepository.findById(receptionId).orElse(null);
        if (reception == null || reception.getStatus() != Reception.ReceptionStatus.CONFIRMED) {
            return 0;
        }
        
        // 해당 접수보다 먼저 확인된 접수들의 개수 + 1
        List<Reception> confirmedReceptions = receptionRepository.findByStatusOrderByConfirmedAtAsc(Reception.ReceptionStatus.CONFIRMED);
        for (int i = 0; i < confirmedReceptions.size(); i++) {
            if (confirmedReceptions.get(i).getId().equals(receptionId)) {
                return i + 1;
            }
        }
        return 0;
    }
    
    @Transactional(readOnly = true)
    public Reception findById(Long receptionId) {
        log.info("Finding reception by ID: {}", receptionId);
        Reception reception = receptionRepository.findById(receptionId).orElse(null);
        if (reception == null) {
            log.warn("Reception not found for ID: {}", receptionId);
        } else {
            // LAZY 로딩 강제 초기화
            if (reception.getPatient() != null) {
                reception.getPatient().getName(); // 강제로 Patient 로딩
            }
        }
        return reception;
    }
    
    @Transactional(readOnly = true)
    public List<Reception> findConfirmedReceptions() {
        return receptionRepository.findByStatusOrderByConfirmedAtAsc(Reception.ReceptionStatus.CONFIRMED);
    }
    
    @Transactional(readOnly = true)
    public PatientInfoResponse getPatientInfo(Long patientId) {
        Patient patient = patientRepository.findById(patientId)
            .orElseThrow(() -> new RuntimeException("환자를 찾을 수 없습니다."));
        
        // 현재 호출된 접수 조회 (CALLED 상태)
        Optional<Reception> currentReception = patient.getReceptions().stream()
            .filter(r -> r.getStatus() == Reception.ReceptionStatus.CALLED)
            .findFirst();
        
        // 최근 문진표 조회
        List<MedicalSurvey> surveys = medicalSurveyRepository.findByPatientOrderByCreatedAtDesc(patient);
        MedicalSurveyResponse latestSurvey = surveys.isEmpty() ? null : convertToMedicalSurveyResponse(surveys.get(0));
        
        // 과거 진료 이력 조회
        List<ReceptionResponse> pastReceptions = patient.getReceptions().stream()
            .filter(r -> r.getStatus() == Reception.ReceptionStatus.DONE)
            .map(this::convertToReceptionResponse)
            .collect(Collectors.toList());
        
        return PatientInfoResponse.builder()
            .patientId(patient.getId())
            .name(patient.getName())
            .birthDate(patient.getBirthDate())
            .phoneNumber(patient.getPhoneNumber())
            .createdAt(patient.getCreatedAt())
            .currentReception(currentReception.map(this::convertToReceptionResponse).orElse(null))
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
            .filter(r -> r.getStatus() == Reception.ReceptionStatus.DONE)
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
        return request.getSymptoms() != null || request.getAllergies() != null ||
               request.getMedications() != null || request.getMedicalHistory() != null ||
               request.getVisitReason() != null;
    }
    
    private int calculateWaitingPosition(Reception reception) {
        return receptionRepository.countConfirmedBefore(reception.getCreatedAt()) + 1;
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
            .notifyAt(reception.getNotifyAt())
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
    
    /**
     * 호출된 환자에게 SMS 발송
     */
    private void sendCallSmsNotification(Reception reception) {
        try {
            if (reception.getNotifyEnabled()) {
                Patient patient = reception.getPatient();
                String phoneNumber = patient.getPhoneNumber();
                
                if (smsService.isValidPhoneNumber(phoneNumber)) {
                    String formattedPhoneNumber = smsService.formatPhoneNumber(phoneNumber);
                    boolean smsSent = smsService.sendCallNotification(formattedPhoneNumber, patient.getName());
                    
                    if (smsSent) {
                        log.info("호출 SMS 발송 완료: 환자 {} (전화번호: {})", patient.getName(), phoneNumber);
                    } else {
                        log.warn("호출 SMS 발송 실패: 환자 {} (전화번호: {})", patient.getName(), phoneNumber);
                    }
                } else {
                    log.warn("유효하지 않은 전화번호로 호출 SMS 발송 불가: 환자 {} (전화번호: {})", 
                            patient.getName(), phoneNumber);
                }
            } else {
                log.debug("SMS 알림이 비활성화된 환자: {}", reception.getPatient().getName());
            }
        } catch (Exception e) {
            log.error("호출 SMS 발송 중 오류 발생: 환자 {} - {}", reception.getPatient().getName(), e.getMessage(), e);
        }
    }
    
    /**
     * SMS 알림 발송 체크 및 실행
     * 2팀 전에 있는 환자들에게 SMS 알림 발송
     */
    private void checkAndSendSmsNotifications() {
        try {
            // 현재 확인된 대기 환자들 조회 (확인 시간 순)
            List<Reception> confirmedReceptions = receptionRepository.findByStatusOrderByConfirmedAtAsc(Reception.ReceptionStatus.CONFIRMED);
            
            // 대기 환자가 3명 이상일 때만 SMS 발송 (3번째부터 알림)
            if (confirmedReceptions.size() >= 3) {
                // 3번째 환자부터 SMS 발송 (2팀 전에 알림)
                for (int i = 2; i < confirmedReceptions.size(); i++) {
                    Reception reception = confirmedReceptions.get(i);
                    
                    // SMS 알림이 활성화되어 있고, 아직 발송하지 않은 경우
                    if (reception.getNotifyEnabled() && !smsService.isAlreadySent(reception.getId())) {
                        Patient patient = reception.getPatient();
                        String phoneNumber = patient.getPhoneNumber();
                        
                        // 전화번호가 유효한 경우에만 SMS 발송
                        if (smsService.isValidPhoneNumber(phoneNumber)) {
                            String formattedPhoneNumber = smsService.formatPhoneNumber(phoneNumber);
                            int waitingPosition = i + 1; // 대기 순서 (1부터 시작)
                            
                            boolean smsSent = smsService.sendWaitingNotification(
                                formattedPhoneNumber, 
                                patient.getName(), 
                                waitingPosition
                            );
                            
                            if (smsSent) {
                                // 발송 완료 표시
                                smsService.markAsSent(reception.getId());
                                log.info("SMS 알림 발송 완료: 환자 {} ({}번째 대기, 전화번호: {})", 
                                        patient.getName(), waitingPosition, phoneNumber);
                            } else {
                                log.warn("SMS 알림 발송 실패: 환자 {} ({}번째 대기, 전화번호: {})", 
                                        patient.getName(), waitingPosition, phoneNumber);
                            }
                        } else {
                            log.warn("유효하지 않은 전화번호로 SMS 발송 불가: 환자 {} (전화번호: {})", 
                                    patient.getName(), phoneNumber);
                        }
                    }
                }
            } else {
                log.debug("대기 환자가 3명 미만이므로 SMS 알림 발송하지 않음 (현재 {}명)", confirmedReceptions.size());
            }
        } catch (Exception e) {
            log.error("SMS 알림 발송 중 오류 발생: {}", e.getMessage(), e);
        }
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
} 