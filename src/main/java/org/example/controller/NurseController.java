package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.*;
import org.example.service.PrescriptionService;
import org.example.service.ReceptionService;
import org.example.service.StatisticsService;
import org.example.service.SmsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/nurse")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NurseController {
    
    private final ReceptionService receptionService;
    private final PrescriptionService prescriptionService;
    private final StatisticsService statisticsService;
    private final SmsService smsService;
    
    /**
     * 대기 중인 접수 목록 조회 (신분증 미확인)
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ReceptionResponse>> getPendingReceptions() {
        log.info("대기 중인 접수 목록 조회");
        List<ReceptionResponse> pendingReceptions = receptionService.getPendingReceptions();
        return ResponseEntity.ok(pendingReceptions);
    }
    
    /**
     * 신분증 확인 후 접수 승인
     */
    @PostMapping("/confirm/{receptionId}")
    public ResponseEntity<ReceptionResponse> confirmReception(@PathVariable Long receptionId) {
        log.info("접수 확인 요청: 접수 ID {}", receptionId);
        ReceptionResponse response = receptionService.confirmReception(receptionId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 확인된 접수 목록 조회 (대기열)
     */
    @GetMapping("/confirmed")
    public ResponseEntity<List<ReceptionResponse>> getConfirmedReceptions() {
        log.info("확인된 접수 목록 조회");
        List<ReceptionResponse> confirmedReceptions = receptionService.getConfirmedReceptions();
        return ResponseEntity.ok(confirmedReceptions);
    }
    
    /**
     * 수기 접수 (바로 확인 상태로 등록)
     */
    @PostMapping("/manual-register")
    public ResponseEntity<ReceptionResponse> manualRegister(@RequestBody org.example.dto.PatientRegistrationRequest request) {
        log.info("수기 접수 요청: {}", request.getName());
        ReceptionResponse response = receptionService.registerPatient(request);
        // 바로 확인 처리
        response = receptionService.confirmReception(response.getId());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 환자 이름으로 검색
     */
    @GetMapping("/search-patient")
    public ResponseEntity<List<PatientInfoResponse>> searchPatientByName(@RequestParam String name) {
        log.info("환자 검색 요청: {}", name);
        List<PatientInfoResponse> patients = receptionService.searchPatientsByName(name);
        return ResponseEntity.ok(patients);
    }
    
    /**
     * 바이탈사인 저장
     */
    @PostMapping("/vital-sign")
    public ResponseEntity<VitalSignResponse> saveVitalSign(@RequestBody VitalSignRequest request) {
        log.info("바이탈사인 저장 요청: 접수 ID {}", request.getReceptionId());
        VitalSignResponse response = receptionService.saveVitalSign(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 바이탈사인 조회 (접수 ID로)
     */
    @GetMapping("/vital-sign/{receptionId}")
    public ResponseEntity<VitalSignResponse> getVitalSign(@PathVariable Long receptionId) {
        log.info("바이탈사인 조회 요청: 접수 ID {}", receptionId);
        VitalSignResponse response = receptionService.getVitalSign(receptionId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 접수 삭제 (간호사 권한)
     */
    @DeleteMapping("/reception/{receptionId}")
    public ResponseEntity<Map<String, Object>> deleteReception(@PathVariable Long receptionId) {
        log.info("접수 삭제 요청: 접수 ID {}", receptionId);
        
        try {
            ReceptionResponse deletedReception = receptionService.deleteReception(receptionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "접수가 성공적으로 삭제되었습니다.");
            response.put("deletedReception", deletedReception);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("접수 삭제 실패: 접수 ID {}, 오류: {}", receptionId, e.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 오늘 통계 조회
     */
    @GetMapping("/statistics/today")
    public ResponseEntity<NurseStatisticsResponse> getTodayStatistics() {
        log.info("오늘 간호사 통계 조회");
        NurseStatisticsResponse statistics = statisticsService.getTodayNurseStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 이번 주 통계 조회
     */
    @GetMapping("/statistics/week")
    public ResponseEntity<NurseStatisticsResponse> getWeekStatistics() {
        log.info("이번 주 간호사 통계 조회");
        NurseStatisticsResponse statistics = statisticsService.getThisWeekNurseStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 이번 달 통계 조회
     */
    @GetMapping("/statistics/month")
    public ResponseEntity<NurseStatisticsResponse> getMonthStatistics() {
        log.info("이번 달 간호사 통계 조회");
        NurseStatisticsResponse statistics = statisticsService.getThisMonthNurseStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 사용자 정의 기간 통계 조회
     */
    @GetMapping("/statistics/custom")
    public ResponseEntity<NurseStatisticsResponse> getCustomStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("사용자 정의 기간 간호사 통계 조회: {} ~ {}", startDate, endDate);
        
        // 기간 유효성 검사
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작 날짜가 종료 날짜보다 늦을 수 없습니다.");
        }
        
        // 최대 1년 제한
        if (startDate.isBefore(endDate.minusYears(1))) {
            throw new IllegalArgumentException("조회 기간은 최대 1년까지 가능합니다.");
        }
        
        NurseStatisticsResponse statistics = statisticsService.getNurseStatistics(startDate, endDate);
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * SMS 모드 상태 조회
     */
    @GetMapping("/sms-mode")
    public ResponseEntity<Map<String, Object>> getSmsMode() {
        Map<String, Object> response = new HashMap<>();
        response.put("simulationMode", smsService.isSimulationMode());
        response.put("currentMode", smsService.isSimulationMode() ? "시뮬레이션 모드" : "실제 발송 모드");
        return ResponseEntity.ok(response);
    }

    /**
     * SMS 모드 설정
     */
    @PostMapping("/sms-mode")
    public ResponseEntity<String> setSmsMode(@RequestParam boolean simulationMode) {
        try {
            smsService.setSimulationMode(simulationMode);
            String mode = simulationMode ? "시뮬레이션 모드" : "실제 발송 모드";
            return ResponseEntity.ok("SMS 모드가 " + mode + "로 변경되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("모드 변경 실패: " + e.getMessage());
        }
    }

    /**
     * SMS 테스트 발송
     */
    @PostMapping("/test-sms")
    public ResponseEntity<String> testSms(@RequestParam String phoneNumber, @RequestParam String message) {
        log.info("SMS 테스트 발송 요청: {} -> {}", phoneNumber, message);
        
        if (!smsService.isValidPhoneNumber(phoneNumber)) {
            return ResponseEntity.badRequest().body("유효하지 않은 전화번호입니다.");
        }
        
        String formattedPhoneNumber = smsService.formatPhoneNumber(phoneNumber);
        boolean success = smsService.sendSms(formattedPhoneNumber, message);
        
        String mode = smsService.isSimulationMode() ? " (시뮬레이션)" : " (실제 발송)";
        if (success) {
            return ResponseEntity.ok("SMS 발송 성공" + mode);
        } else {
            return ResponseEntity.badRequest().body("SMS 발송 실패" + mode);
        }
    }
    
    /**
     * 대기 중인 처방전 목록 조회 (간호사용)
     */
    @GetMapping("/prescriptions/pending")
    public ResponseEntity<List<PrescriptionResponse>> getPendingPrescriptions() {
        log.info("대기 중인 처방전 목록 조회");
        List<PrescriptionResponse> pendingPrescriptions = prescriptionService.getPendingPrescriptions();
        return ResponseEntity.ok(pendingPrescriptions);
    }
    
    /**
     * 모든 처방전 목록 조회 (간호사용)
     */
    @GetMapping("/prescriptions")
    public ResponseEntity<List<PrescriptionResponse>> getAllPrescriptions() {
        log.info("모든 처방전 목록 조회");
        List<PrescriptionResponse> prescriptions = prescriptionService.getAllPrescriptions();
        return ResponseEntity.ok(prescriptions);
    }
    
    /**
     * 처방전 상태 변경 (간호사용)
     */
    @PutMapping("/prescriptions/{prescriptionId}/status")
    public ResponseEntity<PrescriptionResponse> updatePrescriptionStatus(
            @PathVariable Long prescriptionId, 
            @RequestParam String status) {
        log.info("처방전 상태 변경: 처방전 ID {}, 상태 {}", prescriptionId, status);
        PrescriptionResponse response = prescriptionService.updatePrescriptionStatus(prescriptionId, status);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 처방전 조제 완료 처리 (간호사용)
     */
    @PostMapping("/prescriptions/{prescriptionId}/complete")
    public ResponseEntity<PrescriptionResponse> completePrescription(@PathVariable Long prescriptionId) {
        log.info("처방전 조제 완료 처리: 처방전 ID {}", prescriptionId);
        PrescriptionResponse response = prescriptionService.completePrescription(prescriptionId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 처방전 출력 (간호사용)
     */
    @GetMapping("/prescriptions/{prescriptionId}/print")
    public ResponseEntity<PrescriptionResponse> printPrescription(@PathVariable Long prescriptionId) {
        log.info("처방전 출력 요청: 처방전 ID {}", prescriptionId);
        
        // 처방전 조회
        PrescriptionResponse prescription = prescriptionService.getPrescriptionById(prescriptionId);
        
        // 출력 완료 처리 (상태를 COMPLETED로 변경)
        prescriptionService.completePrescription(prescriptionId);
        
        log.info("처방전 출력 완료: 처방전 ID {}", prescriptionId);
        return ResponseEntity.ok(prescription);
    }
    
    /**
     * 수동 SMS 알림 발송 (대기 순서 알림)
     */
    @PostMapping("/send-waiting-notification")
    public ResponseEntity<String> sendWaitingNotification(@RequestParam Long receptionId) {
        log.info("수동 SMS 알림 발송 요청: 접수 ID {}", receptionId);
        
        try {
            org.example.model.Reception reception = receptionService.findById(receptionId);
            if (reception == null) {
                return ResponseEntity.badRequest().body("접수를 찾을 수 없습니다.");
            }
            
            if (reception.getStatus() != org.example.model.Reception.ReceptionStatus.CONFIRMED) {
                return ResponseEntity.badRequest().body("확인된 접수만 SMS 알림을 발송할 수 있습니다.");
            }
            
            String phoneNumber = reception.getPatient().getPhoneNumber();
            if (!smsService.isValidPhoneNumber(phoneNumber)) {
                return ResponseEntity.badRequest().body("유효하지 않은 전화번호입니다.");
            }
            
            int waitingPosition = receptionService.getWaitingPosition(receptionId);
            String formattedPhoneNumber = smsService.formatPhoneNumber(phoneNumber);
            
            boolean success = smsService.sendWaitingNotification(
                formattedPhoneNumber, 
                reception.getPatient().getName(), 
                waitingPosition
            );
            
            if (success) {
                smsService.markAsSent(receptionId);
                return ResponseEntity.ok("SMS 알림 발송 성공");
            } else {
                return ResponseEntity.badRequest().body("SMS 알림 발송 실패");
            }
        } catch (Exception e) {
            log.error("SMS 알림 발송 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("SMS 알림 발송 중 오류가 발생했습니다.");
        }
    }

    /**
     * 환자 정보 수정 (이름, 생년월일, 전화번호)
     */
    @PutMapping("/patient/{patientId}")
    public ResponseEntity<PatientInfoResponse> updatePatientInfo(
            @PathVariable Long patientId,
            @RequestBody Map<String, Object> updateRequest) {
        String name = (String) updateRequest.get("name");
        String birthDateStr = (String) updateRequest.get("birthDate");
        String phoneNumber = (String) updateRequest.get("phoneNumber");
        PatientInfoResponse response = receptionService.updatePatientInfo(patientId, name, birthDateStr, phoneNumber);
        return ResponseEntity.ok(response);
    }
} 