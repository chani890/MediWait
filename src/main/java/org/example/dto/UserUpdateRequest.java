package org.example.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateRequest {
    private String name;
    private String phoneNumber;
    private String currentPassword;  // 현재 비밀번호 (비밀번호 변경 시 필요)
    private String newPassword;  // 새 비밀번호
}


