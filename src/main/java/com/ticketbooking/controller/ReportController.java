package com.ticketbooking.controller;

import com.ticketbooking.dto.ReportResponse;
import com.ticketbooking.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/revenues/{startDate}/{endDate}/{timeOption}")
    public ReportResponse getTotalRevenue(
            @PathVariable String startDate,
            @PathVariable String endDate,
            @PathVariable String timeOption) {
        return reportService.getTotalRevenue(startDate, endDate, timeOption);
    }

    @GetMapping("/revenues/{currentDate}")
    public ReportResponse getWeekTotalRevenueOfCurrentDate(@PathVariable String currentDate) {
        return reportService.getWeekTotalRevenueOfCurrentDate(currentDate);
    }

    @GetMapping("/usages/{startDate}/{endDate}/{timeOption}")
    public ReportResponse getCoachUsage(
            @PathVariable String startDate,
            @PathVariable String endDate,
            @PathVariable String timeOption
    ) {
        return reportService.getCoachUsage(startDate, endDate, timeOption);
    }

    @GetMapping("/toproute/{startDate}/{endDate}/{timeOption}")
    public ReportResponse getTopRoute(
            @PathVariable String startDate,
            @PathVariable String endDate,
            @PathVariable String timeOption
    ) {
        return reportService.getTopRoute(startDate, endDate, timeOption);
    }
}
