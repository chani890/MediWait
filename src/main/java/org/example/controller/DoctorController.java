package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.PatientInfoResponse;
import org.example.dto.PrescriptionRequest;
import org.example.dto.PrescriptionResponse;
import org.example.dto.ReceptionResponse;
import org.example.dto.VitalSignResponse;
import org.example.service.PrescriptionService;
import org.example.service.ReceptionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DoctorController {
    
    private final ReceptionService receptionService;
    private final PrescriptionService prescriptionService;
    
    /**
     * 대기열 조회 (확인된 환자들)
     */
    @GetMapping("/waiting-queue")
    public ResponseEntity<List<ReceptionResponse>> getWaitingQueue() {
        List<ReceptionResponse> waitingQueue = receptionService.getConfirmedReceptions();
        return ResponseEntity.ok(waitingQueue);
    }
    
    /**
     * 다음 환자 호출
     */
    @PostMapping("/call-next")
    public ResponseEntity<PatientInfoResponse> callNextPatient() {
        log.info("다음 환자 호출 요청");
        PatientInfoResponse patientInfo = receptionService.callNextPatient();
        return ResponseEntity.ok(patientInfo);
    }
    
    /**
     * 현재 진료 중인 환자 목록 조회
     */
    @GetMapping("/current-patients")
    public ResponseEntity<List<PatientInfoResponse>> getCurrentPatients() {
        log.info("현재 진료 중인 환자 목록 조회");
        List<PatientInfoResponse> patients = receptionService.getCalledPatients();
        return ResponseEntity.ok(patients);
    }
    
    /**
     * 환자 정보 조회 (문진표 + 과거 이력 포함)
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<PatientInfoResponse> getPatientInfo(@PathVariable Long patientId) {
        PatientInfoResponse patientInfo = receptionService.getPatientInfo(patientId);
        return ResponseEntity.ok(patientInfo);
    }
    
    /**
     * 접수별 바이탈사인 조회
     */
    @GetMapping("/vital-sign/{receptionId}")
    public ResponseEntity<VitalSignResponse> getVitalSign(@PathVariable Long receptionId) {
        log.info("바이탈사인 조회 요청: 접수 ID {}", receptionId);
        try {
            VitalSignResponse response = receptionService.getVitalSign(receptionId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            log.warn("바이탈사인을 찾을 수 없음: 접수 ID {}", receptionId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 처방전 작성
     */
    @PostMapping("/prescriptions")
    public ResponseEntity<PrescriptionResponse> createPrescription(@RequestBody PrescriptionRequest request) {
        log.info("처방전 작성 요청: 접수 ID {}", request.getReceptionId());
        PrescriptionResponse response = prescriptionService.createPrescription(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 처방전 수정
     */
    @PutMapping("/prescriptions/{prescriptionId}")
    public ResponseEntity<PrescriptionResponse> updatePrescription(
            @PathVariable Long prescriptionId, 
            @RequestBody PrescriptionRequest request) {
        log.info("처방전 수정 요청: 처방전 ID {}", prescriptionId);
        PrescriptionResponse response = prescriptionService.updatePrescription(prescriptionId, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 접수별 처방전 조회
     */
    @GetMapping("/prescriptions/reception/{receptionId}")
    public ResponseEntity<PrescriptionResponse> getPrescriptionByReception(@PathVariable Long receptionId) {
        log.info("접수별 처방전 조회: 접수 ID {}", receptionId);
        PrescriptionResponse response = prescriptionService.getPrescriptionByReceptionId(receptionId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 처방전 ID로 처방전 조회
     */
    @GetMapping("/prescriptions/{prescriptionId}")
    public ResponseEntity<PrescriptionResponse> getPrescriptionById(@PathVariable Long prescriptionId) {
        log.info("처방전 ID로 조회: 처방전 ID {}", prescriptionId);
        PrescriptionResponse response = prescriptionService.getPrescriptionById(prescriptionId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 진료 완료 처리 (처방전 작성 후)
     */
    @PostMapping("/complete/{receptionId}")
    public ResponseEntity<ReceptionResponse> completeReception(@PathVariable Long receptionId) {
        log.info("진료 완료 요청: 접수 ID {}", receptionId);
        
        // 처방전이 작성되었는지 확인
        try {
            log.info("Fetching prescription for reception ID: {}", receptionId);
            prescriptionService.getPrescriptionByReceptionId(receptionId);
        } catch (RuntimeException e) {
            log.warn("처방전이 작성되지 않은 상태에서 진료 완료 시도: 접수 ID {}", receptionId);
            throw new RuntimeException("처방전을 먼저 작성해주십시오.");
        }
        
        ReceptionResponse response = receptionService.completeReception(receptionId);
        return ResponseEntity.ok(response);
    }
} 