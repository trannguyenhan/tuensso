package com.tuensso.admin;

import java.time.Instant;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sessions")
public class SessionAdminController {

    private final JdbcTemplate jdbc;

    public SessionAdminController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<SessionRow> list() {
        return jdbc.query("""
            select id, principal_name, authorization_grant_type,
                   access_token_issued_at, access_token_expires_at,
                   registered_client_id
            from oauth2_authorization
            where access_token_value is not null
              and access_token_expires_at > now()
            order by access_token_issued_at desc
            """,
            (rs, n) -> new SessionRow(
                rs.getString("id"),
                rs.getString("principal_name"),
                rs.getString("authorization_grant_type"),
                rs.getTimestamp("access_token_issued_at") != null ? rs.getTimestamp("access_token_issued_at").toInstant() : null,
                rs.getTimestamp("access_token_expires_at") != null ? rs.getTimestamp("access_token_expires_at").toInstant() : null,
                rs.getString("registered_client_id")
            ));
    }

    @DeleteMapping("/{id}")
    public void revoke(@PathVariable String id) {
        jdbc.update("delete from oauth2_authorization where id = ?", id);
    }

    @DeleteMapping("/user/{username}")
    public void revokeAllForUser(@PathVariable String username) {
        jdbc.update("delete from oauth2_authorization where principal_name = ?", username);
    }

    public record SessionRow(String id, String username, String grantType,
                             Instant issuedAt, Instant expiresAt, String clientId) {}
}
