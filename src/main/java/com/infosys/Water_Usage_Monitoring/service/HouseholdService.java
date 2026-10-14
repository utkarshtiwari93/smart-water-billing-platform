package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.HouseholdRequest;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.exception.DuplicateResourceException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.service.ApartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseholdService {
    private final HouseholdRepository repo;
    private final ApartmentService apartmentService;

    @Transactional
    public HouseholdResponse create(Long apartmentId, HouseholdRequest r) {
        Apartment apt = apartmentService.find(apartmentId);
        if (repo.existsByApartmentIdAndFlatNumber(apartmentId, r.flatNumber()))
            throw new DuplicateResourceException("Flat already exists: " + r.flatNumber());
        checkMeter(r.meterNumber(), null);

        Household h = new Household();
        h.setApartment(apt);
        apply(h, r);
        return toDto(repo.save(h));
    }

    @Transactional(readOnly = true)
    public List<HouseholdResponse> listByApartment(Long apartmentId) {
        apartmentService.find(apartmentId);
        return repo.findByApartmentId(apartmentId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public HouseholdResponse get(Long id) { return toDto(find(id)); }

    @Transactional
    public HouseholdResponse update(Long id, HouseholdRequest r) {
        Household h = find(id);
        checkMeter(r.meterNumber(), id);
        apply(h, r);
        return toDto(h);
    }

    public void delete(Long id) { repo.delete(find(id)); }

    private Household find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + id));
    }

    private void checkMeter(String meter, Long selfId) {
        if (meter == null || meter.isBlank()) return;
        repo.findByMeterNumber(meter).ifPresent(existing -> {
            if (!existing.getId().equals(selfId))
                throw new DuplicateResourceException("Meter already assigned: " + meter);
        });
    }

    private void apply(Household h, HouseholdRequest r) {
        h.setFlatNumber(r.flatNumber());
        h.setAreaSqft(r.areaSqft());
        h.setOccupancy(r.occupancy());
        h.setMeterNumber(r.meterNumber() == null || r.meterNumber().isBlank() ? null : r.meterNumber());
    }

    private HouseholdResponse toDto(Household h) {
        return new HouseholdResponse(h.getId(), h.getApartment().getId(), h.getFlatNumber(),
                h.getAreaSqft(), h.getOccupancy(), h.getMeterNumber());
    }
}