package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.HouseholdRequest;
import com.infosys.Water_Usage_Monitoring.dto.HouseholdResponse;
import com.infosys.Water_Usage_Monitoring.service.HouseholdService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1/households")
@RequiredArgsConstructor
public class HouseholdController {
    private final HouseholdService service;

    @GetMapping("/{id}")
    public HouseholdResponse get(@PathVariable Long id) { return service.get(id); }

    @PutMapping("/{id}")
    public HouseholdResponse update(@PathVariable Long id, @Valid @RequestBody HouseholdRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }
}