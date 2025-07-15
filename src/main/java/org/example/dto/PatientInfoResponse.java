package org.example.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PatientInfoResponse {
    
    private Long patientId;
    private String name;
    private LocalDate birthDate;
    private String phoneNumber;
    private LocalDateTime createdAt;
    
    // 현재 접수 정보
    private ReceptionResponse currentReception;
    private Long currentReceptionId;
    
    // 최근 문진표 정보
    private MedicalSurveyResponse latestSurvey;
    
    // 과거 진료 이력
    private List<ReceptionResponse> pastReceptions;
    
    // 통계 정보
    private int totalVisits;
    private LocalDateTime lastVisit;
} 