package org.example.repository;

import org.example.model.Prescription;
import org.example.model.Reception;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    
    // 접수별 처방전 조회
    Optional<Prescription> findByReception(Reception reception);
    
    // 접수 ID로 처방전 조회
    @Query("SELECT p FROM Prescription p WHERE p.reception.id = :receptionId")
    Optional<Prescription> findByReceptionId(@Param("receptionId") Long receptionId);
    
    // 상태별 처방전 조회 (생성 시간 오름차순)
    List<Prescription> findByStatusOrderByCreatedAtAsc(Prescription.PrescriptionStatus status);
    
    // 모든 처방전 조회 (생성 시간 내림차순)
    List<Prescription> findAllByOrderByCreatedAtDesc();
    
    // 오늘 작성된 처방전 수
    @Query("SELECT COUNT(p) FROM Prescription p WHERE DATE(p.createdAt) = CURRENT_DATE")
    long countTodayPrescriptions();
    
    // 이번 주 작성된 처방전 수
    @Query("SELECT COUNT(p) FROM Prescription p WHERE WEEK(p.createdAt) = WEEK(CURRENT_DATE) AND YEAR(p.createdAt) = YEAR(CURRENT_DATE)")
    long countThisWeekPrescriptions();
    
    // 이번 달 작성된 처방전 수
    @Query("SELECT COUNT(p) FROM Prescription p WHERE MONTH(p.createdAt) = MONTH(CURRENT_DATE) AND YEAR(p.createdAt) = YEAR(CURRENT_DATE)")
    long countThisMonthPrescriptions();
    
    // 특정 기간 내 생성된 처방전 수 조회
    @Query("SELECT COUNT(p) FROM Prescription p WHERE p.createdAt BETWEEN :startDate AND :endDate")
    long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                @Param("endDate") LocalDateTime endDate);
} 