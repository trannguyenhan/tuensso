package com.tuensso.user;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "user_attributes")
public class UserAttribute {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "attr_key", nullable = false)
    private String key;

    @Column(name = "attr_value")
    private String value;

    @PrePersist
    void prePersist() { if (this.id == null) this.id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
