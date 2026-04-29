package com.tuensso.admin;

import java.util.List;
import java.util.UUID;

import com.tuensso.audit.AuditService;
import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/users")
public class UserAdminController {

    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserAdminController(UserAccountRepository userRepo, PasswordEncoder passwordEncoder, AuditService audit) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
    }

    @GetMapping
    public List<UserAccount> list() {
        return userRepo.findAll();
    }

    @GetMapping("/{id}")
    public UserAccount get(@PathVariable UUID id) {
        return userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAccount create(@RequestBody CreateUserRequest req, Authentication auth) {
        if (req.password() == null || req.password().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        if (userRepo.existsByUsername(req.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }
        if (userRepo.findByEmail(req.email()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists");
        }
        UserAccount user = new UserAccount();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPasswordHash(passwordEncoder.encode(req.password()));
        user.setEnabled(true);
        UserAccount saved = userRepo.save(user);
        audit.log("USER_CREATED", auth.getName(), "user", req.username(), null, null);
        return saved;
    }

    @PutMapping("/{id}")
    public UserAccount update(@PathVariable UUID id, @RequestBody UpdateUserRequest req) {
        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (req.username() != null && !req.username().isBlank()) user.setUsername(req.username().trim());
        if (req.email() != null && !req.email().isBlank()) user.setEmail(req.email().trim());
        return userRepo.save(user);
    }

    @PutMapping("/{id}/enable")
    public UserAccount enable(@PathVariable UUID id) {
        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setEnabled(true);
        return userRepo.save(user);
    }

    @PutMapping("/{id}/disable")
    public UserAccount disable(@PathVariable UUID id) {
        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setEnabled(false);
        return userRepo.save(user);
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<Void> changePassword(@PathVariable UUID id,
                                               @RequestBody ChangePasswordRequest req,
                                               Authentication auth) {
        if (req.newPassword() == null || req.newPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters");
        }
        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepo.save(user);
        audit.log("PASSWORD_RESET", auth.getName(), "user", user.getUsername(), "Admin reset", null);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication auth) {
        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        userRepo.deleteById(id);
        audit.log("USER_DELETED", auth.getName(), "user", user.getUsername(), null, null);
    }

    public record CreateUserRequest(String username, String email, String password) {}
    public record UpdateUserRequest(String username, String email) {}
    public record ChangePasswordRequest(String newPassword) {}
}
