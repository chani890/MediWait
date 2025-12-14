package org.example.dto;

import lombok.*;
import javax.validation.constraints.NotBlank;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginRequest {
    
    @NotBlank(message = "아이디를 입력해주세요")
    private String username;
    
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;
}





