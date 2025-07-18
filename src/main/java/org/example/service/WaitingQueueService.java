package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Reception;
import org.example.repository.ReceptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaitingQueueService {
    
    private final ReceptionRepository receptionRepository;
    private final SmsService smsService;
    
    /**
     * 대기열 변경 시 SMS 알림 체크 및 발송
     * 설정된 발송 시점에 따라 SMS 발송
     */
    @Transactional
    public void checkAndSendSmsNotifications() {
        try {
            // CONFIRMED 상태인 환자들을 확인 시간 순으로 조회
            List<Reception> waitingReceptions = receptionRepository.findByStatusOrderByConfirmedAtAsc(
                Reception.ReceptionStatus.CONFIRMED
            );
            
            log.info("대기 중인 환자 수: {}", waitingReceptions.size());
            
            // 설정된 발송 시점 가져오기 (기본값: 2번째 순서)
            int notifyTiming = smsService.getSmsNotifyTiming();
            
            // 대기 인원이 설정된 시점 이상이어야 SMS 발송 가능
            if (waitingReceptions.size() >= notifyTiming) {
                // 설정된 순서의 대기자 (인덱스는 0부터 시작하므로 -1)
                Reception targetWaitingReception = waitingReceptions.get(notifyTiming - 1);
                
                // SMS 알림이 활성화되어 있고 아직 발송되지 않은 경우
                if (Boolean.TRUE.equals(targetWaitingReception.getSmsNotificationEnabled()) && 
                    !Boolean.TRUE.equals(targetWaitingReception.getSmsSent())) {
                    
                    String phoneNumber = targetWaitingReception.getPatient().getPhoneNumber();
                    String patientName = targetWaitingReception.getPatient().getName();
                    
                    // 전화번호 유효성 검증
                    if (smsService.isValidPhoneNumber(phoneNumber)) {
                        boolean smsResult = smsService.sendWaitingNotification(phoneNumber, patientName);
                        
                        if (smsResult) {
                            // SMS 발송 성공 시 플래그 업데이트
                            targetWaitingReception.setSmsSent(true);
                            receptionRepository.save(targetWaitingReception);
                            log.info("SMS 발송 완료: {} ({})", patientName, phoneNumber);
                        } else {
                            log.error("SMS 발송 실패: {} ({})", patientName, phoneNumber);
                        }
                    } else {
                        log.warn("유효하지 않은 전화번호: {} ({})", patientName, phoneNumber);
                    }
                }
            } else {
                log.debug("대기 인원이 {}명 미만이므로 SMS 발송하지 않음", notifyTiming);
            }
            
        } catch (Exception e) {
            log.error("SMS 알림 체크 중 오류 발생: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 환자 상태 변경 시 SMS 발송 상태 초기화
     * 환자가 다시 대기열에 들어갔을 때 SMS를 재발송할 수 있도록 함
     */
    @Transactional
    public void resetSmsStatusForReception(Long receptionId) {
        try {
            Reception reception = receptionRepository.findById(receptionId).orElse(null);
            if (reception != null) {
                reception.setSmsSent(false);
                receptionRepository.save(reception);
                log.info("SMS 발송 상태 초기화: 접수 ID {}", receptionId);
            }
        } catch (Exception e) {
            log.error("SMS 상태 초기화 중 오류 발생: {}", e.getMessage());
        }
    }
    
    /**
     * 특정 환자의 대기 순서 조회
     */
    public int getWaitingPosition(Long receptionId) {
        List<Reception> waitingReceptions = receptionRepository.findByStatusOrderByConfirmedAtAsc(
            Reception.ReceptionStatus.CONFIRMED
        );
        
        for (int i = 0; i < waitingReceptions.size(); i++) {
            if (waitingReceptions.get(i).getId().equals(receptionId)) {
                return i + 1; // 1부터 시작하는 순서
            }
        }
        return 0; // 대기열에 없음
    }
} 