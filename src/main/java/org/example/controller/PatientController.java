package org.example.controller;

import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.PatientRegistrationRequest;
import org.example.dto.ReceptionResponse;
import org.example.model.Reception;
import org.example.service.ReceptionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class PatientController {
    
    private final ReceptionService receptionService;
    
    /**
     * QR 코드 스캔 후 환자 접수
     */
    @PostMapping("/register")
    public ResponseEntity<ReceptionResponse> registerPatient(@Valid @RequestBody PatientRegistrationRequest request) {
        log.info("환자 접수 요청: {}", request.getName());
        ReceptionResponse response = receptionService.registerPatient(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 현재 대기 인원 조회
     */
    @GetMapping("/waiting-count")
    public ResponseEntity<Integer> getCurrentWaitingCount() {
        int waitingCount = receptionService.getCurrentWaitingCount();
        return ResponseEntity.ok(waitingCount);
    }
    
    /**
     * 접수 상태 조회
     */
    @GetMapping("/reception/{receptionId}")
    public ResponseEntity<ReceptionResponse> getReceptionStatus(@PathVariable Long receptionId) {
        // 실제 구현에서는 접수 ID로 상태를 조회하는 로직 추가 필요
        return ResponseEntity.ok().build();
    }

    @GetMapping("/reception/status/{receptionId}")
    public ResponseEntity<Map<String, Object>> getWaitingStatus(@PathVariable Long receptionId) {
        log.info("Fetching waiting status for reception ID: {}", receptionId);
        try {
            log.info("Step 1: Finding reception by ID");
            Reception reception = receptionService.findById(receptionId);
            if (reception == null) {
                log.warn("Reception not found for ID: {}", receptionId);
                return ResponseEntity.notFound().build();
            }
            log.info("Step 2: Found reception: {}", reception.getId());

            log.info("Step 3: Getting patient info");
            if (reception.getPatient() == null) {
                log.error("Patient is null for reception ID: {}", receptionId);
                throw new RuntimeException("Patient data is missing");
            }
            log.info("Step 4: Patient found: {}", reception.getPatient().getName());

            log.info("Step 5: Calculating waiting position");
            int waitingPosition = receptionService.getWaitingPosition(receptionId);
            log.info("Step 6: Waiting position calculated: {}", waitingPosition);
            
            log.info("Step 7: Getting total waiting count");
            int totalWaiting = receptionService.getCurrentWaitingCount();
            log.info("Step 8: Total waiting count: {}", totalWaiting);

            log.info("Step 9: Building response");
            Map<String, Object> response = new HashMap<>();
            response.put("receptionId", reception.getId());
            response.put("patientName", reception.getPatient().getName());
            response.put("phoneNumber", reception.getPatient().getPhoneNumber());
            response.put("createdAt", reception.getCreatedAt());
            response.put("status", reception.getStatus());
            response.put("waitingPosition", waitingPosition);
            response.put("totalWaiting", totalWaiting);
            response.put("notifyEnabled", reception.getNotifyEnabled() != null ? reception.getNotifyEnabled() : false);

            log.info("Step 10: Response built successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error fetching waiting status for reception ID: {}", receptionId, e);
            log.error("Exception type: {}", e.getClass().getSimpleName());
            log.error("Exception message: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("Exception cause: {}", e.getCause().getMessage());
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
} 