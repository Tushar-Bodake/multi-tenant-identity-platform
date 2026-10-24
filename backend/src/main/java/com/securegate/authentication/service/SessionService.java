package com.securegate.authentication.service;

import com.securegate.authentication.domain.Session;
import com.securegate.authentication.repository.SessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SessionService {

    private final SessionRepository sessionRepository;

    public SessionService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    @Transactional
    public Session createSession(UUID userId, String deviceInfo, String ipAddress, long durationSeconds) {
        Instant expiresAt = Instant.now().plusSeconds(durationSeconds);
        Session session = new Session(userId, deviceInfo, ipAddress, expiresAt);
        return sessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public Optional<Session> getSession(UUID sessionId) {
        return sessionRepository.findById(sessionId);
    }

    @Transactional
    public void touchSession(UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(Session::touch);
    }

    @Transactional
    public void revokeSession(UUID sessionId) {
        sessionRepository.findById(sessionId).ifPresent(Session::revoke);
    }

    @Transactional
    public void revokeAllUserSessions(UUID userId) {
        List<Session> sessions = sessionRepository.findByUserIdAndRevokedFalse(userId);
        sessions.forEach(Session::revoke);
    }

    @Transactional(readOnly = true)
    public List<Session> getUserActiveSessions(UUID userId) {
        return sessionRepository.findByUserIdAndRevokedFalse(userId);
    }
}
