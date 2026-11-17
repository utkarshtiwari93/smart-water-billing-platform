package com.infosys.Water_Usage_Monitoring.security;

import com.infosys.Water_Usage_Monitoring.entity.Adjustment;
import com.infosys.Water_Usage_Monitoring.entity.BillingCycle;
import com.infosys.Water_Usage_Monitoring.entity.BulkPurchase;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.Invoice;
import com.infosys.Water_Usage_Monitoring.entity.TariffPlan;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.AdjustmentRepository;
import com.infosys.Water_Usage_Monitoring.repository.ApartmentRepository;
import com.infosys.Water_Usage_Monitoring.repository.BillingCycleRepository;
import com.infosys.Water_Usage_Monitoring.repository.BulkPurchaseRepository;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.InvoiceRepository;
import com.infosys.Water_Usage_Monitoring.repository.TariffPlanRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminApartmentScope {

    private final UserRepository userRepository;
    private final ApartmentRepository apartmentRepository;
    private final HouseholdRepository householdRepository;
    private final BillingCycleRepository billingCycleRepository;
    private final TariffPlanRepository tariffPlanRepository;
    private final BulkPurchaseRepository bulkPurchaseRepository;
    private final AdjustmentRepository adjustmentRepository;
    private final InvoiceRepository invoiceRepository;

    public Long assignedApartmentId(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
        if (user.getRole() != Role.ADMIN || user.getApartment() == null) {
            throw new AccessDeniedException("Administrator is not assigned to an apartment");
        }
        return user.getApartment().getId();
    }

    public void apartment(Long apartmentId, String email) {
        apartmentRepository.findById(apartmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + apartmentId));
        requireApartment(apartmentId, assignedApartmentId(email));
    }

    public void household(Long householdId, String email) {
        Household household = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + householdId));
        requireApartment(household.getApartment().getId(), assignedApartmentId(email));
    }

    public void user(Long userId, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        requireApartment(user.getApartment().getId(), assignedApartmentId(email));
    }

    public void cycle(Long cycleId, String email) {
        BillingCycle cycle = billingCycleRepository.findById(cycleId)
                .orElseThrow(() -> new ResourceNotFoundException("Billing cycle not found: " + cycleId));
        requireApartment(cycle.getApartment().getId(), assignedApartmentId(email));
    }

    public void tariff(Long tariffId, String email) {
        TariffPlan tariff = tariffPlanRepository.findById(tariffId)
                .orElseThrow(() -> new ResourceNotFoundException("Tariff plan not found: " + tariffId));
        requireApartment(tariff.getApartment().getId(), assignedApartmentId(email));
    }

    public void purchase(Long purchaseId, String email) {
        BulkPurchase purchase = bulkPurchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Bulk purchase not found: " + purchaseId));
        requireApartment(purchase.getBillingCycle().getApartment().getId(), assignedApartmentId(email));
    }

    public void adjustment(Long adjustmentId, String email) {
        Adjustment adjustment = adjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Adjustment not found: " + adjustmentId));
        requireApartment(adjustment.getBillingCycle().getApartment().getId(), assignedApartmentId(email));
    }

    public void invoice(Long invoiceId, String email) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found: " + invoiceId));
        requireApartment(invoice.getHousehold().getApartment().getId(), assignedApartmentId(email));
    }

    private void requireApartment(Long requestedApartmentId, Long assignedApartmentId) {
        if (!assignedApartmentId.equals(requestedApartmentId)) {
            throw new AccessDeniedException("You can only access data for your assigned apartment");
        }
    }
}
