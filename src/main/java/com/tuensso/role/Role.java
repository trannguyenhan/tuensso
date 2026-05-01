package com.tuensso.role;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    public static final List<String> ALL_PERMISSIONS = List.of(
            "dashboard", "apps", "users", "groups", "roles", "sessions", "audit", "integration");

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "permissions")
    private String permissions;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) this.id = UUID.randomUUID();
        if (this.createdAt == null) this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getCreatedAt() { return createdAt; }
    public String getPermissions() { return permissions; }
    public void setPermissions(String permissions) { this.permissions = permissions; }

    public List<String> getPermissionList() {
        if (permissions == null || permissions.isBlank()) return Collections.emptyList();
        return Arrays.stream(permissions.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
