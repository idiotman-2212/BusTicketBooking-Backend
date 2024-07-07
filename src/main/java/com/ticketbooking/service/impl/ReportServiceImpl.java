package com.ticketbooking.service.impl;

import com.ticketbooking.dto.*;
import com.ticketbooking.repo.ReportRepo;
import com.ticketbooking.service.ReportService;
import com.ticketbooking.utils.AppConstants;
import com.ticketbooking.utils.DateTimeUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepo reportRepo;

    @Override
    public ReportResponse getTotalRevenue(String startDate, String endDate, String timeOption) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startLocalDate, endLocalDate;

        if (startDate == null || startDate.isEmpty())
            startLocalDate = LocalDate.now();
        else startLocalDate = LocalDate.parse(startDate, formatter);

        if (endDate == null || endDate.isEmpty())
            endLocalDate = LocalDate.now();
        else endLocalDate = LocalDate.parse(endDate, formatter);

        return createRevenueReport(startLocalDate, endLocalDate, timeOption);
    }

    @Override
    public ReportResponse getWeekTotalRevenueOfCurrentDate(String currentDate) {
        LocalDate currDate;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (currentDate == null || currentDate.isEmpty()) {
            currDate = LocalDate.now();
        } else currDate = LocalDate.parse(currentDate, formatter);

        LocalDate firstDateOfWeek = DateTimeUtils.getFirstDayOfWeek(currDate);
        LocalDate lastDateOfWeek = DateTimeUtils.getLastDayOfWeek(currDate);

        return createRevenueReport(firstDateOfWeek, lastDateOfWeek, "WEEK");
    }

    @Override
    public ReportResponse getCoachUsage(String startDate, String endDate, String timeOption) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startLocalDate, endLocalDate;

        if (startDate == null || startDate.isEmpty())
            startLocalDate = LocalDate.now();
        else startLocalDate = LocalDate.parse(startDate, formatter);

        if (endDate == null || endDate.isEmpty())
            endLocalDate = LocalDate.now();
        else endLocalDate = LocalDate.parse(endDate, formatter);
        return createUsageReport(startLocalDate, endLocalDate, timeOption);
    }

    @Override
    public ReportResponse getTopRoute(String startDate, String endDate, String timeOption) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate startLocalDate, endLocalDate;

        if (startDate == null || startDate.isEmpty())
            startLocalDate = LocalDate.now();
        else startLocalDate = LocalDate.parse(startDate, formatter);

        if (endDate == null || endDate.isEmpty())
            endLocalDate = LocalDate.now();
        else endLocalDate = LocalDate.parse(endDate, formatter);
        return createTopRouteReport(startLocalDate, endLocalDate, timeOption);
    }

    private ReportResponse createTopRouteReport(LocalDate startLocalDate, LocalDate endLocalDate, String timeOption) {
        ReportResponse reportResponse = new ReportResponse();
        switch (timeOption.trim().toUpperCase()) {
            case "DAY": {
                break;
            }
            case "WEEK": {

                break;
            }
            case "MONTH": {
                LocalDate start = LocalDate.of(startLocalDate.getYear(), startLocalDate.getMonthValue(), 1);
                LocalDate end = LocalDate.of(endLocalDate.getYear(),
                        endLocalDate.getMonthValue(),
                        DateTimeUtils.getTotalDaysInMonthOfYear(endLocalDate));
                List<TopRouteDto> topRouteDtoList = reportRepo.getMonthTopRoute(start, end);
                Map<String, Long> topRoutes = new LinkedHashMap<>();
                for (int i = 0; i < topRouteDtoList.size(); i++) {
                    TopRouteDto topRouteDto = topRouteDtoList.get(0);
                    topRoutes.put(
                            topRouteDto.getRoute(),
                            topRouteDto.getCount()
                    );
                }
                reportResponse.setReportData(topRoutes);
                break;
            }
            case "YEAR": {

                break;
            }
            default:
                break;
        }
        return reportResponse;
    }

    private ReportResponse createUsageReport(LocalDate startLocalDate, LocalDate endLocalDate, String timeOption) {
        ReportResponse reportResponse = new ReportResponse();
        switch (timeOption.trim().toUpperCase()) {
            case "DAY": {
                break;
            }
            case "WEEK": {
                break;
            }
            case "MONTH": {
                LocalDate start = LocalDate.of(startLocalDate.getYear(), startLocalDate.getMonthValue(), 1);
                LocalDate end = LocalDate.of(endLocalDate.getYear(),
                        endLocalDate.getMonthValue(),
                        DateTimeUtils.getTotalDaysInMonthOfYear(endLocalDate));
                List<CoachUsageDto> coachUsageDtoList = reportRepo.getCoachUsage(start, end);
                Map<String, Long> usages = new LinkedHashMap<>();
                String[] coachLabels = {"", "Bed", "Limousine", "Chair"};
                for (int i = 1; i <= 3; i++) {
                    usages.put(
                            coachLabels[i],
                            countCoachUsage(coachLabels[i].toUpperCase(), coachUsageDtoList)
                    );
                }
                reportResponse.setReportData(usages);
                break;
            }
            case "YEAR": {
                break;
            }
            default:
                break;
        }
        return reportResponse;
    }

    private Long countCoachUsage(String coachLabel, List<CoachUsageDto> coachUsageDtoList) {
        return coachUsageDtoList
                .stream()
                .filter(coachUsage -> coachUsage.getCoachType().name().equals(coachLabel))
                .mapToLong(CoachUsageDto::getUsage)
                .sum();
    }

    private ReportResponse createRevenueReport(LocalDate startLocalDate, LocalDate endLocalDate, String timeOption) {
        ReportResponse reportResponse = new ReportResponse();
        switch (timeOption.trim().toUpperCase()) {
            case "DAY": {
                break;
            }
            case "WEEK": {
                List<WeekRevenueDto> revenueDtoList = reportRepo.getWeekTotalRevenue(startLocalDate, endLocalDate);
                Map<String, BigDecimal> revenues = new LinkedHashMap<>();
                for (int i = 1; i <= 7; i++) {
                    revenues.put(
                            "%s (%d/%d)".formatted(AppConstants.WEEK_COLUMNS[i],
                                    startLocalDate.getDayOfMonth(), startLocalDate.getMonthValue()),
                            BigDecimal.valueOf(
                                    countTotalRevenueForWeek(startLocalDate, revenueDtoList)
                            )
                    );
                    startLocalDate = startLocalDate.plusDays(1);
                }
                reportResponse.setReportData(revenues);
                break;
            }
            case "MONTH": {
                // e.g: 2022/07/15 -> 2023/07/20, so we need to start from 2022/07/01 -> 2023/07/31
                LocalDate start = LocalDate.of(startLocalDate.getYear(), startLocalDate.getMonthValue(), 1);
                LocalDate end = LocalDate.of(endLocalDate.getYear(),
                        endLocalDate.getMonthValue(),
                        DateTimeUtils.getTotalDaysInMonthOfYear(endLocalDate));

                List<MonthRevenueDto> revenueDtoList = reportRepo.getMonthTotalRevenue(start, end);
                Map<String, BigDecimal> revenues = new LinkedHashMap<>();
                long totalMonthBetween = DateTimeUtils.getTotalMonthBetween(start, end);
                for (int i = 1; i <= totalMonthBetween; i++) {
                    revenues.put("%d/%d".formatted(start.getMonthValue(), start.getYear()),
                            BigDecimal.valueOf(
                                    countTotalRevenueForMonthYear(
                                            start.getMonthValue(),
                                            start.getYear(),
                                            revenueDtoList)
                            )
                    );
                    start = start.plusMonths(1);
                }
                reportResponse.setReportData(revenues);
                break;
            }
            case "YEAR": {
                int startYear = startLocalDate.getYear(), endYear = endLocalDate.getYear();
                List<YearRevenueDto> revenueDTOList = reportRepo.getYearTotalRevenue(startYear, endYear);
                Map<String, BigDecimal> revenues = new LinkedHashMap<>();
                for (int i = startYear; i <= endYear; i++) {
                    revenues.put(String.valueOf(i), BigDecimal.valueOf(countTotalRevenueForYear(i, revenueDTOList)));
                }
                reportResponse.setReportData(revenues);
                break;
            }
            default:
                break;
        }

        return reportResponse;
    }

    private Double countTotalRevenueForWeek(LocalDate date, List<WeekRevenueDto> revenueDtoList) {
        return revenueDtoList
                .stream()
                .filter(revenue -> revenue.getDay().equals(date.getDayOfMonth())
                        && revenue.getMonth().equals(date.getMonthValue())
                        && revenue.getYear().equals(date.getYear()))
                .mapToDouble(revenue -> revenue.getTotalRevenue().doubleValue())
                .sum();
    }

    private Double countTotalRevenueForMonthYear(int monthValue, int year, List<MonthRevenueDto> revenueDtoList) {
        return revenueDtoList
                .stream()
                .filter(revenue -> revenue.getMonth().equals(monthValue) && revenue.getYear().equals(year))
                .mapToDouble(revenue -> revenue.getTotalRevenue().doubleValue())
                .sum();
    }

    private Double countTotalRevenueForYear(int year, List<YearRevenueDto> revenueDTOList) {
        return revenueDTOList
                .stream()
                .filter(revenue -> revenue.getYear().equals(year))
                .mapToDouble(revenue -> revenue.getTotalRevenue().doubleValue())
                .sum();
    }
}
