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

    private final RoleRepository roleRepo;
    private final UserAccountRepository userRepo;

    public RoleAdminController(RoleRepository roleRepo, UserAccountRepository userRepo) {
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
    }

    @GetMapping
    public List<Role> list() { return roleRepo.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Role create(@RequestBody CreateRoleRequest req) {
        if (roleRepo.existsByName(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Role already exists");
        }
        Role role = new Role();
        role.setName(req.name());
        role.setDescription(req.description());
        return roleRepo.save(role);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        if (!roleRepo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
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

    public record CreateRoleRequest(String name, String description) {}
}
