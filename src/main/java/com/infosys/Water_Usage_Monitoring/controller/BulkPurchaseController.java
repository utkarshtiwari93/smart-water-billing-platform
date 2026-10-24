package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.BulkPurchaseRequest;
import com.infosys.Water_Usage_Monitoring.dto.BulkPurchaseResponse;
import com.infosys.Water_Usage_Monitoring.dto.PurchaseSummaryResponse;
import com.infosys.Water_Usage_Monitoring.service.BulkPurchaseService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class BulkPurchaseController {

    private final BulkPurchaseService service;

    @PostMapping("/billing-cycles/{cycleId}/purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public BulkPurchaseResponse create(@PathVariable Long cycleId, @Valid @RequestBody BulkPurchaseRequest r) {
        return service.create(cycleId, r);
    }

    @GetMapping("/billing-cycles/{cycleId}/purchases")
    public List<BulkPurchaseResponse> list(@PathVariable Long cycleId) {
        return service.list(cycleId);
    }

    @GetMapping("/billing-cycles/{cycleId}/purchases/summary")
    public PurchaseSummaryResponse summary(@PathVariable Long cycleId) {
        return service.summary(cycleId);
    }

    @GetMapping("/purchases/{id}")
    public BulkPurchaseResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/purchases/{id}")
    public BulkPurchaseResponse update(@PathVariable Long id, @Valid @RequestBody BulkPurchaseRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/purchases/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}