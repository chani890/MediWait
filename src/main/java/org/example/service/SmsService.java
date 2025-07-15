package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.nurigo.java_sdk.api.Message;
import net.nurigo.java_sdk.exceptions.CoolsmsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {
    
    @Value("${sms.api.key}")
    private String apiKey;
    
    @Value("${sms.api.secret}")
    private String apiSecret;
    
    @Value("${sms.api.from}")
    private String fromNumber;
    
    // SMS 발송 추적을 위한 메모리 저장소
    private final ConcurrentMap<Long, Boolean> sentNotifications = new ConcurrentHashMap<>();
    
    // 시뮬레이션 모드 플래그
    private boolean simulationMode = false;
    
    /**
     * SMS 발송
     * @param phoneNumber 수신자 전화번호
     * @param message 메시지 내용
     * @return 발송 성공 여부
     */
    public boolean sendSms(String phoneNumber, String message) {
        if (simulationMode) {
            log.info("📱 [시뮬레이션 모드] SMS 발송: {} -> {}", fromNumber, phoneNumber);
            log.info("📱 [시뮬레이션 모드] 메시지 내용: {}", message);
            return true;
        }
        
        try {
            Message coolsms = new Message(apiKey, apiSecret);
            
            HashMap<String, String> params = new HashMap<>();
            params.put("to", phoneNumber);
            params.put("from", fromNumber);
            params.put("type", "SMS");
            params.put("text", message);
            
            coolsms.send(params);
            
            log.info("📱 SMS 발송 성공: {} -> {}", fromNumber, phoneNumber);
            return true;
            
        } catch (CoolsmsException e) {
            log.error("📱 SMS 발송 실패: {} -> {}, 에러: {}", fromNumber, phoneNumber, e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("📱 SMS 발송 중 예외 발생: {} -> {}, 에러: {}", fromNumber, phoneNumber, e.getMessage());
            return false;
        }
    }
    
    /**
     * 대기 순서 알림 SMS 발송
     * @param phoneNumber 수신자 전화번호
     * @param patientName 환자 이름
     * @param remainingCount 앞 대기자 수
     * @return 발송 성공 여부
     */
    public boolean sendWaitingNotification(String phoneNumber, String patientName, int remainingCount) {
        String message = String.format("[서대 병원 대기 알림]\n%s님, 앞 대기자가 %d명 남았습니다.\n병원 내에서 대기해주세요.", 
                                      patientName, remainingCount);
        return sendSms(phoneNumber, message);
    }
    
    /**
     * 호출 알림 SMS 발송
     * @param phoneNumber 수신자 전화번호
     * @param patientName 환자 이름
     * @return 발송 성공 여부
     */
    public boolean sendCallNotification(String phoneNumber, String patientName) {
        String message = String.format("[병원 호출 알림]\n%s님, 진료실로 입장해 주세요!", patientName);
        return sendSms(phoneNumber, message);
    }
    
    /**
     * 중복 알림 방지를 위한 발송 추적
     * @param receptionId 접수 ID
     * @return 이미 발송했는지 여부
     */
    public boolean isAlreadySent(Long receptionId) {
        return sentNotifications.containsKey(receptionId);
    }
    
    /**
     * 발송 완료 표시
     * @param receptionId 접수 ID
     */
    public void markAsSent(Long receptionId) {
        sentNotifications.put(receptionId, true);
    }
    
    /**
     * 발송 기록 초기화 (선택적)
     * @param receptionId 접수 ID
     */
    public void resetSentStatus(Long receptionId) {
        sentNotifications.remove(receptionId);
    }
    
    /**
     * 전화번호 유효성 검증
     * @param phoneNumber 전화번호
     * @return 유효한 전화번호인지 여부
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // 한국 전화번호 패턴 검증 (010, 011, 016, 017, 018, 019로 시작하는 11자리)
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        return cleanNumber.matches("^01[0-9]\\d{8}$");
    }
    
    /**
     * 전화번호 포맷팅 (하이픈 제거)
     * @param phoneNumber 전화번호
     * @return 포맷팅된 전화번호
     */
    public String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null) {
            return null;
        }
        return phoneNumber.replaceAll("[^0-9]", "");
    }
    
    /**
     * 시뮬레이션 모드 설정
     * @param simulationMode 시뮬레이션 모드 활성화 여부
     */
    public void setSimulationMode(boolean simulationMode) {
        this.simulationMode = simulationMode;
        log.info("📱 SMS 모드 변경: {}", simulationMode ? "시뮬레이션 모드" : "실제 발송 모드");
    }
    
    /**
     * 현재 시뮬레이션 모드 상태 조회
     * @return 시뮬레이션 모드 여부
     */
    public boolean isSimulationMode() {
        return simulationMode;
    }
} 