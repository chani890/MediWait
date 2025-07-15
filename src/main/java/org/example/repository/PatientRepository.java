package org.example.repository;

import org.example.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    Optional<Patient> findByNameAndBirthDateAndPhoneNumber(String name, LocalDate birthDate, String phoneNumber);
    
    @Query("SELECT p FROM Patient p WHERE p.name = :name AND p.birthDate = :birthDate")
    Optional<Patient> findByNameAndBirthDate(@Param("name") String name, @Param("birthDate") LocalDate birthDate);
    
    List<Patient> findByNameContainingOrderByCreatedAtDesc(String name);
    
    boolean existsByNameAndBirthDateAndPhoneNumber(String name, LocalDate birthDate, String phoneNumber);
} 