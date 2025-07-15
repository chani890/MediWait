package org.example.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.example.model.Prescription;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrescriptionResponse {
    
    private Long id;
    private Long receptionId;
    private String patientName;
    private String patientPhoneNumber;
    private String diagnosis; // 진단명
    private String symptoms; // 증상
    private String treatmentPlan; // 치료 계획
    private String medications; // 처방 약물
    private String dosageInstructions; // 복용법
    private String additionalNotes; // 추가 소견
    private String notes; // 의사 소견
    private LocalDateTime followUpDate; // 재진 날짜
    private Prescription.PrescriptionStatus status; // 처방전 상태
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // 수정 시간
} 