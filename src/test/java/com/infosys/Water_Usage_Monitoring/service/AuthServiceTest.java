package com.infosys.Water_Usage_Monitoring.service;

import com.infosys.Water_Usage_Monitoring.dto.AuthResponse;
import com.infosys.Water_Usage_Monitoring.dto.LoginRequest;
import com.infosys.Water_Usage_Monitoring.dto.RegisterRequest;
import com.infosys.Water_Usage_Monitoring.entity.Apartment;
import com.infosys.Water_Usage_Monitoring.entity.Household;
import com.infosys.Water_Usage_Monitoring.entity.User;
import com.infosys.Water_Usage_Monitoring.enums.Role;
import com.infosys.Water_Usage_Monitoring.exception.DuplicateResourceException;
import com.infosys.Water_Usage_Monitoring.exception.ResourceNotFoundException;
import com.infosys.Water_Usage_Monitoring.repository.HouseholdRepository;
import com.infosys.Water_Usage_Monitoring.repository.UserRepository;
import com.infosys.Water_Usage_Monitoring.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepo;
    @Mock HouseholdRepository householdRepo;
    @Mock ApartmentService apartmentService;
    @Mock UserService userService;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @InjectMocks AuthService authService;

    private Apartment apartment(long id) {
        Apartment a = new Apartment();
        a.setId(id);
        return a;
    }

    @Test
    void register_createsResidentWithHashedPasswordAndLowercaseEmail() {
        when(userRepo.existsByEmail("ravi@test.com")).thenReturn(false);
        when(apartmentService.find(1L)).thenReturn(apartment(1L));
        when(passwordEncoder.encode("Ravi@1234")).thenReturn("HASHED");
        when(userRepo.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        authService.register(new RegisterRequest("Ravi", "Ravi@Test.com", "Ravi@1234", 1L, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        User saved = captor.getValue();
        assertEquals(Role.RESIDENT, saved.getRole());
        assertEquals("HASHED", saved.getPasswordHash());
        assertEquals("ravi@test.com", saved.getEmail());
    }

    @Test
    void register_duplicateEmail_isRejected() {
        when(userRepo.existsByEmail("ravi@test.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class,
                () -> authService.register(new RegisterRequest("Ravi", "ravi@test.com", "Ravi@1234", 1L, null)));
        verify(userRepo, never()).save(any(User.class));
    }

    @Test
    void register_householdFromAnotherApartment_isRejected() {
        Household other = new Household();
        other.setId(5L);
        other.setApartment(apartment(2L));
        when(userRepo.existsByEmail("ravi@test.com")).thenReturn(false);
        when(apartmentService.find(1L)).thenReturn(apartment(1L));
        when(householdRepo.findById(5L)).thenReturn(Optional.of(other));

        assertThrows(ResourceNotFoundException.class,
                () -> authService.register(new RegisterRequest("Ravi", "ravi@test.com", "Ravi@1234", 1L, 5L)));
    }

    private User storedUser() {
        User u = new User();
        u.setId(7L);
        u.setName("Ravi");
        u.setEmail("ravi@test.com");
        u.setPasswordHash("HASH");
        u.setRole(Role.RESIDENT);
        return u;
    }

    @Test
    void login_success_returnsTokenAndRole() {
        User u = storedUser();
        when(userRepo.findByEmail("ravi@test.com")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("Ravi@1234", "HASH")).thenReturn(true);
        when(jwtService.generateToken(u)).thenReturn("TOKEN");

        AuthResponse r = authService.login(new LoginRequest("ravi@test.com", "Ravi@1234"));

        assertEquals("TOKEN", r.token());
        assertEquals("Bearer", r.tokenType());
        assertEquals("RESIDENT", r.role());
    }

    @Test
    void login_wrongPassword_isRejected() {
        when(userRepo.findByEmail("ravi@test.com")).thenReturn(Optional.of(storedUser()));
        when(passwordEncoder.matches("bad", "HASH")).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("ravi@test.com", "bad")));
    }

    @Test
    void login_unknownEmail_isRejected() {
        when(userRepo.findByEmail("none@test.com")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> authService.login(new LoginRequest("none@test.com", "whatever")));
    }
}