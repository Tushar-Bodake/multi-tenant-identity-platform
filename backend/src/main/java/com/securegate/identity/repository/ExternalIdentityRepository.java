package com.securegate.identity.repository;

import com.securegate.identity.domain.ExternalIdentity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, UUID> {
    Optional<ExternalIdentity> findByProviderAndProviderSubject(String provider, String providerSubject);
    List<ExternalIdentity> findByUserId(UUID userId);
}
