package com.tuensso.web;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/branding")
public class BrandingApiController {

    private final JdbcTemplate jdbcTemplate;

    public BrandingApiController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{clientId}")
    public BrandingResponse branding(@PathVariable String clientId) {
        return jdbcTemplate.query(
                "select client_name, logo_uri, primary_color from oauth2_registered_client where client_id = ?",
                rs -> {
                    if (!rs.next()) return new BrandingResponse(null, null, null);
                    return new BrandingResponse(rs.getString("client_name"), rs.getString("logo_uri"), rs.getString("primary_color"));
                },
                clientId);
    }

    public record BrandingResponse(String clientName, String logoUrl, String primaryColor) {}
}
