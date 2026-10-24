package com.securegate.identity.repository;

import com.securegate.identity.domain.UserEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserEmailRepository extends JpaRepository<UserEmail, UUID> {
    List<UserEmail> findByUserId(UUID userId);
    Optional<UserEmail> findByEmail(String email);
    boolean existsByEmail(String email);
}
