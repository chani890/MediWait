package org.example.model;

import javax.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Entity
@Table(name = "receptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reception {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceptionStatus status;
    
    @Column(nullable = false)
    private Boolean isGuardian; // 보호자 여부
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean notifyEnabled = true; // SMS 알림 활성화 여부
    
    @Column
    private Integer notifyAt; // 몇 명 남았을 때 알림 받을지 (null이면 알림 안 받음)
    
    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime confirmedAt; // 신분증 확인 시간
    
    @Column
    private LocalDateTime calledAt; // 호출 시간
    
    @Column
    private LocalDateTime completedAt; // 진료 완료 시간
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = ReceptionStatus.PENDING;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        // updatedAt = LocalDateTime.now(); // This line is removed
    }
    
    public enum ReceptionStatus {
        PENDING,     // 대기 중 (QR 접수 완료, 신분증 미확인)
        CONFIRMED,   // 확인됨 (신분증 확인 완료)
        CALLED,      // 호출됨 (의사가 호출)
        DONE,        // 진료 완료
        NO_RESPONSE, // 호출 후 미응답
        CANCELED     // 취소됨
    }
} 