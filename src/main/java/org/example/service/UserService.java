package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.UserInfoResponse;
import org.example.dto.UserUpdateRequest;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    
    /**
     * 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        return UserInfoResponse.from(user);
    }
    
    /**
     * 사용자 정보 수정
     */
    @Transactional
    public UserInfoResponse updateUserInfo(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        // 이름 수정
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            user.setName(request.getName());
        }
        
        // 전화번호 수정
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }
        
        // 비밀번호 수정 (현재 비밀번호 확인 필요)
        if (request.getNewPassword() != null && !request.getNewPassword().trim().isEmpty()) {
            if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
                throw new IllegalArgumentException("현재 비밀번호를 입력해주세요");
            }
            
            String encodedCurrentPassword = encodePassword(request.getCurrentPassword());
            if (!user.getPassword().equals(encodedCurrentPassword)) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다");
            }
            
            String encodedNewPassword = encodePassword(request.getNewPassword());
            user.setPassword(encodedNewPassword);
        }
        
        User updated = userRepository.save(user);
        log.info("사용자 정보 수정: {} ({})", updated.getName(), updated.getUsername());
        
        return UserInfoResponse.from(updated);
    }
    
    /**
     * 비밀번호 암호화 (SHA-256)
     */
    private String encodePassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("비밀번호 암호화 실패", e);
        }
    }
}


