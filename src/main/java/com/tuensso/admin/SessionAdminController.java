package com.tuensso.admin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/sessions")
public class SessionAdminController {

    private final SessionRegistry sessionRegistry;
    private final JdbcTemplate jdbc;

    public SessionAdminController(SessionRegistry sessionRegistry, JdbcTemplate jdbc) {
        this.sessionRegistry = sessionRegistry;
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<SessionRow> list() {
        List<SessionRow> result = new ArrayList<>();

        // HTTP sessions (admin/user login)
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String username = principal instanceof org.springframework.security.core.userdetails.UserDetails ud
                    ? ud.getUsername() : principal.toString();
            for (SessionInformation si : sessionRegistry.getAllSessions(principal, false)) {
                result.add(new SessionRow(
                        si.getSessionId(), username, "http_session",
                        si.getLastRequest().toInstant(), null, null, si.isExpired()));
            }
        }

        // OAuth2 authorizations (OIDC client sessions)
        jdbc.query("""
            select id, principal_name, authorization_grant_type,
                   access_token_issued_at, access_token_expires_at, registered_client_id
            from oauth2_authorization
            where access_token_value is not null
            order by access_token_issued_at desc
            limit 100
            """,
            (rs, n) -> {
                Instant expires = rs.getTimestamp("access_token_expires_at") != null
                        ? rs.getTimestamp("access_token_expires_at").toInstant() : null;
                result.add(new SessionRow(
                    rs.getString("id"),
                    rs.getString("principal_name"),
                    rs.getString("authorization_grant_type"),
                    rs.getTimestamp("access_token_issued_at") != null ? rs.getTimestamp("access_token_issued_at").toInstant() : null,
                    expires,
                    rs.getString("registered_client_id"),
                    expires != null && Instant.now().isAfter(expires)));
                return null;
            });

        return result;
    }

    @DeleteMapping("/{id}")
    public void revoke(@PathVariable String id) {
        // Try HTTP session first
        SessionInformation si = sessionRegistry.getSessionInformation(id);
        if (si != null) {
            si.expireNow();
        }
        // Also try OAuth2 authorization
        jdbc.update("delete from oauth2_authorization where id = ?", id);
    }

    @DeleteMapping("/user/{username}")
    public void revokeAllForUser(@PathVariable String username) {
        // Expire all HTTP sessions
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            String name = principal instanceof org.springframework.security.core.userdetails.UserDetails ud
                    ? ud.getUsername() : principal.toString();
            if (username.equals(name)) {
                sessionRegistry.getAllSessions(principal, false).forEach(SessionInformation::expireNow);
            }
        }
        // Delete OAuth2 authorizations
        jdbc.update("delete from oauth2_authorization where principal_name = ?", username);
    }

    public record SessionRow(String id, String username, String grantType,
                             Instant issuedAt, Instant expiresAt, String clientId, boolean expired) {}
}
