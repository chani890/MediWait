package org.example.model;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "medical_surveys")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicalSurvey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reception_id", nullable = false)
    private Reception reception;
    
    @Column(columnDefinition = "TEXT")
    private String symptoms; // 증상
    
    @Column(columnDefinition = "TEXT")
    private String allergies; // 알레르기
    
    @Column(columnDefinition = "TEXT")
    private String medications; // 복용 중인 약물
    
    @Column(columnDefinition = "TEXT")
    private String medicalHistory; // 과거 병력
    
    @Column
    private String visitReason; // 내원 사유
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        // updatedAt = LocalDateTime.now(); // This line is removed
    }
} 