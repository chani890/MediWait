package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.PrescriptionRequest;
import org.example.dto.PrescriptionResponse;
import org.example.model.Prescription;
import org.example.model.Reception;
import org.example.repository.PrescriptionRepository;
import org.example.repository.ReceptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrescriptionService {
    
    private final PrescriptionRepository prescriptionRepository;
    private final ReceptionRepository receptionRepository;
    
    /**
     * 애플리케이션 시작 시 기존 처방전들의 status를 업데이트
     */
    @PostConstruct
    @Transactional
    public void initializePrescriptionStatus() {
        log.info("기존 처방전들의 status 초기화 시작");
        
        List<Prescription> prescriptionsWithoutStatus = prescriptionRepository.findAll()
                .stream()
                .filter(p -> p.getStatus() == null)
                .collect(Collectors.toList());
        
        if (!prescriptionsWithoutStatus.isEmpty()) {
            prescriptionsWithoutStatus.forEach(prescription -> {
                prescription.setStatus(Prescription.PrescriptionStatus.PENDING);
                prescription.setUpdatedAt(LocalDateTime.now());
            });
            
            prescriptionRepository.saveAll(prescriptionsWithoutStatus);
            log.info("{}개의 처방전 status를 PENDING으로 업데이트했습니다.", prescriptionsWithoutStatus.size());
        } else {
            log.info("status 업데이트가 필요한 처방전이 없습니다.");
        }
    }
    
    /**
     * 처방전 작성
     */
    @Transactional
    public PrescriptionResponse createPrescription(PrescriptionRequest request) {
        log.info("처방전 작성 시작: 접수 ID {}", request.getReceptionId());
        
        // 접수 정보 조회
        Reception reception = receptionRepository.findById(request.getReceptionId())
                .orElseThrow(() -> new RuntimeException("접수 정보를 찾을 수 없습니다."));
        
        // 이미 처방전이 있는지 확인
        prescriptionRepository.findByReception(reception)
                .ifPresent(existing -> {
                    throw new RuntimeException("이미 처방전이 작성된 환자입니다.");
                });
        
        // 처방전 생성
        Prescription prescription = Prescription.builder()
                .reception(reception)
                .diagnosis(request.getDiagnosis())
                .symptoms(request.getSymptoms())
                .treatmentPlan(request.getTreatmentPlan())
                .medications(request.getMedications())
                .dosageInstructions(request.getDosageInstructions())
                .notes(request.getNotes())
                .followUpDate(request.getFollowUpDate())
                .status(Prescription.PrescriptionStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        Prescription savedPrescription = prescriptionRepository.save(prescription);
        log.info("처방전 작성 완료: 처방전 ID {}", savedPrescription.getId());
        
        return convertToResponse(savedPrescription);
    }
    
    /**
     * 처방전 수정
     */
    @Transactional
    public PrescriptionResponse updatePrescription(Long prescriptionId, PrescriptionRequest request) {
        log.info("처방전 수정 시작: 처방전 ID {}", prescriptionId);
        
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("처방전을 찾을 수 없습니다."));
        
        // 처방전 정보 업데이트
        prescription.setDiagnosis(request.getDiagnosis());
        prescription.setSymptoms(request.getSymptoms());
        prescription.setTreatmentPlan(request.getTreatmentPlan());
        prescription.setMedications(request.getMedications());
        prescription.setDosageInstructions(request.getDosageInstructions());
        prescription.setNotes(request.getNotes());
        prescription.setFollowUpDate(request.getFollowUpDate());
        
        prescription = prescriptionRepository.save(prescription);
        log.info("처방전 수정 완료: 처방전 ID {}", prescription.getId());
        
        return convertToResponse(prescription);
    }
    
    /**
     * 접수별 처방전 조회
     */
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionByReceptionId(Long receptionId) {
        log.info("접수별 처방전 조회: 접수 ID {}", receptionId);
        Prescription prescription = prescriptionRepository.findByReceptionId(receptionId)
                .orElseThrow(() -> {
                    log.warn("처방전을 찾을 수 없습니다: 접수 ID {}", receptionId);
                    return new RuntimeException("처방전을 찾을 수 없습니다.");
                });
        return convertToResponse(prescription);
    }
    
    /**
     * 처방전 ID로 처방전 조회
     */
    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(Long prescriptionId) {
        log.info("처방전 ID로 조회: 처방전 ID {}", prescriptionId);
        
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("처방전을 찾을 수 없습니다."));
        
        return convertToResponse(prescription);
    }
    
    /**
     * 대기 중인 처방전 목록 조회 (간호사용)
     */
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getPendingPrescriptions() {
        log.info("대기 중인 처방전 목록 조회");
        
        List<Prescription> pendingPrescriptions = prescriptionRepository.findByStatusOrderByCreatedAtAsc(
                Prescription.PrescriptionStatus.PENDING);
        
        return pendingPrescriptions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 모든 처방전 목록 조회 (간호사용)
     */
    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getAllPrescriptions() {
        log.info("모든 처방전 목록 조회");
        
        List<Prescription> prescriptions = prescriptionRepository.findAllByOrderByCreatedAtDesc();
        
        return prescriptions.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * 처방전 상태 변경 (간호사용)
     */
    @Transactional
    public PrescriptionResponse updatePrescriptionStatus(Long prescriptionId, String status) {
        log.info("처방전 상태 변경: 처방전 ID {}, 상태 {}", prescriptionId, status);
        
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("처방전을 찾을 수 없습니다."));
        
        try {
            Prescription.PrescriptionStatus prescriptionStatus = Prescription.PrescriptionStatus.valueOf(status.toUpperCase());
            prescription.setStatus(prescriptionStatus);
            prescription.setUpdatedAt(LocalDateTime.now());
            
            Prescription savedPrescription = prescriptionRepository.save(prescription);
            return convertToResponse(savedPrescription);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("유효하지 않은 처방전 상태입니다: " + status);
        }
    }
    
    /**
     * 처방전 조제 완료 처리 (간호사용)
     */
    @Transactional
    public PrescriptionResponse completePrescription(Long prescriptionId) {
        log.info("처방전 조제 완료 처리: 처방전 ID {}", prescriptionId);
        
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new RuntimeException("처방전을 찾을 수 없습니다."));
        
        prescription.setStatus(Prescription.PrescriptionStatus.COMPLETED);
        prescription.setUpdatedAt(LocalDateTime.now());
        
        Prescription savedPrescription = prescriptionRepository.save(prescription);
        return convertToResponse(savedPrescription);
    }
    
    /**
     * 처방전을 응답 DTO로 변환
     */
    private PrescriptionResponse convertToResponse(Prescription prescription) {
        return PrescriptionResponse.builder()
                .id(prescription.getId())
                .receptionId(prescription.getReception().getId())
                .patientName(prescription.getReception().getPatient().getName())
                .patientPhoneNumber(prescription.getReception().getPatient().getPhoneNumber())
                .diagnosis(prescription.getDiagnosis())
                .symptoms(prescription.getSymptoms())
                .treatmentPlan(prescription.getTreatmentPlan())
                .medications(prescription.getMedications())
                .dosageInstructions(prescription.getDosageInstructions())
                .additionalNotes(prescription.getAdditionalNotes())
                .status(prescription.getStatus())
                .createdAt(prescription.getCreatedAt())
                .updatedAt(prescription.getUpdatedAt())
                .build();
    }
} 