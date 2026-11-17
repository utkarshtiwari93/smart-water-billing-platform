package com.infosys.Water_Usage_Monitoring.security;

import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.Invoice;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.repository.AdjustmentRepository;
import com.infosys.Water_Usage_Monitoring.repository.ApartmentRepository;
import com.infosys.Water_Usage_Monitoring.repository.BillingCycleRepository;
import com.infosys.Water_Usage_Monitoring.repository.BulkPurchaseRepository;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.InvoiceRepository;
import com.infosys.Water_Usage_Monitoring.repository.TariffPlanRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminApartmentScopeTest {

    @Test
    void invoice_isRestrictedToAdminsAssignedApartment() {
        UserRepository users = mock(UserRepository.class);
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        AdminApartmentScope scope = new AdminApartmentScope(
                users,
                mock(ApartmentRepository.class),
                mock(HouseholdRepository.class),
                mock(BillingCycleRepository.class),
                mock(TariffPlanRepository.class),
                mock(BulkPurchaseRepository.class),
                mock(AdjustmentRepository.class),
                invoices);

        Apartment assignedApartment = apartment(1L);
        Apartment otherApartment = apartment(2L);
        User admin = new User();
        admin.setRole(Role.ADMIN);
        admin.setApartment(assignedApartment);

        Household household = new Household();
        household.setApartment(otherApartment);
        Invoice invoice = new Invoice();
        invoice.setHousehold(household);

        when(users.findByEmail("admin@example.test")).thenReturn(Optional.of(admin));
        when(invoices.findById(42L)).thenReturn(Optional.of(invoice));

        assertThrows(AccessDeniedException.class, () -> scope.invoice(42L, "admin@example.test"));
    }

    private Apartment apartment(Long id) {
        Apartment apartment = new Apartment();
        apartment.setId(id);
        return apartment;
    }
}
