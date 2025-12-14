package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AnnouncementRequest;
import org.example.dto.AnnouncementResponse;
import org.example.model.Announcement;
import org.example.repository.AnnouncementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnouncementService {
    
    private final AnnouncementRepository announcementRepository;
    
    /**
     * 활성화된 공지사항 조회 (대기열 화면용)
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getActiveAnnouncements() {
        return announcementRepository.findByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(AnnouncementResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 모든 공지사항 조회 (관리 화면용)
     */
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAllAnnouncements() {
        return announcementRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(AnnouncementResponse::from)
                .collect(Collectors.toList());
    }
    
    /**
     * 공지사항 생성
     */
    @Transactional
    public AnnouncementResponse createAnnouncement(AnnouncementRequest request) {
        Announcement announcement = Announcement.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();
        
        Announcement saved = announcementRepository.save(announcement);
        log.info("공지사항 생성: {}", saved.getTitle());
        
        return AnnouncementResponse.from(saved);
    }
    
    /**
     * 공지사항 수정
     */
    @Transactional
    public AnnouncementResponse updateAnnouncement(Long id, AnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다: " + id));
        
        if (request.getTitle() != null) {
            announcement.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            announcement.setContent(request.getContent());
        }
        if (request.getDisplayOrder() != null) {
            announcement.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            announcement.setIsActive(request.getIsActive());
        }
        
        Announcement updated = announcementRepository.save(announcement);
        log.info("공지사항 수정: {}", updated.getTitle());
        
        return AnnouncementResponse.from(updated);
    }
    
    /**
     * 공지사항 삭제
     */
    @Transactional
    public void deleteAnnouncement(Long id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("공지사항을 찾을 수 없습니다: " + id));
        
        announcementRepository.delete(announcement);
        log.info("공지사항 삭제: {}", announcement.getTitle());
    }
}


