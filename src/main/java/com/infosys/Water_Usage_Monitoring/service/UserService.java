package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.ChangePasswordRequest;
import com.infosys.Water_Usage_Monitoring.dto.UpdateProfileRequest;
import com.infosys.Water_Usage_Monitoring.dto.UserResponse;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final HouseholdRepository householdRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        return toResponse(findByEmail(email));
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest r) {
        User u = findByEmail(email);
        u.setName(r.name().trim());          // dirty checking se UPDATE khud chalega
        return toResponse(u);
    }

    @Transactional
    public void changePassword(String email, ChangePasswordRequest r) {
        User u = findByEmail(email);
        if (u.getPasswordHash() == null)
            throw new BadRequestException("This account signs in with Google and has no password to change");
        if (!passwordEncoder.matches(r.currentPassword(), u.getPasswordHash()))
            throw new BadRequestException("Current password is incorrect");
        if (r.currentPassword().equals(r.newPassword()))
            throw new BadRequestException("New password must be different from the current one");
        u.setPasswordHash(passwordEncoder.encode(r.newPassword()));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listUsers(Long apartmentId) {
        List<User> users = (apartmentId == null)
                ? userRepository.findAll()
                : userRepository.findByApartmentId(apartmentId);
        return users.stream().map(this::toResponse).toList();
    }

    @Transactional
    public UserResponse assignHousehold(Long userId, Long householdId) {
        User u = findById(userId);
        if (u.getRole() != Role.RESIDENT)
            throw new BadRequestException("Only residents can be assigned to a household");

        Household h = householdRepository.findById(householdId)
                .orElseThrow(() -> new ResourceNotFoundException("Household not found: " + householdId));
        if (!h.getApartment().getId().equals(u.getApartment().getId()))
            throw new BadRequestException("Household belongs to a different apartment");

        u.setHousehold(h);
        return toResponse(u);
    }

    @Transactional
    public UserResponse unassignHousehold(Long userId) {
        User u = findById(userId);
        u.setHousehold(null);
        return toResponse(u);
    }

    public UserResponse toResponse(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole().name(),
                u.getApartment().getId(),
                u.getHousehold() == null ? null : u.getHousehold().getId());
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    private User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }
}