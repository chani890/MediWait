package org.example.repository;

import org.example.model.MedicalSurvey;
import org.example.model.Patient;
import org.example.model.Reception;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalSurveyRepository extends JpaRepository<MedicalSurvey, Long> {
    
    Optional<MedicalSurvey> findByReception(Reception reception);
    
    List<MedicalSurvey> findByPatientOrderByCreatedAtDesc(Patient patient);
    
    @Query("SELECT ms FROM MedicalSurvey ms WHERE ms.patient.id = :patientId ORDER BY ms.createdAt DESC")
    List<MedicalSurvey> findByPatientIdOrderByCreatedAtDesc(@Param("patientId") Long patientId);
    
    void deleteByReceptionId(Long receptionId);
} 