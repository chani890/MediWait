package org.example.model;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "vital_sign")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VitalSign {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reception_id", nullable = false)
    private Reception reception;
    
    @Column(name = "body_temp")
    private Double bodyTemp; // 체온 (°C)
    
    @Column(name = "blood_pressure")
    private String bloodPressure; // 혈압 (예: "120/80")
    
    @Column(name = "pulse")
    private Integer pulse; // 맥박 (bpm)
    
    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms; // 증상 (체크박스 선택사항들)
    
    @Column(name = "other_symptoms", columnDefinition = "TEXT")
    private String otherSymptoms; // 기타 증상 (직접 입력)
    
    @Column(name = "medical_history", columnDefinition = "TEXT")
    private String medicalHistory; // 과거력 (체크박스 선택사항들)
    
    @Column(name = "nurse_notes", columnDefinition = "TEXT")
    private String nurseNotes; // 간호사 메모
    
    @Column(name = "nurse_id")
    private String nurseId; // 입력한 간호사 ID
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
    }
} 