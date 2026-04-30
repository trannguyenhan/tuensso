package com.tuensso.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/me")
public class UserProfileApiController {

    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public UserProfileApiController(UserAccountRepository userRepo, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public ProfileResponse me(Authentication authentication) {
        UserAccount user = currentUser(authentication);
        UserAccount withGroups = userRepo.findWithGroupsById(user.getId()).orElse(user);
        List<String> groups = withGroups.getGroups().stream().map(g -> g.getName()).sorted().toList();
        return new ProfileResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.isEnabled(), groups, user.getCreatedAt());
    }

    @PutMapping
    public ProfileResponse updateProfile(Authentication authentication, @RequestBody UpdateProfileRequest req) {
        UserAccount user = currentUser(authentication);
        if (req.email() != null && !req.email().isBlank()) {
            String trimmed = req.email().trim();
            userRepo.findByEmail(trimmed).ifPresent(existing -> {
                if (!existing.getId().equals(user.getId())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
                }
            });
            user.setEmail(trimmed);
        }
        userRepo.save(user);
        UserAccount withGroups = userRepo.findWithGroupsById(user.getId()).orElse(user);
        List<String> groups = withGroups.getGroups().stream().map(g -> g.getName()).sorted().toList();
        return new ProfileResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.isEnabled(), groups, user.getCreatedAt());
    }

    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(Authentication authentication, @RequestBody ChangePasswordRequest req) {
        UserAccount user = currentUser(authentication);
        if (req.newPassword() == null || req.newPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        user.setPasswordChangedAt(java.time.Instant.now());
        userRepo.save(user);
        return ResponseEntity.noContent().build();
    }

    private UserAccount currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        String username = authentication.getName();
        return userRepo.findByUsername(username)
                .or(() -> userRepo.findByEmail(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public record ProfileResponse(UUID id, String username, String email,
                                   boolean enabled, List<String> groups, Instant createdAt) {}
    public record UpdateProfileRequest(String email) {}
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}
}
