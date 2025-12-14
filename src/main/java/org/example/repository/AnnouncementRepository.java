package org.example.repository;

import org.example.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    
    // 활성화된 공지사항만 조회 (표시 순서대로)
    List<Announcement> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    // 모든 공지사항 조회 (표시 순서대로)
    List<Announcement> findAllByOrderByDisplayOrderAsc();
}


