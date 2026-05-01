package com.tuensso.group;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupAttributeRepository extends JpaRepository<GroupAttribute, UUID> {
    List<GroupAttribute> findByGroupId(UUID groupId);
    Optional<GroupAttribute> findByGroupIdAndKey(UUID groupId, String key);
    void deleteByGroupIdAndKey(UUID groupId, String key);
}
