package org.example.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionRequest {
    
    private Long receptionId;
    private String diagnosis; // 진단명
    private String symptoms; // 증상
    private String treatmentPlan; // 치료 계획
    private String medications; // 처방 약물
    private String dosageInstructions; // 복용법
    private String notes; // 의사 소견
    private LocalDateTime followUpDate; // 재진 날짜
} 