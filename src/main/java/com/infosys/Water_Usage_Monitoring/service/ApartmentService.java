package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.ApartmentRequest;
import com.infosys.Water_Usage_Monitoring.dto.ApartmentResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.ApartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ApartmentService {
    private final ApartmentRepository repo;

    public ApartmentResponse create(ApartmentRequest r) {
        Apartment a = new Apartment();
        apply(a, r);
        return toDto(repo.save(a));
    }

    @Transactional(readOnly = true)
    public List<ApartmentResponse> getAll() {
        return repo.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public ApartmentResponse get(Long id) { return toDto(find(id)); }

    @Transactional
    public ApartmentResponse update(Long id, ApartmentRequest r) {
        Apartment a = find(id);
        apply(a, r);
        return toDto(a);
    }

    public void delete(Long id) { repo.delete(find(id)); }

    public Apartment find(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Apartment not found: " + id));
    }

    private void apply(Apartment a, ApartmentRequest r) {
        a.setName(r.name()); a.setAddress(r.address()); a.setCity(r.city());
    }

    private ApartmentResponse toDto(Apartment a) {
        return new ApartmentResponse(a.getId(), a.getName(), a.getAddress(), a.getCity());
    }
}