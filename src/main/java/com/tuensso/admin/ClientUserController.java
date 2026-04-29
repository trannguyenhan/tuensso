package com.tuensso.admin;

import java.util.List;
import java.util.UUID;

import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/clients/{clientId}/users")
public class ClientUserController {

    private final JdbcTemplate jdbc;
    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;

    public ClientUserController(JdbcTemplate jdbc, UserAccountRepository userRepo, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<AssignedUser> list(@PathVariable String clientId) {
        return jdbc.query(
                "SELECT u.id, u.username, u.email, u.enabled FROM client_user_assignment a JOIN users u ON u.id = a.user_id WHERE a.client_id = ? ORDER BY u.username",
                (rs, n) -> new AssignedUser(rs.getObject("id", UUID.class), rs.getString("username"), rs.getString("email"), rs.getBoolean("enabled")),
                clientId);
    }

    /** Create user (or find by email) and assign to this app */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AssignedUser createAndAssign(@PathVariable String clientId, @RequestBody CreateAssignRequest req) {
        UserAccount user = userRepo.findByEmail(req.email().trim()).orElseGet(() -> {
            UserAccount u = new UserAccount();
            u.setUsername(req.username().trim());
            u.setEmail(req.email().trim());
            u.setPasswordHash(passwordEncoder.encode(req.password()));
            u.setEnabled(true);
            return userRepo.save(u);
        });
        jdbc.update("INSERT INTO client_user_assignment (client_id, user_id) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM client_user_assignment WHERE client_id = ? AND user_id = ?)", clientId, user.getId(), clientId, user.getId());
        return new AssignedUser(user.getId(), user.getUsername(), user.getEmail(), user.isEnabled());
    }

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable String clientId, @PathVariable UUID userId) {
        jdbc.update("INSERT INTO client_user_assignment (client_id, user_id) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM client_user_assignment WHERE client_id = ? AND user_id = ?)", clientId, userId, clientId, userId);
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String clientId, @PathVariable UUID userId) {
        jdbc.update("DELETE FROM client_user_assignment WHERE client_id = ? AND user_id = ?", clientId, userId);
    }

    public record CreateAssignRequest(String username, String email, String password) {}
    public record AssignedUser(UUID id, String username, String email, boolean enabled) {}
}
