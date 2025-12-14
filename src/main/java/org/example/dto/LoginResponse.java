package org.example.dto;

import lombok.*;
import org.example.model.UserRole;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    
    private Long userId;
    private String username;
    private String name;
    private UserRole role;
    private String licenseNumber;
    private String token;  // 세션 토큰 (간단하게 userId 사용)
    private String message;
}

