package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.AuthResponse;
import com.infosys.Water_Usage_Monitoring.dto.LoginRequest;
import com.infosys.Water_Usage_Monitoring.dto.RegisterRequest;
import com.infosys.Water_Usage_Monitoring.dto.UserResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.DuplicateResourceException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;
    private final ApartmentService apartmentService;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public UserResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email))
            throw new DuplicateResourceException("Email already registered: " + email);

        Apartment apartment = apartmentService.find(r.apartmentId());

        Household household = null;
        if (r.householdId() != null) {
            household = householdRepository.findById(r.householdId())
                    .filter(h -> h.getApartment().getId().equals(r.apartmentId()))
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Household not found in this apartment: " + r.householdId()));
        }

        User u = new User();
        u.setName(r.name());
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(r.password()));
        u.setRole(Role.RESIDENT);            // public register = hamesha RESIDENT
        u.setApartment(apartment);
        u.setHousehold(household);
        return userService.toResponse(userRepository.save(u));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = userRepository.findByEmail(r.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));
        if (!passwordEncoder.matches(r.password(), u.getPasswordHash()))
            throw new BadCredentialsException("Invalid email or password");

        return new AuthResponse(jwtService.generateToken(u), "Bearer",
                u.getId(), u.getName(), u.getEmail(), u.getRole().name());
    }
}