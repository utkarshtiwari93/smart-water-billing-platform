package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.HouseholdRequest;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.exception.DuplicateResourceException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseholdServiceTest {

    @Mock HouseholdRepository repo;
    @Mock ApartmentService apartmentService;
    @InjectMocks HouseholdService service;

    private Apartment apartment;

    @BeforeEach
    void setUp() {
        apartment = new Apartment();
        apartment.setId(1L);
    }

    private HouseholdRequest req(String meter) {
        return new HouseholdRequest("A-101", new BigDecimal("1200"), 4, meter);
    }

    @Test
    void create_success() {
        when(apartmentService.find(1L)).thenReturn(apartment);
        when(repo.existsByApartmentIdAndFlatNumber(1L, "A-101")).thenReturn(false);
        when(repo.findByMeterNumber("MTR-1")).thenReturn(Optional.empty());
        when(repo.save(any(Household.class))).thenAnswer(i -> i.getArgument(0));

        HouseholdResponse r = service.create(1L, req("MTR-1"));

        assertEquals("A-101", r.flatNumber());
        assertEquals(1L, r.apartmentId());
    }

    @Test
    void create_duplicateFlat_isRejected() {
        when(apartmentService.find(1L)).thenReturn(apartment);
        when(repo.existsByApartmentIdAndFlatNumber(1L, "A-101")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> service.create(1L, req("MTR-1")));
    }

    @Test
    void create_meterAlreadyAssigned_isRejected() {
        Household other = new Household();
        other.setId(99L);
        when(apartmentService.find(1L)).thenReturn(apartment);
        when(repo.existsByApartmentIdAndFlatNumber(1L, "A-101")).thenReturn(false);
        when(repo.findByMeterNumber("MTR-1")).thenReturn(Optional.of(other));

        assertThrows(DuplicateResourceException.class, () -> service.create(1L, req("MTR-1")));
    }

    @Test
    void create_blankMeter_isStoredAsNull() {
        when(apartmentService.find(1L)).thenReturn(apartment);
        when(repo.existsByApartmentIdAndFlatNumber(1L, "A-101")).thenReturn(false);
        when(repo.save(any(Household.class))).thenAnswer(i -> i.getArgument(0));

        service.create(1L, req("  "));

        ArgumentCaptor<Household> captor = ArgumentCaptor.forClass(Household.class);
        verify(repo).save(captor.capture());
        assertNull(captor.getValue().getMeterNumber());
    }

    @Test
    void get_unknownHousehold_throwsNotFound() {
        when(repo.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.get(9L));
    }
}