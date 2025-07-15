package org.example.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgeGroupStatisticsResponse {
    
    private String ageGroup; // "10대", "20대", "30대" 등
    private Integer minAge; // 연령대 최소값
    private Integer maxAge; // 연령대 최대값
    private Long visitCount; // 방문자 수
    private Double percentage; // 전체 대비 비율
} 