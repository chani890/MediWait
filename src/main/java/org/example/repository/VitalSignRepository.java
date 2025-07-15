package org.example.repository;

import org.example.model.VitalSign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VitalSignRepository extends JpaRepository<VitalSign, Long> {
    
    /**
     * 접수 ID로 바이탈사인 조회
     */
    Optional<VitalSign> findByReceptionId(Long receptionId);
    
    /**
     * 환자 ID로 바이탈사인 목록 조회 (최신순)
     */
    @Query("SELECT vs FROM VitalSign vs WHERE vs.reception.patient.id = :patientId ORDER BY vs.createdAt DESC")
    List<VitalSign> findByPatientIdOrderByCreatedAtDesc(@Param("patientId") Long patientId);
    
    /**
     * 접수 ID로 바이탈사인 삭제
     */
    void deleteByReceptionId(Long receptionId);
} 