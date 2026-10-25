package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.AdjustmentRequest;
import com.infosys.Water_Usage_Monitoring.dto.AdjustmentResponse;
import com.infosys.Water_Usage_Monitoring.service.AdjustmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdjustmentController {

    private final AdjustmentService service;

    @PostMapping("/billing-cycles/{cycleId}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public AdjustmentResponse create(@PathVariable Long cycleId, @Valid @RequestBody AdjustmentRequest r,
                                     Authentication authentication) {
        return service.create(cycleId, r, authentication.getName());
    }

    @GetMapping("/billing-cycles/{cycleId}/adjustments")
    public List<AdjustmentResponse> list(@PathVariable Long cycleId) {
        return service.list(cycleId);
    }

    @DeleteMapping("/adjustments/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}