package com.securegate.authentication.repository;

import com.securegate.authentication.domain.MagicLinkToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MagicLinkTokenRepository extends JpaRepository<MagicLinkToken, UUID> {
    Optional<MagicLinkToken> findByTokenHash(String tokenHash);
}
