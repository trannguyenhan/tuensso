package com.tuensso.scope;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "client_scopes")
public class ClientScope {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String name;

    private String description;

    @Column(name = "claim_name")
    private String claimName;

    @Column(name = "claim_value")
    private String claimValue;

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
    public String getClaimName() { return claimName; }
    public void setClaimName(String claimName) { this.claimName = claimName; }
    public String getClaimValue() { return claimValue; }
    public void setClaimValue(String claimValue) { this.claimValue = claimValue; }
    public Instant getCreatedAt() { return createdAt; }
}
