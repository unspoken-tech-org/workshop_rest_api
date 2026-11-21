package com.tproject.workshop.repository;

import com.tproject.workshop.model.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query(
            "UPDATE RefreshToken rt SET rt.revoked = true "
                    + "WHERE rt.id = :id AND rt.revoked = false")
    int revokeIfActive(Long id);

    @Modifying
    @Query(
            "UPDATE RefreshToken rt SET rt.revoked = true "
                    + "WHERE rt.apiKey.id = :apiKeyId AND rt.revoked = false")
    void revokeByApiKeyId(Long apiKeyId);

    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.apiKey.clientName = :clientName")
    void revokeAllByClientName(String clientName);
}
