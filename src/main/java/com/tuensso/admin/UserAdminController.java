package com.tuensso.admin;

import java.util.List;
import java.util.UUID;

import com.tuensso.audit.AuditService;
import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import com.tuensso.user.UserAttribute;
import com.tuensso.user.UserAttributeRepository;
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
    private final UserAttributeRepository attrRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public UserAdminController(UserAccountRepository userRepo, UserAttributeRepository attrRepo,
                               PasswordEncoder passwordEncoder, AuditService audit) {
        this.userRepo = userRepo;
        this.attrRepo = attrRepo;
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
        if (req.firstName() != null) user.setFirstName(req.firstName().trim());
        if (req.lastName() != null) user.setLastName(req.lastName().trim());
        if (req.phone() != null) user.setPhone(req.phone().trim());
        if (req.address() != null) user.setAddress(req.address().trim());
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

    @PutMapping("/{id}/lock")
    public UserAccount lock(@PathVariable UUID id) {
        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setLocked(true);
        user.setLockedUntil(null); // Permanent lock until admin unlocks
        return userRepo.save(user);
    }

    @PutMapping("/{id}/unlock")
    public UserAccount unlock(@PathVariable UUID id) {
        UserAccount user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setLocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
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
        user.setPasswordChangedAt(java.time.Instant.now());
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
    public record UpdateUserRequest(String username, String email,
                                     String firstName, String lastName, String phone, String address) {}
    public record ChangePasswordRequest(String newPassword) {}

    // Custom attributes
    @GetMapping("/{id}/attributes")
    public java.util.List<UserAttribute> getAttributes(@PathVariable java.util.UUID id) {
        return attrRepo.findByUserId(id);
    }

    @PutMapping("/{id}/attributes")
    public UserAttribute setAttribute(@PathVariable java.util.UUID id,
                                      @RequestBody AttributeRequest req) {
        if (!userRepo.existsById(id)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        UserAttribute attr = attrRepo.findByUserIdAndKey(id, req.key()).orElseGet(() -> {
            UserAttribute a = new UserAttribute();
            a.setUserId(id);
            a.setKey(req.key());
            return a;
        });
        attr.setValue(req.value());
        return attrRepo.save(attr);
    }

    @jakarta.transaction.Transactional
    @DeleteMapping("/{id}/attributes/{key}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAttribute(@PathVariable java.util.UUID id, @PathVariable String key) {
        attrRepo.deleteByUserIdAndKey(id, key);
    }

    public record AttributeRequest(String key, String value) {}
}
