package com.tuensso.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    @EntityGraph(attributePaths = "groups")
    @Query("select u from UserAccount u")
    java.util.List<UserAccount> findAllWithGroups();

    @EntityGraph(attributePaths = {"groups", "roles"})
    @Query("select u from UserAccount u")
    java.util.List<UserAccount> findAllWithGroupsAndRoles();

    @EntityGraph(attributePaths = "groups")
    @Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> findWithGroupsById(@Param("id") UUID id);

    @EntityGraph(attributePaths = {"groups", "roles"})
    @Query("select u from UserAccount u where u.id = :id")
    Optional<UserAccount> findWithGroupsAndRolesById(@Param("id") UUID id);

    Optional<UserAccount> findByUsername(String username);

    Optional<UserAccount> findByEmail(String email);

    boolean existsByUsername(String username);

    long countByGroups_Id(UUID groupId);
}
