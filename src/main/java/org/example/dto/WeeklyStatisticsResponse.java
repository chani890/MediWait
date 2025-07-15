package org.example.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyStatisticsResponse {
    
    private Integer dayOfWeek; // 1(월요일)-7(일요일)
    private String dayName; // "월요일", "화요일" 등
    private Long visitCount; // 방문자 수
    private Double percentage; // 전체 대비 비율
} 