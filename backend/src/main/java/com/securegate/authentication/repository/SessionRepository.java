package com.securegate.authentication.repository;

import com.securegate.authentication.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SessionRepository extends JpaRepository<Session, UUID> {
    List<Session> findByUserIdAndRevokedFalse(UUID userId);
    List<Session> findByUserId(UUID userId);
}
