package com.tuensso.admin;

import java.util.List;
import java.util.UUID;

import com.tuensso.role.Role;
import com.tuensso.role.RoleRepository;
import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/roles")
public class RoleAdminController {

    private static final String ADMIN_ROLE_NAME = "ADMIN";

    private final RoleRepository roleRepo;
    private final UserAccountRepository userRepo;

    public RoleAdminController(RoleRepository roleRepo, UserAccountRepository userRepo) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<Role> list() { return roleRepo.findAll(); }

    @GetMapping("/{id}")
    public Role get(@PathVariable UUID id) {
        return roleRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/{roleId}/users")
    public List<RoleMember> roleMembers(@PathVariable UUID roleId) {
        roleRepo.findById(roleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return userRepo.findAll().stream()
                .filter(u -> {
                    var full = userRepo.findWithGroupsAndRolesById(u.getId()).orElse(u);
                    return full.getRoles().stream().anyMatch(r -> r.getId().equals(roleId));
                })
                .map(u -> new RoleMember(u.getId().toString(), u.getUsername(), u.getEmail(), u.isEnabled()))
                .toList();
    }

    public record RoleMember(String id, String username, String email, boolean enabled) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Role create(@RequestBody CreateRoleRequest req) {
        if (ADMIN_ROLE_NAME.equalsIgnoreCase(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot create reserved role ADMIN");
        }
        if (roleRepo.existsByName(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role already exists");
        }
        Role role = new Role();
        role.setName(req.name());
        role.setDescription(req.description());
        role.setPermissions(req.permissions());
        return roleRepo.save(role);
    }

    @PutMapping("/{id}")
    public Role update(@PathVariable UUID id, @RequestBody UpdateRoleRequest req) {
        Role role = roleRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ADMIN_ROLE_NAME.equalsIgnoreCase(role.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot modify the built-in ADMIN role");
        }
        if (req.description() != null) role.setDescription(req.description());
        if (req.permissions() != null) role.setPermissions(req.permissions());
        return roleRepo.save(role);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        Role role = roleRepo.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (ADMIN_ROLE_NAME.equalsIgnoreCase(role.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot delete the built-in ADMIN role");
        }
        roleRepo.deleteById(id);
    }

    @PostMapping("/{roleId}/users/{userId}")
    public void assignRole(@PathVariable UUID roleId, @PathVariable UUID userId) {
        Role role = roleRepo.findById(roleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        UserAccount user = userRepo.findWithGroupsAndRolesById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.getRoles().add(role);
        userRepo.save(user);
    }

    @DeleteMapping("/{roleId}/users/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRole(@PathVariable UUID roleId, @PathVariable UUID userId) {
        Role role = roleRepo.findById(roleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        UserAccount user = userRepo.findWithGroupsAndRolesById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.getRoles().remove(role);
        userRepo.save(user);
    }

    @GetMapping("/user/{userId}")
    public List<Role> userRoles(@PathVariable UUID userId) {
        UserAccount user = userRepo.findWithGroupsAndRolesById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return user.getRoles().stream().sorted((a, b) -> a.getName().compareTo(b.getName())).toList();
    }

    public record CreateRoleRequest(String name, String description, String permissions) {}
    public record UpdateRoleRequest(String description, String permissions) {}
}
