package org.example.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HourlyStatisticsResponse {
    
    private Integer hour; // 0-23시
    private String timeRange; // "09:00-10:00" 형태
    private Long visitCount; // 방문자 수
    private Double percentage; // 전체 대비 비율
} 