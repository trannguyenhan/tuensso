package com.tuensso.admin;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import com.tuensso.group.UserGroup;
import com.tuensso.group.UserGroupRepository;
import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminConsoleService {

    private final UserAccountRepository userRepo;
    private final UserGroupRepository groupRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminConsoleService(UserAccountRepository userRepo,
                               UserGroupRepository groupRepo,
                               PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.groupRepo = groupRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UserRow> users() {
        return userRepo.findAllWithGroups().stream()
                .map(user -> new UserRow(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.isEnabled(),
                        user.getGroups().stream()
                    .map(group -> new GroupRef(group.getId(), group.getName()))
                    .sorted(Comparator.comparing(GroupRef::name))
                                .toList()))
                .sorted(Comparator.comparing(UserRow::username))
                .toList();
    }

    public List<GroupRow> groups() {
        return groupRepo.findAll().stream()
                .map(group -> new GroupRow(
                        group.getId(),
                        group.getName(),
                        group.getDescription(),
                        userRepo.countByGroups_Id(group.getId())))
                .sorted(Comparator.comparing(GroupRow::name))
                .toList();
    }

    public void createUser(String username, String email, String password) {
        String normalizedUsername = normalize(username);
        String normalizedEmail = normalize(email);

        if (normalizedUsername.isEmpty() || normalizedEmail.isEmpty() || password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username, email, password are required");
        }
        if (userRepo.existsByUsername(normalizedUsername)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepo.findByEmail(normalizedEmail).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }

        UserAccount user = new UserAccount();
        user.setUsername(normalizedUsername);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEnabled(true);
        userRepo.save(user);
    }

    @Transactional
    public void toggleUser(UUID userId) {
        UserAccount user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setEnabled(!user.isEnabled());
    }

    public void deleteUser(UUID userId) {
        if (!userRepo.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        userRepo.deleteById(userId);
    }

    @Transactional
    public void resetPassword(UUID userId, String newPassword) {
        if (newPassword == null || newPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "new password is required");
        }
        UserAccount user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
    }

    public void createGroup(String name, String description) {
        String normalizedName = normalize(name);
        if (normalizedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "group name is required");
        }
        if (groupRepo.existsByName(normalizedName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Group already exists");
        }

        UserGroup group = new UserGroup();
        group.setName(normalizedName);
        group.setDescription(description == null ? "" : description.trim());
        groupRepo.save(group);
    }

    public void deleteGroup(UUID groupId) {
        if (!groupRepo.existsById(groupId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found");
        }
        groupRepo.deleteById(groupId);
    }

    @Transactional
    public void addUserToGroup(UUID userId, UUID groupId) {
        UserAccount user = userRepo.findWithGroupsById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        UserGroup group = groupRepo.findById(groupId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Group not found"));
        user.getGroups().add(group);
    }

    @Transactional
    public void removeUserFromGroup(UUID userId, UUID groupId) {
        UserAccount user = userRepo.findWithGroupsById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        user.getGroups().removeIf(g -> g.getId().equals(groupId));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public record UserRow(UUID id, String username, String email, boolean enabled, List<GroupRef> groups) {
        public String groupsDisplay() {
            return groups.isEmpty() ? "-" : String.join(", ", groups.stream().map(GroupRef::name).toList());
        }
    }

    public record GroupRef(UUID id, String name) {
    }

    public record GroupRow(UUID id, String name, String description, long memberCount) {
        public String descriptionOrDash() {
            return description == null || description.isBlank() ? "-" : description;
        }
    }
}