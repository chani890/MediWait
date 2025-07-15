package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.*;
import org.example.model.Reception;
import org.example.repository.ReceptionRepository;
import org.example.repository.PatientRepository;
import org.example.repository.PrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {
    
    private final ReceptionRepository receptionRepository;
    private final PatientRepository patientRepository;
    private final PrescriptionRepository prescriptionRepository;
    
    public DailyStatisticsResponse getDailyStatistics(LocalDate date) {
        LocalDateTime startDate = date.atStartOfDay();
        LocalDateTime endDate = date.plusDays(1).atStartOfDay();
        
        long totalReceptions = receptionRepository.countByDate(startDate, endDate);
        long completedReceptions = receptionRepository.countByStatusAndDate(Reception.ReceptionStatus.DONE, startDate, endDate);
        long noShowReceptions = receptionRepository.countByStatusAndDate(Reception.ReceptionStatus.NO_RESPONSE, startDate, endDate) +
                               receptionRepository.countByStatusAndDate(Reception.ReceptionStatus.CANCELED, startDate, endDate);
        long guardianReceptions = receptionRepository.countGuardianReceptionsByDate(startDate, endDate);
        
        double guardianRatio = totalReceptions > 0 ? (double) guardianReceptions / totalReceptions * 100 : 0;
        double completionRate = totalReceptions > 0 ? (double) completedReceptions / totalReceptions * 100 : 0;
        double noShowRate = totalReceptions > 0 ? (double) noShowReceptions / totalReceptions * 100 : 0;
        
        return DailyStatisticsResponse.builder()
            .date(date)
            .totalReceptions(totalReceptions)
            .completedReceptions(completedReceptions)
            .noShowReceptions(noShowReceptions)
            .guardianReceptions(guardianReceptions)
            .guardianRatio(guardianRatio)
            .completionRate(completionRate)
            .noShowRate(noShowRate)
            .build();
    }
    
    public DailyStatisticsResponse getTodayStatistics() {
        return getDailyStatistics(LocalDate.now());
    }
    
    /**
     * 간호사용 종합 통계 조회
     */
    @Transactional(readOnly = true)
    public NurseStatisticsResponse getNurseStatistics(LocalDate startDate, LocalDate endDate) {
        log.info("간호사용 통계 조회: {} ~ {}", startDate, endDate);
        
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();
        
        // 기본 통계 계산
        List<Reception> receptions = receptionRepository.findByCreatedAtBetween(startDateTime, endDateTime);
        long totalVisits = receptions.size();
        
        // 신규 환자 수 계산 (해당 기간 중 첫 접수인 환자들)
        // 간단하게 해당 기간 중 처음 접수한 환자 수로 계산
        long totalNewPatients = receptions.stream()
                .map(r -> r.getPatient().getId())
                .distinct()
                .count();
                
        long totalPrescriptions = prescriptionRepository.countByCreatedAtBetween(startDateTime, endDateTime);
        
        // 평균 대기시간 계산 (확인 시간 - 접수 시간)
        double averageWaitingTime = receptions.stream()
                .filter(r -> r.getConfirmedAt() != null)
                .mapToLong(r -> ChronoUnit.MINUTES.between(r.getCreatedAt(), r.getConfirmedAt()))
                .average()
                .orElse(0.0);
        
        // 세부 통계 생성
        List<HourlyStatisticsResponse> hourlyStats = getHourlyStatistics(receptions);
        List<WeeklyStatisticsResponse> weeklyStats = getWeeklyStatistics(receptions);
        List<AgeGroupStatisticsResponse> ageGroupStats = getAgeGroupStatistics(receptions);
        
        // 피크 시간 찾기
        String busiestHour = hourlyStats.stream()
                .max(Comparator.comparing(HourlyStatisticsResponse::getVisitCount))
                .map(HourlyStatisticsResponse::getTimeRange)
                .orElse("정보 없음");
        
        String busiestDay = weeklyStats.stream()
                .max(Comparator.comparing(WeeklyStatisticsResponse::getVisitCount))
                .map(WeeklyStatisticsResponse::getDayName)
                .orElse("정보 없음");
        
        // 기간 설명 생성
        String periodDescription = generatePeriodDescription(startDate, endDate);
        
        return NurseStatisticsResponse.builder()
                .startDate(startDate)
                .endDate(endDate)
                .periodDescription(periodDescription)
                .totalVisits(totalVisits)
                .totalNewPatients(totalNewPatients)
                .totalPrescriptions(totalPrescriptions)
                .averageWaitingTime(averageWaitingTime)
                .hourlyStats(hourlyStats)
                .weeklyStats(weeklyStats)
                .ageGroupStats(ageGroupStats)
                .busiestHour(busiestHour)
                .busiestDay(busiestDay)
                .build();
    }
    
    /**
     * 시간대별 통계 계산
     */
    private List<HourlyStatisticsResponse> getHourlyStatistics(List<Reception> receptions) {
        Map<Integer, Long> hourlyCount = receptions.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().getHour(),
                        Collectors.counting()
                ));
        
        long totalCount = receptions.size();
        
        return hourlyCount.entrySet().stream()
                .map(entry -> {
                    int hour = entry.getKey();
                    long count = entry.getValue();
                    double percentage = totalCount > 0 ? (double) count / totalCount * 100 : 0;
                    
                    return HourlyStatisticsResponse.builder()
                            .hour(hour)
                            .timeRange(String.format("%02d:00-%02d:00", hour, hour + 1))
                            .visitCount(count)
                            .percentage(Math.round(percentage * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(HourlyStatisticsResponse::getHour))
                .collect(Collectors.toList());
    }
    
    /**
     * 요일별 통계 계산
     */
    private List<WeeklyStatisticsResponse> getWeeklyStatistics(List<Reception> receptions) {
        Map<Integer, Long> weeklyCount = receptions.stream()
                .collect(Collectors.groupingBy(
                        r -> r.getCreatedAt().getDayOfWeek().getValue(),
                        Collectors.counting()
                ));
        
        long totalCount = receptions.size();
        String[] dayNames = {"월요일", "화요일", "수요일", "목요일", "금요일", "토요일", "일요일"};
        
        return weeklyCount.entrySet().stream()
                .map(entry -> {
                    int dayOfWeek = entry.getKey();
                    long count = entry.getValue();
                    double percentage = totalCount > 0 ? (double) count / totalCount * 100 : 0;
                    
                    return WeeklyStatisticsResponse.builder()
                            .dayOfWeek(dayOfWeek)
                            .dayName(dayNames[dayOfWeek - 1])
                            .visitCount(count)
                            .percentage(Math.round(percentage * 100.0) / 100.0)
                            .build();
                })
                .sorted(Comparator.comparing(WeeklyStatisticsResponse::getDayOfWeek))
                .collect(Collectors.toList());
    }
    
    /**
     * 연령대별 통계 계산
     */
    private List<AgeGroupStatisticsResponse> getAgeGroupStatistics(List<Reception> receptions) {
        Map<String, Long> ageGroupCount = receptions.stream()
                .filter(r -> r.getPatient().getBirthDate() != null)
                .collect(Collectors.groupingBy(
                        r -> {
                            int age = Period.between(r.getPatient().getBirthDate(), LocalDate.now()).getYears();
                            if (age < 10) return "10세 미만";
                            else if (age < 20) return "10대";
                            else if (age < 30) return "20대";
                            else if (age < 40) return "30대";
                            else if (age < 50) return "40대";
                            else if (age < 60) return "50대";
                            else if (age < 70) return "60대";
                            else return "70세 이상";
                        },
                        Collectors.counting()
                ));
        
        long totalCount = receptions.stream()
                .filter(r -> r.getPatient().getBirthDate() != null)
                .count();
        
        // 연령대 순서 정의
        List<String> ageGroups = Arrays.asList("10세 미만", "10대", "20대", "30대", "40대", "50대", "60대", "70세 이상");
        
        return ageGroups.stream()
                .map(ageGroup -> {
                    long count = ageGroupCount.getOrDefault(ageGroup, 0L);
                    double percentage = totalCount > 0 ? (double) count / totalCount * 100 : 0;
                    
                    // 연령대 범위 설정
                    int minAge = 0, maxAge = 0;
                    switch (ageGroup) {
                        case "10세 미만": minAge = 0; maxAge = 9; break;
                        case "10대": minAge = 10; maxAge = 19; break;
                        case "20대": minAge = 20; maxAge = 29; break;
                        case "30대": minAge = 30; maxAge = 39; break;
                        case "40대": minAge = 40; maxAge = 49; break;
                        case "50대": minAge = 50; maxAge = 59; break;
                        case "60대": minAge = 60; maxAge = 69; break;
                        case "70세 이상": minAge = 70; maxAge = 999; break;
                    }
                    
                    return AgeGroupStatisticsResponse.builder()
                            .ageGroup(ageGroup)
                            .minAge(minAge)
                            .maxAge(maxAge)
                            .visitCount(count)
                            .percentage(Math.round(percentage * 100.0) / 100.0)
                            .build();
                })
                .filter(stat -> stat.getVisitCount() > 0) // 방문자가 있는 연령대만 포함
                .collect(Collectors.toList());
    }
    
    /**
     * 기간 설명 생성
     */
    private String generatePeriodDescription(LocalDate startDate, LocalDate endDate) {
        LocalDate today = LocalDate.now();
        
        if (startDate.equals(today) && endDate.equals(today)) {
            return "오늘";
        } else if (startDate.equals(today.minusDays(1)) && endDate.equals(today.minusDays(1))) {
            return "어제";
        } else if (startDate.equals(today.with(java.time.DayOfWeek.MONDAY)) && 
                   endDate.equals(today.with(java.time.DayOfWeek.SUNDAY))) {
            return "이번 주";
        } else if (startDate.equals(today.withDayOfMonth(1)) && 
                   endDate.equals(today.withDayOfMonth(today.lengthOfMonth()))) {
            return "이번 달";
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M월 d일");
            return startDate.format(formatter) + " ~ " + endDate.format(formatter);
        }
    }
    
    /**
     * 오늘 간호사 통계 조회
     */
    public NurseStatisticsResponse getTodayNurseStatistics() {
        LocalDate today = LocalDate.now();
        return getNurseStatistics(today, today);
    }
    
    /**
     * 이번 주 간호사 통계 조회
     */
    public NurseStatisticsResponse getThisWeekNurseStatistics() {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(java.time.DayOfWeek.MONDAY);
        LocalDate endOfWeek = today.with(java.time.DayOfWeek.SUNDAY);
        return getNurseStatistics(startOfWeek, endOfWeek);
    }
    
    /**
     * 이번 달 간호사 통계 조회
     */
    public NurseStatisticsResponse getThisMonthNurseStatistics() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        LocalDate endOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        return getNurseStatistics(startOfMonth, endOfMonth);
    }
} 