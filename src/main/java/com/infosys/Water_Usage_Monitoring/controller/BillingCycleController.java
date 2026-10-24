package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.BillingCycleRequest;
import com.infosys.Water_Usage_Monitoring.dto.BillingCycleResponse;
import com.infosys.Water_Usage_Monitoring.enums.CycleStatus;
import com.infosys.Water_Usage_Monitoring.service.BillingCycleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BillingCycleController {

    private final BillingCycleService service;

    @PostMapping("/apartments/{apartmentId}/billing-cycles")
    @ResponseStatus(HttpStatus.CREATED)
    public BillingCycleResponse create(@PathVariable Long apartmentId,
                                       @Valid @RequestBody BillingCycleRequest r) {
        return service.create(apartmentId, r);
    }

    @GetMapping("/apartments/{apartmentId}/billing-cycles")
    public List<BillingCycleResponse> list(@PathVariable Long apartmentId,
                                           @RequestParam(required = false) CycleStatus status) {
        return service.list(apartmentId, status);
    }

    @GetMapping("/billing-cycles/{id}")
    public BillingCycleResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/billing-cycles/{id}")
    public BillingCycleResponse update(@PathVariable Long id, @Valid @RequestBody BillingCycleRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/billing-cycles/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}