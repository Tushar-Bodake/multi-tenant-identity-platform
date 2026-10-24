package com.securegate.tenant.domain;

import com.securegate.common.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "organizations")
public class Organization extends BaseEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "ACTIVE";

    @Column(name = "enforce_mfa", nullable = false)
    private boolean enforceMfa = false;

    @Column(name = "allowed_domains")
    private String allowedDomains;

    public Organization() {
    }

    public Organization(String name, String slug) {
        this.name = name;
        this.slug = slug;
        this.status = "ACTIVE";
        this.enforceMfa = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isEnforceMfa() {
        return enforceMfa;
    }

    public void setEnforceMfa(boolean enforceMfa) {
        this.enforceMfa = enforceMfa;
    }

    public String getAllowedDomains() {
        return allowedDomains;
    }

    public void setAllowedDomains(String allowedDomains) {
        this.allowedDomains = allowedDomains;
    }
}
