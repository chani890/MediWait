package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AnnouncementRequest;
import org.example.dto.AnnouncementResponse;
import org.example.service.AnnouncementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class AnnouncementController {
    
    private final AnnouncementService announcementService;
    
    /**
     * 활성화된 공지사항 조회 (대기열 화면용)
     */
    @GetMapping("/active")
    public ResponseEntity<List<AnnouncementResponse>> getActiveAnnouncements() {
        log.info("활성화된 공지사항 조회 요청");
        List<AnnouncementResponse> announcements = announcementService.getActiveAnnouncements();
        return ResponseEntity.ok(announcements);
    }
    
    /**
     * 모든 공지사항 조회 (관리 화면용)
     */
    @GetMapping
    public ResponseEntity<List<AnnouncementResponse>> getAllAnnouncements() {
        log.info("모든 공지사항 조회 요청");
        List<AnnouncementResponse> announcements = announcementService.getAllAnnouncements();
        return ResponseEntity.ok(announcements);
    }
    
    /**
     * 공지사항 생성
     */
    @PostMapping
    public ResponseEntity<AnnouncementResponse> createAnnouncement(@RequestBody AnnouncementRequest request) {
        log.info("공지사항 생성 요청: {}", request.getTitle());
        AnnouncementResponse response = announcementService.createAnnouncement(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 공지사항 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody AnnouncementRequest request) {
        log.info("공지사항 수정 요청: {}", id);
        AnnouncementResponse response = announcementService.updateAnnouncement(id, request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 공지사항 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAnnouncement(@PathVariable Long id) {
        log.info("공지사항 삭제 요청: {}", id);
        announcementService.deleteAnnouncement(id);
        return ResponseEntity.ok().build();
    }
}


