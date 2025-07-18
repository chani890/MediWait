package org.example.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientRegistrationRequest {
    
    @NotBlank(message = "이름은 필수입니다")
    private String name;
    
    @NotNull(message = "생년월일은 필수입니다")
    private LocalDate birthDate;
    
    @NotBlank(message = "전화번호는 필수입니다")
    @Pattern(regexp = "^\\d{3}-\\d{4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)")
    private String phoneNumber;

    private String visitReason;
    private String symptoms;
    private String allergies;
    private String medications;
    private String medicalHistory;
} 