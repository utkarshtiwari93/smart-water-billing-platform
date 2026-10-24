package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.TariffPlanRequest;
import com.infosys.Water_Usage_Monitoring.dto.TariffPlanResponse;
import com.infosys.Water_Usage_Monitoring.service.TariffService;
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
public class TariffController {

    private final TariffService service;

    @PostMapping("/apartments/{apartmentId}/tariffs")
    @ResponseStatus(HttpStatus.CREATED)
    public TariffPlanResponse create(@PathVariable Long apartmentId, @Valid @RequestBody TariffPlanRequest r) {
        return service.create(apartmentId, r);
    }

    @GetMapping("/apartments/{apartmentId}/tariffs")
    public List<TariffPlanResponse> list(@PathVariable Long apartmentId) {
        return service.list(apartmentId);
    }

    @GetMapping("/tariffs/{id}")
    public TariffPlanResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @PutMapping("/tariffs/{id}")
    public TariffPlanResponse update(@PathVariable Long id, @Valid @RequestBody TariffPlanRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/tariffs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}