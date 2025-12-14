package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.UserInfoResponse;
import org.example.dto.UserUpdateRequest;
import org.example.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class UserController {
    
    private final UserService userService;
    
    /**
     * 사용자 정보 조회
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserInfoResponse> getUserInfo(@PathVariable Long userId) {
        log.info("사용자 정보 조회 요청: {}", userId);
        UserInfoResponse response = userService.getUserInfo(userId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 사용자 정보 수정
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserInfoResponse> updateUserInfo(
            @PathVariable Long userId,
            @RequestBody UserUpdateRequest request) {
        log.info("사용자 정보 수정 요청: {}", userId);
        UserInfoResponse response = userService.updateUserInfo(userId, request);
        return ResponseEntity.ok(response);
    }
}


