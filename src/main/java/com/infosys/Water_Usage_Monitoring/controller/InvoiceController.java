package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.FinalizeResponse;
import com.infosys.Water_Usage_Monitoring.dto.InvoiceResponse;
import com.infosys.Water_Usage_Monitoring.service.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService service;

    @PostMapping("/billing-cycles/{cycleId}/finalize")
    @PreAuthorize("hasRole('ADMIN')")
    public FinalizeResponse finalizeCycle(@PathVariable Long cycleId) {
        return service.finalizeCycle(cycleId);
    }

    @PostMapping("/billing-cycles/{cycleId}/archive")
    @PreAuthorize("hasRole('ADMIN')")
    public FinalizeResponse archive(@PathVariable Long cycleId) {
        return service.archive(cycleId);
    }

    @GetMapping("/billing-cycles/{cycleId}/invoices")
    @PreAuthorize("hasRole('ADMIN')")
    public List<InvoiceResponse> listForCycle(@PathVariable Long cycleId) {
        return service.listForCycle(cycleId);
    }

    @GetMapping("/invoices/my")
    public List<InvoiceResponse> myInvoices(Authentication authentication) {
        return service.myInvoices(authentication.getName());
    }

    @GetMapping("/invoices/{id}")
    public InvoiceResponse get(@PathVariable Long id, Authentication authentication) {
        return service.get(id, authentication.getName());
    }
}