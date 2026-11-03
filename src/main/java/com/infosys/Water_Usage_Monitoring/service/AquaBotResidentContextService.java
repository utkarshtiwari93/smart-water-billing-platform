package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.entity.Invoice;
import com.infosys.Water_Usage_Monitoring.entity.InvoiceItem;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.entity.WaterUsageLog;
import com.infosys.Water_Usage_Monitoring.enums.NotificationType;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.InvoiceRepository;
import com.infosys.Water_Usage_Monitoring.repository.NotificationRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AquaBotResidentContextService {

    private static final int READING_LIMIT = 50;
    private static final int INVOICE_LIMIT = 12;
    private static final int ALERT_LIMIT = 20;
    private static final List<NotificationType> ALERT_TYPES =
            List.of(NotificationType.LEAK_SUSPECTED, NotificationType.OVERUSE);

    private final UserRepository userRepository;
    private final WaterUsageLogRepository usageRepository;
    private final InvoiceRepository invoiceRepository;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public AquaBotResidentContext forResident(String authenticatedEmail) {
        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated resident was not found"));
        if (user.getRole() != Role.RESIDENT) {
            throw new AccessDeniedException("AquaBot personal data is available to residents only");
        }
        if (user.getHousehold() == null) {
            return AquaBotResidentContext.withoutHousehold();
        }

        Long householdId = user.getHousehold().getId();
        List<WaterUsageLog> readings = usageRepository
                .findTop51ByHouseholdIdOrderByReadingDateDescIdDesc(householdId);
        List<Invoice> invoices = invoiceRepository
                .findTop13ByHouseholdIdOrderByIssuedAtDescIdDesc(householdId);
        var alerts = notificationRepository
                .findTop21ByUserIdAndTypeInOrderByCreatedAtDescIdDesc(user.getId(), ALERT_TYPES);

        return new AquaBotResidentContext(
                true,
                readings.stream().limit(READING_LIMIT)
                        .map(reading -> new AquaBotResidentContext.Reading(
                                reading.getReadingDate(),
                                reading.getMeterReading(),
                                reading.getConsumptionLitres()))
                        .toList(),
                invoices.stream().limit(INVOICE_LIMIT).map(this::toContext).toList(),
                alerts.stream().limit(ALERT_LIMIT)
                        .map(alert -> new AquaBotResidentContext.Alert(
                                alert.getType().name(), alert.getCreatedAt()))
                        .toList(),
                readings.size() > READING_LIMIT,
                invoices.size() > INVOICE_LIMIT,
                alerts.size() > ALERT_LIMIT);
    }

    private AquaBotResidentContext.Invoice toContext(Invoice invoice) {
        List<AquaBotResidentContext.InvoiceItem> items = invoice.getItems().stream()
                .map(this::toContext)
                .toList();
        return new AquaBotResidentContext.Invoice(
                invoice.getBillingCycle().getStartDate(),
                invoice.getBillingCycle().getEndDate(),
                invoice.getConsumptionLitres(),
                invoice.getTotalAmount(),
                invoice.getIssuedAt(),
                items);
    }

    private AquaBotResidentContext.InvoiceItem toContext(InvoiceItem item) {
        return new AquaBotResidentContext.InvoiceItem(
                item.getItemType().name(),
                item.getQuantity(),
                item.getUnitRate(),
                item.getAmount());
    }
}
