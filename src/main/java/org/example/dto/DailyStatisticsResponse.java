package org.example.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDate;

@Data
@Builder
public class DailyStatisticsResponse {
    
    private LocalDate date;
    private long totalReceptions;      // 총 접수 수
    private long completedReceptions;  // 진료 완료 수
    private long noShowReceptions;     // 노쇼 수
    private long guardianReceptions;   // 보호자 접수 수
    private double guardianRatio;      // 보호자 접수 비율 (%)
    private double completionRate;     // 진료 완료율 (%)
    private double noShowRate;         // 노쇼율 (%)
} 