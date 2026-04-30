package com.tuensso.user;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAttributeRepository extends JpaRepository<UserAttribute, UUID> {
    List<UserAttribute> findByUserId(UUID userId);
    Optional<UserAttribute> findByUserIdAndKey(UUID userId, String key);
    void deleteByUserIdAndKey(UUID userId, String key);
}
