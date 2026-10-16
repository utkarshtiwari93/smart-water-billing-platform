package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.ChangePasswordRequest;
import com.infosys.Water_Usage_Monitoring.dto.UserResponse;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.BadRequestException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock HouseholdRepository householdRepository;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks UserService service;

    private Apartment apartment(long id) {
        Apartment a = new Apartment();
        a.setId(id);
        return a;
    }

    private User user(long id, Role role, long apartmentId) {
        User u = new User();
        u.setId(id);
        u.setName("Test");
        u.setEmail("t@test.com");
        u.setPasswordHash("HASH");
        u.setRole(role);
        u.setApartment(apartment(apartmentId));
        return u;
    }

    private Household household(long id, long apartmentId) {
        Household h = new Household();
        h.setId(id);
        h.setApartment(apartment(apartmentId));
        return h;
    }

    @Test
    void assignHousehold_toAdmin_isRejected() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, Role.ADMIN, 1L)));

        assertThrows(BadRequestException.class, () -> service.assignHousehold(1L, 5L));
    }

    @Test
    void assignHousehold_fromDifferentApartment_isRejected() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.RESIDENT, 1L)));
        when(householdRepository.findById(5L)).thenReturn(Optional.of(household(5L, 2L)));

        assertThrows(BadRequestException.class, () -> service.assignHousehold(2L, 5L));
    }

    @Test
    void assignHousehold_success_setsHousehold() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, Role.RESIDENT, 1L)));
        when(householdRepository.findById(5L)).thenReturn(Optional.of(household(5L, 1L)));

        UserResponse r = service.assignHousehold(2L, 5L);

        assertEquals(5L, r.householdId());
    }

    @Test
    void changePassword_wrongCurrent_isRejected() {
        when(userRepository.findByEmail("t@test.com")).thenReturn(Optional.of(user(2L, Role.RESIDENT, 1L)));
        when(passwordEncoder.matches("bad", "HASH")).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> service.changePassword("t@test.com", new ChangePasswordRequest("bad", "New@12345")));
    }

    @Test
    void changePassword_sameAsCurrent_isRejected() {
        when(userRepository.findByEmail("t@test.com")).thenReturn(Optional.of(user(2L, Role.RESIDENT, 1L)));
        when(passwordEncoder.matches("Same@1234", "HASH")).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> service.changePassword("t@test.com", new ChangePasswordRequest("Same@1234", "Same@1234")));
    }

    @Test
    void changePassword_success_storesNewHash() {
        User u = user(2L, Role.RESIDENT, 1L);
        when(userRepository.findByEmail("t@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("Old@12345", "HASH")).thenReturn(true);
        when(passwordEncoder.encode("New@12345")).thenReturn("NEWHASH");

        service.changePassword("t@test.com", new ChangePasswordRequest("Old@12345", "New@12345"));

        assertEquals("NEWHASH", u.getPasswordHash());
    }
}