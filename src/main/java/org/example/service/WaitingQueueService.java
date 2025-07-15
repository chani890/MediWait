package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Reception;
import org.example.repository.ReceptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingQueueService {
    
    private final ReceptionRepository receptionRepository;
    private final SmsService smsService;
    
    // 이미 SMS를 발송한 접수 ID들을 추적하는 Set
    private final Set<Long> smsNotifiedReceptionIds = new HashSet<>();
    
    /**
     * 대기 순서 변경 시 SMS 알림 체크 및 발송
     * 각 환자의 notifyAt 설정에 따라 알림 발송
     */
    @Transactional(readOnly = true)
    public void checkAndSendWaitingNotifications() {
        log.info("대기 순서 SMS 알림 체크 시작");
        
        // 확인된 환자들을 순서대로 조회
        List<Reception> confirmedReceptions = receptionRepository.findConfirmedReceptionsInOrder();
        
        if (confirmedReceptions.size() < 3) {
            log.debug("대기 환자가 3명 미만이므로 SMS 알림 발송하지 않음. 현재 대기: {}명", confirmedReceptions.size());
            return;
        }
        
        // 각 환자의 notifyAt 설정에 따라 SMS 발송
        for (int i = 0; i < confirmedReceptions.size(); i++) {
            Reception reception = confirmedReceptions.get(i);
            int remainingCount = i; // 앞 대기자 수
            
            // notifyAt 설정이 없거나 SMS 알림이 비활성화된 경우 건너뛰기
            if (reception.getNotifyAt() == null || !reception.getNotifyEnabled()) {
                continue;
            }
            
            // 앞 대기자 수가 설정된 notifyAt와 일치하는 경우에만 알림 발송
            if (remainingCount != reception.getNotifyAt()) {
                continue;
            }
            
            // 이미 알림을 보낸 접수인지 확인
            if (smsNotifiedReceptionIds.contains(reception.getId())) {
                log.debug("이미 SMS 알림을 발송한 접수: ID {}, 환자 {}", reception.getId(), reception.getPatient().getName());
                continue;
            }
            
            // SMS 알림이 활성화되어 있고 전화번호가 있는 경우에만 발송
            if (reception.getPatient().getPhoneNumber() != null) {
                boolean smsSuccess = smsService.sendWaitingNotification(
                    reception.getPatient().getPhoneNumber(),
                    reception.getPatient().getName(),
                    remainingCount
                );
                
                if (smsSuccess) {
                    // 발송 성공 시 추적 Set에 추가
                    smsNotifiedReceptionIds.add(reception.getId());
                    log.info("대기 순서 SMS 발송 성공: 환자 {} (앞 대기자 {}명)", reception.getPatient().getName(), remainingCount);
                } else {
                    log.warn("대기 순서 SMS 발송 실패: 환자 {} (앞 대기자 {}명)", reception.getPatient().getName(), remainingCount);
                }
            } else {
                log.debug("SMS 알림 조건 불충족: 환자 {} - 전화번호: {}", 
                    reception.getPatient().getName(), 
                    reception.getPatient().getPhoneNumber() != null ? "있음" : "없음");
            }
        }
        
        log.info("대기 순서 SMS 알림 체크 완료");
    }
    
    /**
     * 환자 호출 시 SMS 발송
     */
    public void sendCallNotification(Reception reception) {
        if (reception.getNotifyEnabled() && reception.getPatient().getPhoneNumber() != null) {
            boolean smsSuccess = smsService.sendCallNotification(
                reception.getPatient().getPhoneNumber(),
                reception.getPatient().getName()
            );
            
            if (smsSuccess) {
                log.info("호출 SMS 발송 성공: 환자 {}", reception.getPatient().getName());
            } else {
                log.warn("호출 SMS 발송 실패: 환자 {}", reception.getPatient().getName());
            }
        } else {
            log.debug("호출 SMS 발송 조건 불충족: 환자 {} - 알림설정: {}, 전화번호: {}", 
                reception.getPatient().getName(), 
                reception.getNotifyEnabled(), 
                reception.getPatient().getPhoneNumber() != null ? "있음" : "없음");
        }
    }
    
    /**
     * 접수 완료 시 SMS 추적에서 제거
     */
    public void removeFromSmsTracking(Long receptionId) {
        if (smsNotifiedReceptionIds.remove(receptionId)) {
            log.debug("SMS 추적에서 제거: 접수 ID {}", receptionId);
        }
    }
    
    /**
     * 현재 SMS 알림 발송 상태 조회 (디버깅용)
     */
    @Transactional(readOnly = true)
    public void logCurrentWaitingStatus() {
        List<Reception> confirmedReceptions = receptionRepository.findConfirmedReceptionsInOrder();
        log.info("=== 현재 대기 현황 ===");
        log.info("총 대기 환자 수: {}명", confirmedReceptions.size());
        
        for (int i = 0; i < confirmedReceptions.size(); i++) {
            Reception reception = confirmedReceptions.get(i);
            boolean smsNotified = smsNotifiedReceptionIds.contains(reception.getId());
            log.info("{}번째: {} (ID: {}, SMS발송: {}, 알림설정: {})", 
                i + 1, 
                reception.getPatient().getName(), 
                reception.getId(),
                smsNotified ? "완료" : "미발송",
                reception.getNotifyEnabled() ? "활성" : "비활성"
            );
        }
        log.info("===================");
    }
} 