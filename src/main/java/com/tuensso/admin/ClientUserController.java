package com.tuensso.admin;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/clients/{clientId}/users")
public class ClientUserController {

    private final JdbcTemplate jdbc;
    private final UserAccountRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final boolean binaryUuidColumns;

    public ClientUserController(JdbcTemplate jdbc, UserAccountRepository userRepo, PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.binaryUuidColumns = detectBinaryUuidColumns(jdbc);
    }

    @GetMapping
    public List<AssignedUser> list(@PathVariable String clientId) {
        return jdbc.query(
                "SELECT u.id, u.username, u.email, u.enabled FROM client_user_assignment a JOIN users u ON u.id = a.user_id WHERE a.client_id = ? ORDER BY u.username",
                (rs, n) -> new AssignedUser(readUserId(rs.getObject("id")), rs.getString("username"), rs.getString("email"), rs.getBoolean("enabled")),
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
        jdbc.update("INSERT INTO client_user_assignment (client_id, user_id) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM client_user_assignment WHERE client_id = ? AND user_id = ?)",
            clientId,
            bindUserId(user.getId()),
            clientId,
            bindUserId(user.getId()));
        return new AssignedUser(user.getId(), user.getUsername(), user.getEmail(), user.isEnabled());
    }

    @PostMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void add(@PathVariable String clientId, @PathVariable UUID userId) {
        jdbc.update("INSERT INTO client_user_assignment (client_id, user_id) SELECT ?, ? WHERE NOT EXISTS (SELECT 1 FROM client_user_assignment WHERE client_id = ? AND user_id = ?)",
                clientId,
                bindUserId(userId),
                clientId,
                bindUserId(userId));
    }

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String clientId, @PathVariable UUID userId) {
        jdbc.update("DELETE FROM client_user_assignment WHERE client_id = ? AND user_id = ?", clientId, bindUserId(userId));
    }

    private static boolean detectBinaryUuidColumns(JdbcTemplate jdbc) {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) con -> {
            String product = con.getMetaData().getDatabaseProductName();
            if (product == null) {
                return false;
            }
            String normalized = product.toLowerCase(Locale.ROOT);
            return normalized.contains("mariadb") || normalized.contains("mysql");
        }));
    }

    private Object bindUserId(UUID userId) {
        if (!binaryUuidColumns) {
            return userId;
        }
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.putLong(userId.getMostSignificantBits());
        bb.putLong(userId.getLeastSignificantBits());
        return bb.array();
    }

    private UUID readUserId(Object dbValue) {
        if (dbValue instanceof UUID uuid) {
            return uuid;
        }
        if (dbValue instanceof byte[] bytes) {
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            return new UUID(bb.getLong(), bb.getLong());
        }
        if (dbValue instanceof String value) {
            return UUID.fromString(value);
        }
        throw new IllegalStateException("Unsupported user id type from database: " + dbValue);
    }

    public record CreateAssignRequest(String username, String email, String password) {}
    public record AssignedUser(UUID id, String username, String email, boolean enabled) {}
}
