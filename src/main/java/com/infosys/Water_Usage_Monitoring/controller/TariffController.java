package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.TariffPlanRequest;
import com.infosys.Water_Usage_Monitoring.dto.TariffPlanResponse;
import com.infosys.Water_Usage_Monitoring.service.TariffService;
import com.infosys.Water_Usage_Monitoring.security.AdminApartmentScope;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
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
    private final AdminApartmentScope apartmentScope;

    @PostMapping("/apartments/{apartmentId}/tariffs")
    @ResponseStatus(HttpStatus.CREATED)
    public TariffPlanResponse create(@PathVariable Long apartmentId, @Valid @RequestBody TariffPlanRequest r,
                                     Authentication authentication) {
        apartmentScope.apartment(apartmentId, authentication.getName());
        return service.create(apartmentId, r);
    }

    @GetMapping("/apartments/{apartmentId}/tariffs")
    public List<TariffPlanResponse> list(@PathVariable Long apartmentId, Authentication authentication) {
        apartmentScope.apartment(apartmentId, authentication.getName());
        return service.list(apartmentId);
    }

    @GetMapping("/tariffs/{id}")
    public TariffPlanResponse get(@PathVariable Long id, Authentication authentication) {
        apartmentScope.tariff(id, authentication.getName());
        return service.get(id);
    }

    @PutMapping("/tariffs/{id}")
    public TariffPlanResponse update(@PathVariable Long id, @Valid @RequestBody TariffPlanRequest r,
                                     Authentication authentication) {
        apartmentScope.tariff(id, authentication.getName());
        return service.update(id, r);
    }

    @PatchMapping("/tariffs/{id}/activation")
    public TariffPlanResponse setActive(@PathVariable Long id,
                                        @RequestParam boolean active,
                                        Authentication authentication) {
        apartmentScope.tariff(id, authentication.getName());
        return service.setActive(id, active);
    }

    @DeleteMapping("/tariffs/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, Authentication authentication) {
        apartmentScope.tariff(id, authentication.getName());
        service.delete(id);
    }
}