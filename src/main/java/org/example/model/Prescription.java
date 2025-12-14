package org.example.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "prescriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prescription {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "reception_id", nullable = false)
    private Reception reception;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id")
    private User doctor;  // 처방의 (의사)
    
    @Column(name = "doctor_name", length = 50)
    private String doctorName;  // 처방의 성명
    
    @Column(name = "doctor_license_number", length = 50)
    private String doctorLicenseNumber;  // 의사 면허번호
    
    @Column(name = "diagnosis", columnDefinition = "TEXT")
    private String diagnosis; // 진단명
    
    @Column(name = "symptoms", columnDefinition = "TEXT")
    private String symptoms; // 증상
    
    @Column(name = "treatment_plan", columnDefinition = "TEXT")
    private String treatmentPlan; // 치료 계획
    
    @Column(name = "medications", columnDefinition = "TEXT")
    private String medications; // 처방 약물
    
    @Column(name = "dosage_instructions", columnDefinition = "TEXT")
    private String dosageInstructions; // 복용법
    
    @Column(name = "additional_notes", columnDefinition = "TEXT")
    private String additionalNotes; // 추가 소견
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes; // 의사 소견
    
    @Column(name = "follow_up_date")
    private LocalDateTime followUpDate; // 재진 날짜
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    @Builder.Default
    private PrescriptionStatus status = PrescriptionStatus.PENDING; // 처방전 상태
    
    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정 시간
    
    public enum PrescriptionStatus {
        PENDING,    // 대기 중
        COMPLETED,  // 조제 완료
        CANCELLED   // 취소
    }
} 