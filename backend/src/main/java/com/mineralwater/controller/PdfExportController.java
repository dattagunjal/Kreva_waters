package com.mineralwater.controller;

import com.mineralwater.service.PdfExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/sales/export")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class PdfExportController {

    private final PdfExportService pdfExportService;

    // GET /api/admin/sales/export/daily?date=2024-06-14
    @GetMapping("/daily")
    public ResponseEntity<byte[]> exportDailyPdf(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        if (date == null) date = LocalDate.now();
        byte[] pdf = pdfExportService.exportDailySalesPdf(date);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ugamwaters-daily-sales-" + date + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    // GET /api/admin/sales/export/monthly?year=2024&month=6
    @GetMapping("/monthly")
    public ResponseEntity<byte[]> exportMonthlyPdf(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {

        if (year == null)  year  = LocalDate.now().getYear();
        if (month == null) month = LocalDate.now().getMonthValue();

        byte[] pdf = pdfExportService.exportMonthlySalesPdf(year, month);
        String monthName = Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH).toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"ugamwaters-monthly-sales-" + monthName + "-" + year + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }
}
