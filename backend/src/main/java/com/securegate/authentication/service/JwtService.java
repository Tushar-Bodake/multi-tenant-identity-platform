package com.securegate.authentication.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.securegate.common.error.UnauthorizedException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    @Value("${securegate.jwt.issuer:https://auth.securegate.local}")
    private String issuer;

    @Value("${securegate.jwt.audience:securegate-api}")
    private String audience;

    @Value("${securegate.jwt.access-token-expiration-seconds:900}")
    private long accessTokenExpirationSeconds;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private String keyId;

    @PostConstruct
    public void init() {
        try {
            // Generate in-memory RSA 2048-bit keypair on startup for dev/test
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            this.privateKey = (RSAPrivateKey) kp.getPrivate();
            this.publicKey = (RSAPublicKey) kp.getPublic();
            this.keyId = "securegate-key-" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Initialized RSA signing key with kid='{}'", keyId);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to initialize RSA key generator", e);
        }
    }

    public String generateAccessToken(UUID userId, String email, String fullName, UUID tenantId, String role, Set<String> permissions) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpirationSeconds);

        JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder()
                .subject(userId.toString())
                .issuer(issuer)
                .audience(audience)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiry))
                .jwtID(UUID.randomUUID().toString())
                .claim("email", email)
                .claim("name", fullName);

        if (tenantId != null) {
            builder.claim("tenant_id", tenantId.toString());
        }
        if (role != null) {
            builder.claim("role", role);
        }
        if (permissions != null && !permissions.isEmpty()) {
            builder.claim("permissions", new ArrayList<>(permissions));
        }

        JWTClaimsSet claimsSet = builder.build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(keyId)
                .type(JOSEObjectType.JWT)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);

        try {
            JWSSigner signer = new RSASSASigner(privateKey);
            signedJWT.sign(signer);
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to sign JWT access token", e);
        }
    }

    public JWTClaimsSet validateAndParseToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            // Verify algorithm is strictly RS256
            if (!JWSAlgorithm.RS256.equals(signedJWT.getHeader().getAlgorithm())) {
                throw new UnauthorizedException("Invalid token algorithm: " + signedJWT.getHeader().getAlgorithm());
            }

            // Verify signature
            JWSVerifier verifier = new RSASSAVerifier(publicKey);
            if (!signedJWT.verify(verifier)) {
                throw new UnauthorizedException("Invalid token signature");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Validate expiration
            Date expiration = claims.getExpirationTime();
            if (expiration == null || new Date().after(expiration)) {
                throw new UnauthorizedException("Token has expired");
            }

            // Validate issuer
            if (!issuer.equals(claims.getIssuer())) {
                throw new UnauthorizedException("Token issuer mismatch");
            }

            // Validate audience
            if (!claims.getAudience().contains(audience)) {
                throw new UnauthorizedException("Token audience mismatch");
            }

            return claims;
        } catch (ParseException | JOSEException e) {
            throw new UnauthorizedException("Malformed or invalid token: " + e.getMessage());
        }
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }

    public RSAPublicKey getPublicKey() {
        return publicKey;
    }

    public String getKeyId() {
        return keyId;
    }
}
