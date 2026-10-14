package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);      // login ke liye
    boolean existsByEmail(String email);           // register pe duplicate check
}