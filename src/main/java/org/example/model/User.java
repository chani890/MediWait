package org.example.model;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;  // 로그인 ID
    
    @Column(nullable = false, length = 255)
    private String password;  // 암호화된 비밀번호
    
    @Column(nullable = false, length = 50)
    private String name;  // 실명
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;  // ADMIN, DOCTOR, NURSE
    
    @Column(length = 50)
    private String licenseNumber;  // 의사 면허번호 (의사만)
    
    @Column(length = 20)
    private String phoneNumber;  // 연락처
    
    @Column(nullable = false)
    private Boolean isActive = true;  // 활성화 여부
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

