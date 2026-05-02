package com.tuensso.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sso")
public class SsoLogoutController {

    private final JdbcTemplate jdbcTemplate;

    public SsoLogoutController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/logout/validate")
    public LogoutValidation validateLogout(@RequestParam(required = false) String client_id,
                                           @RequestParam(required = false) String redirect_uri,
                                           @RequestParam(required = false) String state) {
        if (client_id == null || client_id.isBlank()) {
            return new LogoutValidation(false, null, null);
        }

        String clientName = jdbcTemplate.query(
                "select client_name from oauth2_registered_client where client_id = ?",
                rs -> rs.next() ? rs.getString("client_name") : null,
                client_id);

        if (clientName == null) {
            return new LogoutValidation(false, null, null);
        }

        String validatedRedirect = validateRedirectUri(client_id, redirect_uri, state);
        return new LogoutValidation(true, clientName, validatedRedirect);
    }

    @PostMapping("/logout")
    public LogoutResult performLogout(HttpServletRequest request,
                                      @RequestBody LogoutRequest body) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        // Re-validate redirect_uri server-side (don't trust client)
        String redirect = null;
        if (body.clientId != null && body.redirectUri != null) {
            redirect = validateRedirectUri(body.clientId, body.redirectUri, body.state);
        }
        return new LogoutResult(true, redirect);
    }

    /**
     * Validates redirect_uri against post_logout_redirect_uris first, then redirect_uris as fallback.
     */
    private String validateRedirectUri(String clientId, String redirectUri, String state) {
        if (redirectUri == null || redirectUri.isBlank()) return null;

        String[] columns = jdbcTemplate.query(
                "select redirect_uris, post_logout_redirect_uris from oauth2_registered_client where client_id = ?",
                rs -> {
                    if (rs.next()) {
                        return new String[]{ rs.getString("redirect_uris"), rs.getString("post_logout_redirect_uris") };
                    }
                    return new String[]{ "", "" };
                },
                clientId);

        String requested = redirectUri.trim();
        String allUris = ((columns[1] != null ? columns[1] : "") + "," + (columns[0] != null ? columns[0] : ""));
        for (String registered : allUris.split(",")) {
            if (requested.equals(registered.trim())) {
                if (state != null && !state.isBlank()) {
                    return UriComponentsBuilder.fromUriString(requested)
                            .queryParam("state", state)
                            .build(true)
                            .toUriString();
                }
                return requested;
            }
        }
        return null; // Not in registered URIs — reject
    }

    public record LogoutValidation(boolean valid, String clientName, String redirectUri) {}
    public record LogoutRequest(String clientId, String redirectUri, String state) {}
    public record LogoutResult(boolean success, String redirectUri) {}
}
