package com.tuensso.group;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
@Table(name = "group_attributes")
public class GroupAttribute {

    @Id
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "attr_key", nullable = false)
    private String key;

    @Column(name = "attr_value")
    private String value;

    @PrePersist
    void prePersist() { if (this.id == null) this.id = UUID.randomUUID(); }

    public UUID getId() { return id; }
    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
