package com.tuensso.scope;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientScopeRepository extends JpaRepository<ClientScope, UUID> {
    boolean existsByName(String name);
}
