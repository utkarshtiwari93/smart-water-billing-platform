package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.CsvImportResult;
import com.infosys.Water_Usage_Monitoring.dto.ApartmentUsageResponse;
import com.infosys.Water_Usage_Monitoring.dto.ResidentBenchmarkResponse;
import com.infosys.Water_Usage_Monitoring.dto.UsageRequest;
import com.infosys.Water_Usage_Monitoring.dto.UsageResponse;
import com.infosys.Water_Usage_Monitoring.dto.UsageTrendResponse;
import com.infosys.Water_Usage_Monitoring.dto.UsagePeriodComparisonResponse;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.service.ConsumptionAggregationService;
import com.infosys.Water_Usage_Monitoring.service.CsvImportService;
import com.infosys.Water_Usage_Monitoring.service.UsageService;
import com.infosys.Water_Usage_Monitoring.security.AdminApartmentScope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class UsageController {

    private final UsageService usageService;
    private final CsvImportService csvImportService;
    private final AdminApartmentScope apartmentScope;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UsageResponse create(@Valid @RequestBody UsageRequest r, Authentication authentication) {
        apartmentScope.household(r.householdId(), authentication.getName());
        return usageService.createManual(r);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public CsvImportResult upload(@RequestParam("file") MultipartFile file, Authentication authentication) {
        Long apartmentId = apartmentScope.assignedApartmentId(authentication.getName());
        return csvImportService.importUsage(file, apartmentId);
    }

    @GetMapping
    public List<UsageResponse> list(
            @RequestParam Long householdId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            apartmentScope.household(householdId, authentication.getName());
        }
        return usageService.list(householdId, from, to, authentication.getName());
    }

    @GetMapping("/apartment")
    @PreAuthorize("hasRole('ADMIN')")
    public ApartmentUsageResponse apartmentUsage(
            @RequestParam Long apartmentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return usageService.apartmentUsage(apartmentId, from, to, authentication.getName());
    }

    @GetMapping("/trend")
    public UsageTrendResponse trend(
            @RequestParam Long householdId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "DAY") String granularity,
            Authentication authentication) {
        ConsumptionAggregationService.Granularity parsed;
        try {
            parsed = ConsumptionAggregationService.Granularity.valueOf(granularity.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("granularity must be DAY or MONTH");
        }
        return usageService.trend(householdId, from, to, parsed, authentication.getName());
    }

    @GetMapping("/compare")
    public UsagePeriodComparisonResponse compare(
            @RequestParam Long householdId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return usageService.compare(householdId, from, to, authentication.getName());
    }

    @GetMapping("/benchmark")
    @PreAuthorize("hasRole('RESIDENT')")
    public ResidentBenchmarkResponse benchmark(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Authentication authentication) {
        return usageService.benchmark(from, to, authentication.getName());
    }
}