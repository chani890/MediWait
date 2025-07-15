package org.example.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class MedicalSurveyResponse {
    
    private Long id;
    private Long patientId;
    private Long receptionId;
    private String symptoms;
    private String allergies;
    private String medications;
    private String medicalHistory;
    private String visitReason;

    private LocalDateTime createdAt;
} 