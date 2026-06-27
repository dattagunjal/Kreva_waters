package com.mineralwater.controller;

import com.mineralwater.service.SalesService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SalesController {

    private final SalesService salesService;

    // GET /api/admin/sales/daily?date=2024-06-14
    @GetMapping("/daily")
    public ResponseEntity<Map<String, Object>> getDailySales(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        if (date == null) date = LocalDate.now();
        return ResponseEntity.ok(salesService.getDailySales(date));
    }

    // GET /api/admin/sales/monthly?year=2024&month=6
    @GetMapping("/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlySales(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        if (year == null) year = LocalDate.now().getYear();
        if (month == null) month = LocalDate.now().getMonthValue();
        return ResponseEntity.ok(salesService.getMonthlySales(year, month));
    }

    // GET /api/admin/sales/monthly-chart?year=2024
    @GetMapping("/monthly-chart")
    public ResponseEntity<Map<String, Object>> getYearlyChart(
            @RequestParam(required = false) Integer year) {
        if (year == null) year = LocalDate.now().getYear();
        return ResponseEntity.ok(salesService.getYearlyChartData(year));
    }
}
