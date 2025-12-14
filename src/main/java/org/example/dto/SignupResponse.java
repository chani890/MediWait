package org.example.dto;

import lombok.*;
import org.example.model.UserRole;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupResponse {
    
    private Long id;
    private String username;
    private String name;
    private UserRole role;
    private String licenseNumber;
    private String phoneNumber;
    private String message;
}

