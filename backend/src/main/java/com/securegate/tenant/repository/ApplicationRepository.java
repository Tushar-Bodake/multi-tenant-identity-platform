package com.securegate.tenant.repository;

import com.securegate.tenant.domain.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    Optional<Application> findByClientId(String clientId);
    List<Application> findByOrganizationId(UUID organizationId);
}
