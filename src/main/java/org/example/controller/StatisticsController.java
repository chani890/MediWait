package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.DailyStatisticsResponse;
import org.example.service.StatisticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    /**
     * 오늘 통계 조회
     */
    @GetMapping("/today")
    public ResponseEntity<DailyStatisticsResponse> getTodayStatistics() {
        DailyStatisticsResponse statistics = statisticsService.getTodayStatistics();
        return ResponseEntity.ok(statistics);
    }
    
    /**
     * 특정 날짜 통계 조회
     */
    @GetMapping("/daily")
    public ResponseEntity<DailyStatisticsResponse> getDailyStatistics(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        DailyStatisticsResponse statistics = statisticsService.getDailyStatistics(date);
        return ResponseEntity.ok(statistics);
    }
} 