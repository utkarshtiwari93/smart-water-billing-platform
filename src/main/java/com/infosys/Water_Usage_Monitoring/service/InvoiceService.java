package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.DistributionResponse;
import com.infosys.Water_Usage_Monitoring.dto.FinalizeResponse;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdShareResponse;
import com.infosys.Water_Usage_Monitoring.dto.InvoiceItemResponse;
import com.infosys.Water_Usage_Monitoring.dto.InvoicePdfData;
import com.infosys.Water_Usage_Monitoring.dto.InvoiceResponse;
import com.infosys.Water_Usage_Monitoring.entity.Adjustment;
import com.infosys.Water_Usage_Monitoring.entity.AlertSettings;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.Invoice;
import com.infosys.Water_Usage_Monitoring.entity.InvoiceItem;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.enums.InvoiceItemType;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ConflictException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.AdjustmentRepository;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.InvoiceRepository;
import com.infosys.Water_Usage_Monitoring.repository.InvoiceCycleTotals;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.repository.WaterUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final BillingCycleService cycleService;
    private final DistributionService distributionService;
    private final TariffCalculator calculator;
    private final HouseholdRepository householdRepo;
    private final AdjustmentRepository adjustmentRepo;
    private final InvoiceRepository invoiceRepo;
    private final UserRepository userRepo;
    private final WaterUsageLogRepository usageRepo;
    private final AlertSettingsService alertSettingsService;
    private final AlertMailer mailer;
    @Value("${app.billing.payment-instructions:Please pay through the payment channel configured by your apartment administrator.}")
    private String paymentInstructions;

    @Transactional
    public FinalizeResponse finalizeCycle(Long cycleId) {
        BillingCycle cycle = cycleService.find(cycleId);
        if (cycle.getStatus() != CycleStatus.OPEN)
            throw new ConflictException("Only OPEN billing cycles can be finalized (current status: "
                    + cycle.getStatus() + ")");

        DistributionResponse distribution = distributionService.calculate(cycleId);
        if (distribution.households().isEmpty())
            throw new BadRequestException("No households to bill in this apartment");

        TariffPlan plan = cycle.getTariffPlan();
        Map<Long, List<Adjustment>> adjustments = adjustmentRepo.findByBillingCycleIdOrderByIdAsc(cycleId)
                .stream().collect(Collectors.groupingBy(a -> a.getHousehold().getId()));

        BigDecimal totalInvoiced = BigDecimal.ZERO;

        for (HouseholdShareResponse share : distribution.households()) {
            Household household = householdRepo.findById(share.householdId())
                    .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + share.householdId()));

            Invoice invoice = new Invoice();
            invoice.setInvoiceNumber("INV-C" + cycleId + "-H" + household.getId());
            invoice.setBillingCycle(cycle);
            invoice.setHousehold(household);
            invoice.setConsumptionLitres(share.consumptionLitres());

            TariffCalculator.Charge charge = calculator.calculate(share.consumptionLitres(), plan);
            for (TariffCalculator.SlabCharge slab : charge.slabs()) {
                if (slab.consumptionKl().signum() == 0) continue;
                InvoiceItemType itemType = slab.slabNumber() == 1
                        ? InvoiceItemType.WATER_TIER1 : InvoiceItemType.WATER_TIER2;
                invoice.addItem(item(itemType, slabDescription(slab), slab.consumptionKl(),
                        slab.rate(), slab.amount()));
            }

            if (share.sharedAllocation().signum() > 0)
                invoice.addItem(item(InvoiceItemType.SHARED_AREA,
                        "Shared area and common usage (" + basisText(share.allocationBasis()) + ")",
                        null, null, share.sharedAllocation()));

            for (Adjustment a : adjustments.getOrDefault(household.getId(), List.of()))
                invoice.addItem(item(InvoiceItemType.ADJUSTMENT, a.getReason(), null, null, a.getAmount()));

            BigDecimal total = invoice.getItems().stream()
                    .map(InvoiceItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
            invoice.setTotalAmount(total);

            invoiceRepo.save(invoice);
            sendInvoiceEmail(household, invoice);
            totalInvoiced = totalInvoiced.add(total);
        }

        cycle.setStatus(CycleStatus.FINALIZED);
        return new FinalizeResponse(cycleId, cycle.getStatus().name(),
                distribution.households().size(), totalInvoiced);
    }

    @Transactional
    public FinalizeResponse archive(Long cycleId) {
        BillingCycle cycle = cycleService.find(cycleId);
        if (cycle.getStatus() != CycleStatus.FINALIZED)
            throw new ConflictException("Only FINALIZED billing cycles can be archived (current status: "
                    + cycle.getStatus() + ")");
        cycle.setStatus(CycleStatus.ARCHIVED);
        return new FinalizeResponse(cycleId, cycle.getStatus().name(), 0, BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> listForCycle(Long cycleId) {
        cycleService.find(cycleId);
        return invoiceRepo.findByBillingCycleIdOrderByHouseholdFlatNumberAsc(cycleId)
                .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public CycleSummary summarizeForCycle(Long cycleId) {
        cycleService.find(cycleId);
        InvoiceCycleTotals totals = invoiceRepo.summarizeByBillingCycleId(cycleId);
        return new CycleSummary(totals.getInvoiceCount(), totals.getInvoiceTotal());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse get(Long id, String email) {
        Invoice invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        requireInvoiceAccess(invoice, email);
        return toDto(invoice);
    }

    @Transactional(readOnly = true)
    public InvoicePdfData getPdfData(Long id, String email) {
        Invoice invoice = invoiceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + id));
        requireInvoiceAccess(invoice, email);

        BillingCycle cycle = invoice.getBillingCycle();
        Household household = invoice.getHousehold();
        Long householdId = household.getId();
        var previous = usageRepo.findTopByHouseholdIdAndReadingDateLessThanOrderByReadingDateDesc(
                householdId, cycle.getStartDate());
        var current = usageRepo.findTopByHouseholdIdAndReadingDateBetweenOrderByReadingDateDesc(
                householdId, cycle.getStartDate(), cycle.getEndDate());
        var apartment = household.getApartment();
        return new InvoicePdfData(toDto(invoice), apartment.getName(), apartment.getAddress(), apartment.getCity(),
                household.getMeterNumber(),
                previous.map(log -> log.getReadingDate()).orElse(null),
                previous.map(log -> log.getMeterReading()).orElse(null),
                current.map(log -> log.getReadingDate()).orElse(null),
                current.map(log -> log.getMeterReading()).orElse(null));
    }

    private void requireInvoiceAccess(Invoice invoice, String email) {
        User user = findUser(email);

        if (user.getRole() == Role.RESIDENT
                && (user.getHousehold() == null
                || !user.getHousehold().getId().equals(invoice.getHousehold().getId())))
            throw new AccessDeniedException("You can only view your own household's invoices");
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> myInvoices(String email) {
        User user = findUser(email);
        if (user.getHousehold() == null)
            throw new BadRequestException("You are not assigned to a household");
        return invoiceRepo.findByHouseholdIdOrderByIssuedAtDesc(user.getHousehold().getId())
                .stream().map(this::toDto).toList();
    }

    private User findUser(String email) {
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private InvoiceItem item(InvoiceItemType type, String description, BigDecimal quantity,
                             BigDecimal unitRate, BigDecimal amount) {
        InvoiceItem i = new InvoiceItem();
        i.setItemType(type);
        i.setDescription(description.length() > 255 ? description.substring(0, 255) : description);
        i.setQuantity(quantity);
        i.setUnitRate(unitRate);
        i.setAmount(amount);
        return i;
    }

    private String basisText(String basis) {
        return switch (basis) {
            case "CONSUMPTION" -> "by consumption";
            case "AREA_ESTIMATE" -> "by flat area, no meter";
            case "AREA" -> "by flat area";
            default -> "equal split";
        };
    }

    private String slabDescription(TariffCalculator.SlabCharge slab) {
        String from = slab.fromKl().stripTrailingZeros().toPlainString();
        if (slab.upToKl() == null)
            return slab.slabNumber() == 1
                    ? "Water charge - slab " + slab.slabNumber() + " (all usage)"
                    : "Water charge - slab " + slab.slabNumber() + " (above " + from + " kL)";
        String to = slab.upToKl().stripTrailingZeros().toPlainString();
        return "Water charge - slab " + slab.slabNumber() + " (" + from + " to " + to + " kL)";
    }

    private void sendInvoiceEmail(Household household, Invoice invoice) {
        AlertSettings settings = alertSettingsService.getOrCreate(household.getApartment().getId());
        if (settings == null || !settings.isEmailEnabled()) return;

        for (User resident : userRepo.findByHouseholdId(household.getId())) {
            if (resident.getRole() == Role.RESIDENT) {
                mailer.sendHtml(resident.getEmail(), "Water bill " + invoice.getInvoiceNumber(),
                        TransactionalEmailTemplates.invoice(household, invoice, paymentInstructions));
                mailer.sendHtml(resident.getEmail(), "Water-saving tips for your billing period",
                        TransactionalEmailTemplates.waterSavingTips(household, invoice));
            }
        }
    }

    private InvoiceResponse toDto(Invoice inv) {
        List<InvoiceItemResponse> items = new ArrayList<>();
        for (InvoiceItem i : inv.getItems())
            items.add(new InvoiceItemResponse(i.getItemType().name(), i.getDescription(),
                    i.getQuantity(), i.getUnitRate(), i.getAmount()));

        return new InvoiceResponse(inv.getId(), inv.getInvoiceNumber(), inv.getBillingCycle().getId(),
                inv.getBillingCycle().getStartDate(), inv.getBillingCycle().getEndDate(),
                inv.getHousehold().getId(), inv.getHousehold().getFlatNumber(),
                inv.getConsumptionLitres(), inv.getTotalAmount(), inv.getIssuedAt(), items);
    }

    public record CycleSummary(long invoiceCount, BigDecimal invoiceTotal) {
    }
}