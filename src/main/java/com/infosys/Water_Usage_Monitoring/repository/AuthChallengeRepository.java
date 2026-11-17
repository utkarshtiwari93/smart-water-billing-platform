package com.infosys.Water_Usage_Monitoring.repository;

import com.infosys.Water_Usage_Monitoring.entity.AuthChallenge;
import com.infosys.Water_Usage_Monitoring.entity.AuthChallenge.Purpose;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthChallengeRepository extends JpaRepository<AuthChallenge, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select challenge from AuthChallenge challenge where challenge.email = :email and challenge.purpose = :purpose")
    Optional<AuthChallenge> findForUpdate(@Param("email") String email, @Param("purpose") Purpose purpose);
}
