package com.securegate.audit.repository;

import com.securegate.audit.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditRepository extends JpaRepository<AuditEvent, UUID> {

    @Query("SELECT a FROM AuditEvent a WHERE (:tenantId IS NULL OR a.tenantId = :tenantId) ORDER BY a.timestamp ASC")
    List<AuditEvent> findChainForTenant(@Param("tenantId") UUID tenantId);

    @Query("SELECT a FROM AuditEvent a WHERE (:tenantId IS NULL OR a.tenantId = :tenantId) ORDER BY a.timestamp DESC")
    Page<AuditEvent> findByTenantIdPaged(@Param("tenantId") UUID tenantId, Pageable pageable);

    @Query("SELECT a FROM AuditEvent a WHERE (:tenantId IS NULL AND a.tenantId IS NULL) OR (a.tenantId = :tenantId) ORDER BY a.timestamp DESC LIMIT 1")
    Optional<AuditEvent> findLatestEventForTenant(@Param("tenantId") UUID tenantId);
}
