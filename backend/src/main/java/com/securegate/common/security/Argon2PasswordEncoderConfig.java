package com.securegate.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class Argon2PasswordEncoderConfig {

    public static final int SALT_LENGTH = 16;       // 128 bits
    public static final int HASH_LENGTH = 32;       // 256 bits
    public static final int PARALLELISM = 1;       // 1 thread
    public static final int MEMORY = 65536;         // 64 MiB
    public static final int ITERATIONS = 3;         // 3 passes
    public static final int MAX_PASSWORD_LENGTH = 128; // Reject excessively long inputs to prevent DoS

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(
                SALT_LENGTH,
                HASH_LENGTH,
                PARALLELISM,
                MEMORY,
                ITERATIONS
        );
    }
}
