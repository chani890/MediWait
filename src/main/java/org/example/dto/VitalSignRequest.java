package org.example.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VitalSignRequest {
    
    private Long receptionId; // 접수 ID
    private Double bodyTemp; // 체온
    private String bloodPressure; // 혈압
    private Integer pulse; // 맥박
    private String symptoms; // 증상
    private String otherSymptoms; // 기타 증상
    private String medicalHistory; // 과거력
    private String nurseNotes; // 간호사 메모
    private String nurseId; // 간호사 ID
} 