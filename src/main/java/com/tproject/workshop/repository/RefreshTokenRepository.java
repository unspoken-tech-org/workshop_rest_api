package com.tproject.workshop.repository;

import com.tproject.workshop.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true " +
           "WHERE rt.apiKey.clientName = :clientName AND rt.deviceId = :deviceId AND rt.revoked = false")
    void revokeByClientNameAndDeviceId(String clientName, String deviceId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.apiKey.clientName = :clientName")
    void revokeAllByClientName(String clientName);
}