package org.example.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NurseStatisticsResponse {
    
    // 기간 정보
    private LocalDate startDate;
    private LocalDate endDate;
    private String periodDescription; // "오늘", "이번 주", "이번 달" 등
    
    // 전체 통계
    private Long totalVisits; // 총 방문자 수
    private Long totalNewPatients; // 신규 환자 수
    private Long totalPrescriptions; // 총 처방전 수
    private Double averageWaitingTime; // 평균 대기시간 (분)
    
    // 세부 통계
    private List<HourlyStatisticsResponse> hourlyStats; // 시간대별 통계
    private List<WeeklyStatisticsResponse> weeklyStats; // 요일별 통계
    private List<AgeGroupStatisticsResponse> ageGroupStats; // 연령대별 통계
    
    // 피크 시간 정보
    private String busiestHour; // 가장 바쁜 시간대
    private String busiestDay; // 가장 바쁜 요일
} 