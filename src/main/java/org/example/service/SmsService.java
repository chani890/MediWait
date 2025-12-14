package org.example.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class SmsService {
    
    @Value("${sms.api.key}")
    private String apiKey;
    
    @Value("${sms.api.secret}")
    private String apiSecret;
    
    @Value("${sms.api.from}")
    private String fromNumber;
    
    private final RestTemplate restTemplate;
    
    // SMS 모드 설정 (시뮬레이션 모드 기본값: true)
    private boolean simulationMode = true;
    
    // SMS 알림 발송 시점 (기본값: 2번째 순서)
    private int smsNotifyTiming = 2;
    
    public SmsService() {
        this.restTemplate = new RestTemplate();
    }
    
    /**
     * SMS 발송 메서드
     * @param phoneNumber 수신자 전화번호
     * @param patientName 환자 이름
     * @return 발송 성공 여부
     */
    public boolean sendWaitingNotification(String phoneNumber, String patientName) {
        // 시뮬레이션 모드인 경우 실제 발송하지 않고 로그만 출력
        if (simulationMode) {
            log.info("[시뮬레이션 모드] SMS 발송: {} -> {} (실제 발송 없음)", phoneNumber, patientName);
            return true;
        }
        
        try {
            String message = String.format("[병원 알림] %s님, 앞에 대기인원이 1명 남았습니다. 병원 내에서 대기해주세요.", patientName);
            
            // 현재 시간을 ISO 8601 형식으로 생성
            String timestamp = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            
            String salt = String.valueOf(System.nanoTime());
            
            // 서명 생성을 위한 문자열
            String data = timestamp + salt;
            String signature = generateHmacSha256(data, apiSecret);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", new HashMap<String, Object>() {{
                put("to", phoneNumber);
                put("from", fromNumber);
                put("text", message);
            }});
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", String.format("HMAC-SHA256 =%s, date=%s, salt=%s, signature=%s", 
                    apiKey, timestamp, salt, signature));
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.coolsms.co.kr/messages/v4/send",
                HttpMethod.POST,
                entity,
                String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("SMS 발송 성공: {} -> {}", phoneNumber, patientName);
                return true;
            } else {
                log.error("SMS 발송 실패: {}", response.getBody());
                return false;
            }
            
        } catch (Exception e) {
            log.error("SMS 발송 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * SMS 모드 설정
     * @param simulationMode 시뮬레이션 모드 여부
     */
    public void setSimulationMode(boolean simulationMode) {
        this.simulationMode = simulationMode;
        log.info("SMS 모드 변경: {}", simulationMode ? "시뮬레이션 모드" : "실제 발송 모드");
    }
    
    /**
     * SMS 모드 조회
     * @return 시뮬레이션 모드 여부
     */
    public boolean isSimulationMode() {
        return simulationMode;
    }
    
    /**
     * SMS 알림 발송 시점 설정
     * @param timing 발송 시점 (1-5)
     */
    public void setSmsNotifyTiming(int timing) {
        if (timing >= 1 && timing <= 5) {
            this.smsNotifyTiming = timing;
            log.info("SMS 알림 발송 시점 변경: {}번째 순서", timing);
        } else {
            log.warn("잘못된 SMS 알림 발송 시점: {}", timing);
        }
    }
    
    /**
     * SMS 알림 발송 시점 조회
     * @return 발송 시점
     */
    public int getSmsNotifyTiming() {
        return smsNotifyTiming;
    }
    
    /**
     * HMAC-SHA256 서명 생성
     */
    private String generateHmacSha256(String data, String key) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            
            byte[] signedBytes = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : signedBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
            
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("HMAC-SHA256 서명 생성 실패: {}", e.getMessage());
            return "dummy_signature";
        }
    }
    
    /**
     * 전화번호 형식 검증
     */
    public boolean isValidPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return false;
        }
        
        // 한국 전화번호 형식 검증 (010-XXXX-XXXX 또는 01XXXXXXXXX)
        String cleanNumber = phoneNumber.replaceAll("[^0-9]", "");
        return cleanNumber.matches("^010\\d{8}$");
    }
} 