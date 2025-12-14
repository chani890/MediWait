package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.dto.SignupRequest;
import org.example.dto.SignupResponse;
import org.example.model.User;
import org.example.model.UserRole;
import org.example.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    
    /**
     * 회원가입
     */
    @Transactional
    public SignupResponse signup(SignupRequest request) {
        log.info("회원가입 시도: username={}, role={}", request.getUsername(), request.getRole());
        
        // 1. 아이디 중복 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다: " + request.getUsername());
        }
        
        // 2. 역할별 유효성 검사
        validateByRole(request);
        
        // 3. 비밀번호 암호화
        String encodedPassword = encodePassword(request.getPassword());
        
        // 4. 사용자 생성
        User user = User.builder()
                .username(request.getUsername())
                .password(encodedPassword)
                .name(request.getName())
                .role(request.getRole())
                .licenseNumber(request.getLicenseNumber())
                .phoneNumber(request.getPhoneNumber())
                .isActive(true)
                .build();
        
        User savedUser = userRepository.save(user);
        log.info("회원가입 성공: id={}, username={}, role={}", 
                savedUser.getId(), savedUser.getUsername(), savedUser.getRole());
        
        // 5. 응답 생성
        return SignupResponse.builder()
                .id(savedUser.getId())
                .username(savedUser.getUsername())
                .name(savedUser.getName())
                .role(savedUser.getRole())
                .licenseNumber(savedUser.getLicenseNumber())
                .phoneNumber(savedUser.getPhoneNumber())
                .message("회원가입이 완료되었습니다")
                .build();
    }
    
    /**
     * 역할별 유효성 검사
     */
    private void validateByRole(SignupRequest request) {
        UserRole role = request.getRole();
        
        // 의사는 면허번호 필수
        if (role == UserRole.DOCTOR) {
            if (request.getLicenseNumber() == null || request.getLicenseNumber().trim().isEmpty()) {
                throw new IllegalArgumentException("의사는 면허번호가 필수입니다");
            }
        }
    }
    
    /**
     * 로그인
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        log.info("로그인 시도: username={}", request.getUsername());
        
        // 1. 사용자 조회
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다"));
        
        // 2. 비밀번호 확인
        String encodedPassword = encodePassword(request.getPassword());
        if (!user.getPassword().equals(encodedPassword)) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다");
        }
        
        // 3. 활성화 상태 확인
        if (!user.getIsActive()) {
            throw new IllegalArgumentException("비활성화된 계정입니다. 관리자에게 문의하세요");
        }
        
        log.info("로그인 성공: userId={}, username={}, role={}", 
                user.getId(), user.getUsername(), user.getRole());
        
        // 4. 응답 생성
        return LoginResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .name(user.getName())
                .role(user.getRole())
                .licenseNumber(user.getLicenseNumber())
                .token(String.valueOf(user.getId()))  // 간단하게 userId를 토큰으로 사용
                .message("로그인 성공")
                .build();
    }
    
    /**
     * 비밀번호 암호화 (SHA-256)
     * 나중에 BCrypt로 변경 권장
     */
    private String encodePassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("비밀번호 암호화 실패", e);
            throw new RuntimeException("비밀번호 암호화에 실패했습니다", e);
        }
    }
}

