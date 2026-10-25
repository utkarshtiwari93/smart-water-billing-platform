package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.BillPreviewResponse;
import com.infosys.Water_Usage_Monitoring.service.BillingPreviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/billing-cycles/{cycleId}/preview")
@RequiredArgsConstructor
public class BillingPreviewController {

    private final BillingPreviewService service;

    @GetMapping
    public BillPreviewResponse preview(@PathVariable Long cycleId,
                                       @RequestParam(required = false) Long householdId,
                                       Authentication authentication) {
        return service.preview(cycleId, householdId, authentication.getName());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public List<BillPreviewResponse> previewAll(@PathVariable Long cycleId) {
        return service.previewAll(cycleId);
    }
}