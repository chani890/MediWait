package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeNotificationService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * 대기열 업데이트 알림
     */
    public void notifyWaitingQueueUpdate() {
        log.info("대기열 업데이트 알림 전송");
        Map<String, Object> message = new HashMap<>();
        message.put("type", "QUEUE_UPDATE");
        message.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/waiting-queue", message);
    }
    
    /**
     * 특정 환자에게 상태 변경 알림
     */
    public void notifyPatientStatusChange(Long receptionId, String oldStatus, String newStatus) {
        log.info("환자 상태 변경 알림: 접수 ID {}, {} -> {}", receptionId, oldStatus, newStatus);
        
        // 개별 환자에게 알림
        Map<String, Object> message = new HashMap<>();
        message.put("type", "STATUS_CHANGE");
        message.put("receptionId", receptionId);
        message.put("oldStatus", oldStatus);
        message.put("newStatus", newStatus);
        message.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/reception/" + receptionId, message);
        
        // 전체 대기열에도 알림
        notifyWaitingQueueUpdate();
    }
    
    /**
     * 새 접수 알림
     */
    public void notifyNewReception(Long receptionId, String patientName) {
        log.info("새 접수 알림: 접수 ID {}, 환자 {}", receptionId, patientName);
        
        // 간호사 화면에 알림
        Map<String, Object> nurseMessage = new HashMap<>();
        nurseMessage.put("type", "NEW_RECEPTION");
        nurseMessage.put("receptionId", receptionId);
        nurseMessage.put("patientName", patientName);
        nurseMessage.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/nurse/new-reception", nurseMessage);
        
        // 전체 대기열 업데이트
        notifyWaitingQueueUpdate();
    }
    
    /**
     * 의사 호출 알림
     */
    public void notifyDoctorCall(Long receptionId, String patientName) {
        log.info("의사 호출 알림: 접수 ID {}, 환자 {}", receptionId, patientName);
        
        // 특정 환자에게 호출 알림
        Map<String, Object> patientMessage = new HashMap<>();
        patientMessage.put("type", "DOCTOR_CALL");
        patientMessage.put("receptionId", receptionId);
        patientMessage.put("patientName", patientName);
        patientMessage.put("message", patientName + "님, 진료실로 입장해 주세요!");
        patientMessage.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/reception/" + receptionId, patientMessage);
        
        // 의사 화면에 환자 정보 업데이트
        Map<String, Object> doctorMessage = new HashMap<>();
        doctorMessage.put("type", "CURRENT_PATIENT_UPDATE");
        doctorMessage.put("receptionId", receptionId);
        doctorMessage.put("patientName", patientName);
        doctorMessage.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/doctor/current-patient", doctorMessage);
    }
    
    /**
     * 통계 업데이트 알림
     */
    public void notifyStatisticsUpdate() {
        log.info("통계 업데이트 알림 전송");
        Map<String, Object> message = new HashMap<>();
        message.put("type", "STATISTICS_UPDATE");
        message.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/statistics", message);
    }
    
    /**
     * 환자 호출 메시지를 대기열 현황 페이지에 전송
     */
    public void notifyPatientCall(String patientName) {
        log.info("환자 호출 메시지 전송: {}", patientName);
        Map<String, Object> message = new HashMap<>();
        message.put("type", "PATIENT_CALL");
        message.put("patientName", patientName);
        message.put("timestamp", System.currentTimeMillis());
        messagingTemplate.convertAndSend("/topic/patient-call", message);
    }
} 