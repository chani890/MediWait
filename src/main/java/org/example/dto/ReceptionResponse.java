package org.example.dto;

import lombok.Data;
import lombok.Builder;
import org.example.model.Reception.ReceptionStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class ReceptionResponse {
    
    private Long id;
    private Long patientId;
    private String patientName;
    private LocalDate birthDate; // 생년월일 추가
    private String phoneNumber;
    private ReceptionStatus status;
    private Boolean isGuardian;
    private Boolean notifyEnabled; // SMS 알림 활성화 여부
    private Integer notifyAt;
    private LocalDateTime createdAt;
    private LocalDateTime confirmedAt;
    private LocalDateTime calledAt;
    private LocalDateTime completedAt;
    private int waitingPosition; // 대기 순번
    private boolean isNewPatient; // 신규 환자 여부
    private boolean hasSurvey; // 문진표 작성 여부
} 