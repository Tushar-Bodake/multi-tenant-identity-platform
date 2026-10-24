package com.securegate.common.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket createBucketForPath(String path) {
        if (path.contains("/auth/login")) {
            // 10 requests per minute
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(10, Refill.greedy(10, Duration.ofMinutes(1))))
                    .build();
        } else if (path.contains("/auth/register")) {
            // 5 requests per minute
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                    .build();
        } else if (path.contains("/magic-link/request") || path.contains("/forgot-password")) {
            // 3 requests per minute
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(1))))
                    .build();
        }
        // General API rate limit: 100 requests per minute
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.greedy(100, Duration.ofMinutes(1))))
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        // Only rate-limit mutative authentication endpoints
        if (path.startsWith("/api/v1/auth/") && "POST".equalsIgnoreCase(request.getMethod())) {
            String clientIp = getClientIp(request);
            String bucketKey = clientIp + ":" + path;

            Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucketForPath(path));

            if (!bucket.tryConsume(1)) {
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                response.setHeader("Retry-After", "60");
                response.getWriter().write("""
                    {
                      "type": "https://securegate.local/errors/429",
                      "title": "Too Many Requests",
                      "status": 429,
                      "detail": "Rate limit exceeded. Please wait a moment before trying again."
                    }
                    """);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
