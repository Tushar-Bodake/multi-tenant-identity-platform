package com.securegate.tenant.repository;

import com.securegate.tenant.domain.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, UUID> {
    Optional<Invitation> findByTokenHash(String tokenHash);
    List<Invitation> findByOrganizationId(UUID organizationId);
    List<Invitation> findByEmail(String email);
    boolean existsByOrganizationIdAndEmailAndAcceptedAtIsNull(UUID organizationId, String email);
}
