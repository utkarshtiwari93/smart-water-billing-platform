package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.DistributionResponse;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdShareResponse;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.Notification;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.enums.NotificationType;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.BillingCycleRepository;
import com.infosys.Water_Usage_Monitoring.repository.NotificationRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AquaBotAdminContextService {

    private static final int PERIOD_LIMIT = 12;
    private static final int ALERT_LIMIT = 50;
    private static final int HOUSEHOLD_RANKING_LIMIT = 5;
    private static final int SHARED_ALLOCATION_LIMIT = 30;
    private static final List<NotificationType> ALERT_TYPES =
            List.of(NotificationType.LEAK_SUSPECTED, NotificationType.OVERUSE);

    private final UserRepository userRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final WaterUsageLogRepository usageRepository;
    private final NotificationRepository notificationRepository;
    private final DistributionService distributionService;
    private final BulkPurchaseService bulkPurchaseService;
    private final InvoiceService invoiceService;

    @Transactional(readOnly = true)
    public AquaBotAdminContext forAdmin(String authenticatedEmail) {
        User admin = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated administrator was not found"));
        if (admin.getRole() != Role.ADMIN || admin.getApartment() == null) {
            throw new AccessDeniedException("Apartment analytics are available to assigned administrators only");
        }

        Long apartmentId = admin.getApartment().getId();
        List<BillingCycle> cycles = billingCycleRepository
                .findTop13ByApartmentIdOrderByStartDateDescIdDesc(apartmentId);
        if (cycles.stream().anyMatch(cycle -> cycle.getApartment() == null
                || !apartmentId.equals(cycle.getApartment().getId()))) {
            throw new AccessDeniedException("Billing cycle is outside the administrator's apartment");
        }
        List<BillingCycle> selectedCycles = cycles.stream().limit(PERIOD_LIMIT).toList();
        List<AquaBotAdminContext.PeriodSummary> periods = selectedCycles.stream()
                .map(cycle -> summarizePeriod(apartmentId, cycle))
                .toList();

        List<Notification> queriedAlerts = notificationRepository.findLatestApartmentAlerts(
                apartmentId, ALERT_TYPES, PageRequest.of(0, ALERT_LIMIT * 3));
        List<AquaBotAdminContext.AlertSummary> alerts = uniqueAlerts(queriedAlerts);

        int openCycleCount = Math.toIntExact(
                billingCycleRepository.countByApartmentIdAndStatus(apartmentId, CycleStatus.OPEN));
        return new AquaBotAdminContext(
                periods,
                alerts.stream().limit(ALERT_LIMIT).toList(),
                openCycleCount,
                cycles.size() > PERIOD_LIMIT,
                queriedAlerts.size() >= ALERT_LIMIT * 3 || alerts.size() > ALERT_LIMIT,
                false);
    }

    private AquaBotAdminContext.PeriodSummary summarizePeriod(Long apartmentId, BillingCycle cycle) {
        DistributionResponse distribution = distributionService.calculate(cycle.getId());
        var purchaseSummary = bulkPurchaseService.summary(cycle.getId());
        var householdUsage = usageRepository.summarizeApartmentHouseholdConsumption(
                apartmentId, cycle.getStartDate(), cycle.getEndDate());

        BigDecimal totalConsumption = householdUsage.stream()
                .map(row -> nz(row.getConsumptionLitres()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = householdUsage.isEmpty()
                ? null
                : totalConsumption.divide(BigDecimal.valueOf(householdUsage.size()), 2, RoundingMode.HALF_UP);

        List<AquaBotAdminContext.HouseholdConsumption> descending = householdUsage.stream()
                .sorted(Comparator.comparing(
                        (com.infosys.Water_Usage_Monitoring.repository.ApartmentHouseholdConsumption row) ->
                                nz(row.getConsumptionLitres())).reversed()
                        .thenComparing(com.infosys.Water_Usage_Monitoring.repository.ApartmentHouseholdConsumption
                                ::getFlatNumber))
                .map(row -> new AquaBotAdminContext.HouseholdConsumption(
                        row.getFlatNumber(), nz(row.getConsumptionLitres())))
                .toList();
        List<AquaBotAdminContext.HouseholdConsumption> highest =
                descending.stream().limit(HOUSEHOLD_RANKING_LIMIT).toList();
        List<AquaBotAdminContext.HouseholdConsumption> lowest = new ArrayList<>(descending);
        lowest.sort(Comparator.comparing(AquaBotAdminContext.HouseholdConsumption::consumptionLitres)
                .thenComparing(AquaBotAdminContext.HouseholdConsumption::flatNumber));

        InvoiceService.CycleSummary invoiceSummary = cycle.getStatus() == CycleStatus.OPEN
                ? new InvoiceService.CycleSummary(0, BigDecimal.ZERO)
                : invoiceService.summarizeForCycle(cycle.getId());

        List<AquaBotAdminContext.HouseholdAllocation> allocations = distribution.households().stream()
                .sorted(Comparator.comparing(HouseholdShareResponse::flatNumber))
                .limit(SHARED_ALLOCATION_LIMIT)
                .map(share -> new AquaBotAdminContext.HouseholdAllocation(
                        share.flatNumber(), share.allocationBasis(), share.sharedAllocation()))
                .toList();

        return new AquaBotAdminContext.PeriodSummary(
                cycle.getStartDate(),
                cycle.getEndDate(),
                cycle.getStatus().name(),
                cycle.getStatus() != CycleStatus.OPEN,
                totalConsumption,
                householdUsage.size(),
                average,
                highest,
                lowest.stream().limit(HOUSEHOLD_RANKING_LIMIT).toList(),
                allocations,
                purchaseSummary.totalVolumeKl(),
                purchaseSummary.totalCost(),
                distribution.sharedCost(),
                distribution.surplus(),
                invoiceSummary.invoiceTotal(),
                Math.toIntExact(invoiceSummary.invoiceCount()),
                cycle.getStatus() == CycleStatus.OPEN,
                distribution.households().size() > SHARED_ALLOCATION_LIMIT);
    }

    private List<AquaBotAdminContext.AlertSummary> uniqueAlerts(List<Notification> notifications) {
        Set<String> seen = new HashSet<>();
        return notifications.stream()
                .filter(notification -> seen.add(notification.getHousehold().getId() + ":"
                        + notification.getUsageLogId() + ":" + notification.getType()))
                .map(notification -> new AquaBotAdminContext.AlertSummary(
                        notification.getType().name(),
                        notification.getHousehold().getFlatNumber(),
                        notification.getCreatedAt(),
                        notification.getMessage()))
                .toList();
    }

    private BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
