package com.securegate.administration.web;

import com.securegate.audit.domain.AuditEvent;
import com.securegate.audit.repository.AuditRepository;
import com.securegate.identity.domain.User;
import com.securegate.identity.dto.UserDto;
import com.securegate.identity.repository.UserRepository;
import com.securegate.tenant.domain.Organization;
import com.securegate.tenant.repository.OrganizationRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
@Tag(name = "Platform Administration", description = "System-level administrative operational controls")
public class AdminController {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditRepository auditRepository;

    public AdminController(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            AuditRepository auditRepository) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.auditRepository = auditRepository;
    }

    @GetMapping("/stats")
    @Operation(summary = "Get platform-wide operational statistics")
    public ResponseEntity<Map<String, Object>> getPlatformStats() {
        long totalUsers = userRepository.count();
        long totalOrganizations = organizationRepository.count();
        long totalAuditEvents = auditRepository.count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalOrganizations", totalOrganizations);
        stats.put("totalAuditEvents", totalAuditEvents);
        stats.put("systemStatus", "OPERATIONAL");
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/organizations")
    @Operation(summary = "List all organizations on the platform")
    public ResponseEntity<List<Organization>> listAllOrganizations() {
        return ResponseEntity.ok(organizationRepository.findAll());
    }

    @GetMapping("/users")
    @Operation(summary = "List all users on the platform")
    public ResponseEntity<List<UserDto>> listAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDto> dtos = users.stream().map(u -> new UserDto(
                u.getId(), u.getFullName(), u.getEmail(), u.isEmailVerified(), u.getStatus()
        )).toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Get platform-wide audit log stream")
    public ResponseEntity<Page<AuditEvent>> getPlatformAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "timestamp"));
        return ResponseEntity.ok(auditRepository.findAll(pageRequest));
    }
}
