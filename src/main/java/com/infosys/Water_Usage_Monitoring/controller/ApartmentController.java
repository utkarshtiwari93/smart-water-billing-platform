package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.ApartmentRequest;
import com.infosys.Water_Usage_Monitoring.dto.ApartmentResponse;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdRequest;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdResponse;
import com.infosys.Water_Usage_Monitoring.service.ApartmentService;
import com.infosys.Water_Usage_Monitoring.service.HouseholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/apartments")
@RequiredArgsConstructor
public class ApartmentController {
    private final ApartmentService service;
    private final HouseholdService householdService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApartmentResponse create(@Valid @RequestBody ApartmentRequest r) { return service.create(r); }

    @GetMapping
    public List<ApartmentResponse> all() { return service.getAll(); }

    @GetMapping("/{id}")
    public ApartmentResponse get(@PathVariable Long id) { return service.get(id); }

    @PutMapping("/{id}")
    public ApartmentResponse update(@PathVariable Long id, @Valid @RequestBody ApartmentRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    // households is apartment ke andar
    @PostMapping("/{apartmentId}/households")
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdResponse addHousehold(@PathVariable Long apartmentId,
                                          @Valid @RequestBody HouseholdRequest r) {
        return householdService.create(apartmentId, r);
    }

    @GetMapping("/{apartmentId}/households")
    public List<HouseholdResponse> households(@PathVariable Long apartmentId) {
        return householdService.listByApartment(apartmentId);
    }
}