package org.example.config;

import org.example.model.Announcement;
import org.example.repository.AnnouncementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AnnouncementInitializer implements CommandLineRunner {

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Override
    public void run(String... args) throws Exception {
        // 이미 공지사항이 있으면 초기화하지 않음
        List<Announcement> existingAnnouncements = announcementRepository.findAll();
        if (!existingAnnouncements.isEmpty()) {
            System.out.println("공지사항이 이미 존재합니다. 초기화를 건너뜁니다.");
            return;
        }

        // 기본 공지사항 추가
        Announcement announcement1 = Announcement.builder()
                .title("진료 시간 안내")
                .content("평일 09:00 - 18:00\n점심시간 12:00 - 13:00")
                .displayOrder(1)
                .isActive(true)
                .build();

        Announcement announcement2 = Announcement.builder()
                .title("마스크 착용 안내")
                .content("원활한 진료를 위해 마스크 착용을 부탁드립니다")
                .displayOrder(2)
                .isActive(true)
                .build();

        announcementRepository.save(announcement1);
        announcementRepository.save(announcement2);

        System.out.println("✅ 기본 공지사항이 추가되었습니다.");
    }
}


