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
     * 진료 중인 환자를 포함한 접수 강제 삭제 (관리자용)
     */
    @DeleteMapping("/reception/{receptionId}/force")
    public ResponseEntity<Map<String, Object>> forceDeleteReception(@PathVariable Long receptionId) {
        log.info("접수 강제 삭제 요청: 접수 ID {}", receptionId);
        
        try {
            ReceptionResponse deletedReception = receptionService.forceDeleteReception(receptionId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "접수가 강제로 삭제되었습니다.");
            response.put("deletedReception", deletedReception);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("접수 강제 삭제 실패: 접수 ID {}, 오류: {}", receptionId, e.getMessage());
            
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
    
    /**
     * 접수별 SMS 알림 설정
     */
    @PutMapping("/reception/{receptionId}/sms")
    public ResponseEntity<Map<String, Object>> updateSmsNotification(
            @PathVariable Long receptionId,
            @RequestBody Map<String, Object> request) {
        try {
            Boolean enabled = (Boolean) request.get("enabled");
            receptionService.updateSmsNotification(receptionId, enabled);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "SMS 알림 설정이 업데이트되었습니다.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("SMS 알림 설정 업데이트 실패: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "SMS 알림 설정 업데이트에 실패했습니다.");
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * SMS 모드 조회 (시뮬레이션 모드 vs 실제 발송 모드)
     */
    @GetMapping("/sms-mode")
    public ResponseEntity<Map<String, Object>> getSmsMode() {
        boolean simulationMode = smsService.isSimulationMode();
        String currentMode = simulationMode 
            ? "시뮬레이션 모드 (실제 SMS 발송 없음)" 
            : "실제 발송 모드 (실제 SMS 발송)";
        
        Map<String, Object> response = new HashMap<>();
        response.put("simulationMode", simulationMode);
        response.put("currentMode", currentMode);
        return ResponseEntity.ok(response);
    }
    
    /**
     * SMS 모드 설정 (시뮬레이션 모드 vs 실제 발송 모드)
     */
    @PostMapping("/sms-mode")
    public ResponseEntity<String> setSmsMode(@RequestParam boolean simulationMode) {
        smsService.setSimulationMode(simulationMode);
        
        String mode = simulationMode ? "시뮬레이션 모드" : "실제 발송 모드";
        String message = String.format("SMS 발송 모드가 %s로 변경되었습니다.", mode);
        
        log.info("SMS 모드 변경: {}", mode);
        return ResponseEntity.ok(message);
    }
    
    /**
     * SMS 알림 발송 시점 조회
     */
    @GetMapping("/sms-timing")
    public ResponseEntity<Integer> getSmsNotifyTiming() {
        int timing = smsService.getSmsNotifyTiming();
        return ResponseEntity.ok(timing);
    }
    
    /**
     * SMS 알림 발송 시점 설정
     */
    @PostMapping("/sms-timing")
    public ResponseEntity<String> setSmsNotifyTiming(@RequestParam int timing) {
        if (timing < 1 || timing > 5) {
            return ResponseEntity.badRequest().body("알림 발송 시점은 1명에서 5명 사이로 설정해주세요.");
        }
        
        smsService.setSmsNotifyTiming(timing);
        String message = String.format("SMS 알림 발송 시점이 %d번째 순서로 설정되었습니다.", timing);
        
        log.info("SMS 알림 발송 시점 변경: {}번째 순서", timing);
        return ResponseEntity.ok(message);
    }
    
    /**
     * SMS 테스트 발송
     */
    @PostMapping("/test-sms")
    public ResponseEntity<Map<String, Object>> sendTestSms(@RequestBody Map<String, Object> request) {
        try {
            String phoneNumber = (String) request.get("phoneNumber");
            String patientName = (String) request.get("patientName");
            
            if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "전화번호를 입력해주세요.");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            if (patientName == null || patientName.trim().isEmpty()) {
                patientName = "테스트 환자";
            }
            
            // 전화번호 형식 검증
            if (!smsService.isValidPhoneNumber(phoneNumber)) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "유효하지 않은 전화번호 형식입니다. (010-XXXX-XXXX)");
                return ResponseEntity.badRequest().body(errorResponse);
            }
            
            // SMS 발송
            boolean smsResult = smsService.sendWaitingNotification(phoneNumber, patientName);
            
            Map<String, Object> response = new HashMap<>();
            if (smsResult) {
                response.put("success", true);
                response.put("message", "SMS 테스트 발송이 성공했습니다.");
                log.info("SMS 테스트 발송 성공: {} -> {}", phoneNumber, patientName);
            } else {
                response.put("success", false);
                response.put("message", "SMS 테스트 발송에 실패했습니다.");
                log.error("SMS 테스트 발송 실패: {} -> {}", phoneNumber, patientName);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("SMS 테스트 발송 중 오류 발생: {}", e.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "SMS 테스트 발송 중 오류가 발생했습니다: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
} 