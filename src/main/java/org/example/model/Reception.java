package org.example.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "receptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reception {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReceptionStatus status = ReceptionStatus.PENDING;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "called_at")
    private LocalDateTime calledAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "is_guardian")
    private Boolean isGuardian = false;

    @Column(name = "sms_notification_enabled")
    private Boolean smsNotificationEnabled = false;

    @Column(name = "sms_sent")
    private Boolean smsSent = false;

    public enum ReceptionStatus {
        PENDING,     // 대기 중
        CONFIRMED,   // 간호사 확인 완료
        CALLED,      // 의사 호출
        COMPLETED    // 진료 완료
    }
} 