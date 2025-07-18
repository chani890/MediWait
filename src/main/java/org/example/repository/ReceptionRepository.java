package org.example.repository;

import org.example.model.Reception;
import org.example.model.Reception.ReceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReceptionRepository extends JpaRepository<Reception, Long> {
    
    List<Reception> findByStatusOrderByCreatedAtAsc(ReceptionStatus status);
    
    List<Reception> findByStatusOrderByConfirmedAtAsc(ReceptionStatus status);
    
    List<Reception> findByStatusOrderByCalledAtAsc(ReceptionStatus status);
    
    @Query("SELECT r FROM Reception r WHERE r.status = :status AND r.createdAt >= :startDate AND r.createdAt < :endDate ORDER BY r.confirmedAt ASC")
    List<Reception> findByStatusAndDateOrderByConfirmedAtAsc(@Param("status") ReceptionStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT r FROM Reception r WHERE r.status = 'CALLED' AND r.calledAt < :cutoffTime")
    List<Reception> findNoResponseReceptions(@Param("cutoffTime") LocalDateTime cutoffTime);
    
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.status = 'CONFIRMED' AND r.confirmedAt < :confirmedAt")
    int countConfirmedBefore(@Param("confirmedAt") LocalDateTime confirmedAt);
    
    /**
     * 동시성 제어를 위한 락이 적용된 다음 호출 가능한 환자 조회
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Reception r WHERE r.status = 'CONFIRMED' ORDER BY r.confirmedAt ASC")
    List<Reception> findNextPatientToCallWithLock();
    
    /**
     * 특정 접수의 상태를 원자적으로 CONFIRMED에서 CALLED로 변경
     */
    @Modifying
    @Query("UPDATE Reception r SET r.status = 'CALLED', r.calledAt = :calledAt WHERE r.id = :receptionId AND r.status = 'CONFIRMED'")
    int updateStatusToCalledIfConfirmed(@Param("receptionId") Long receptionId, @Param("calledAt") LocalDateTime calledAt);
    
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.createdAt >= :startDate AND r.createdAt < :endDate")
    long countByDate(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.status = :status AND r.createdAt >= :startDate AND r.createdAt < :endDate")
    long countByStatusAndDate(@Param("status") ReceptionStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.isGuardian = true AND r.createdAt >= :startDate AND r.createdAt < :endDate")
    long countGuardianReceptionsByDate(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    // 간호사용 통계를 위한 메서드들
    List<Reception> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    
    // 환자별 접수 조회 (생성일 기준 오름차순)
    @Query("SELECT r FROM Reception r WHERE r.patient.id = :patientId ORDER BY r.createdAt ASC")
    List<Reception> findByPatientIdOrderByCreatedAtAsc(@Param("patientId") Long patientId);
    
    // 대기 중인 환자들 순서대로 조회 (확인된 환자들)
    @Query("SELECT r FROM Reception r WHERE r.status = 'CONFIRMED' ORDER BY r.confirmedAt ASC")
    List<Reception> findConfirmedReceptionsInOrder();
    
    // 특정 접수보다 앞서 확인된 대기 중인 환자 수 계산
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.status = 'CONFIRMED' AND r.confirmedAt < :confirmedAt")
    int countConfirmedReceptionsBefore(@Param("confirmedAt") LocalDateTime confirmedAt);
    
    /**
     * 현재 호출된 환자가 있는지 확인
     */
    @Query("SELECT COUNT(r) FROM Reception r WHERE r.status = 'CALLED'")
    int countCalledReceptions();
    
    /**
     * 현재 호출된 환자 목록 조회
     */
    @Query("SELECT r FROM Reception r WHERE r.status = 'CALLED' ORDER BY r.calledAt DESC")
    List<Reception> findCalledReceptions();
    
    /**
     * 특정 상태의 접수 개수 조회
     */
    long countByStatus(ReceptionStatus status);
    
    /**
     * Patient 정보를 함께 fetch하는 접수 조회
     */
    @Query("SELECT r FROM Reception r JOIN FETCH r.patient WHERE r.id = :id")
    Optional<Reception> findByIdWithPatient(@Param("id") Long id);
    
    /**
     * 날짜 범위 내 접수 조회 (Patient 정보 포함)
     */
    @Query("SELECT r FROM Reception r JOIN FETCH r.patient WHERE r.createdAt BETWEEN :startDate AND :endDate")
    List<Reception> findByCreatedAtBetweenWithPatient(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
} 