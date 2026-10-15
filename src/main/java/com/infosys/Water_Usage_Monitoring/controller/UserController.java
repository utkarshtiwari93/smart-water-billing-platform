package com.infosys.Water_Usage_Monitoring.controller;

import com.infosys.Water_Usage_Monitoring.dto.AssignHouseholdRequest;
import com.infosys.Water_Usage_Monitoring.dto.ChangePasswordRequest;
import com.infosys.Water_Usage_Monitoring.dto.UpdateProfileRequest;
import com.infosys.Water_Usage_Monitoring.dto.UserResponse;
import com.infosys.Water_Usage_Monitoring.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return userService.getProfile(authentication.getName());
    }

    @PutMapping("/me")
    public UserResponse updateMe(@Valid @RequestBody UpdateProfileRequest r, Authentication authentication) {
        return userService.updateProfile(authentication.getName(), r);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody ChangePasswordRequest r, Authentication authentication) {
        userService.changePassword(authentication.getName(), r);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> list(@RequestParam(required = false) Long apartmentId) {
        return userService.listUsers(apartmentId);
    }

    @PutMapping("/{userId}/household")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse assign(@PathVariable Long userId, @Valid @RequestBody AssignHouseholdRequest r) {
        return userService.assignHousehold(userId, r.householdId());
    }

    @DeleteMapping("/{userId}/household")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse unassign(@PathVariable Long userId) {
        return userService.unassignHousehold(userId);
    }
}