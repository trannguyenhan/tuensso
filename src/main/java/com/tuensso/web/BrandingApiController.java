package com.tuensso.web;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/branding")
public class BrandingApiController {

    private final JdbcTemplate jdbcTemplate;

    public BrandingApiController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<BrandingResponse> branding(@PathVariable String clientId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.MINUTES).cachePublic())
                .body(brandingForClient(clientId));
    }

    @Cacheable("branding")
    BrandingResponse brandingForClient(String clientId) {
        return jdbcTemplate.query(
                "select client_name, logo_uri, primary_color, powered_by_text from oauth2_registered_client where client_id = ?",
                rs -> {
                    if (!rs.next()) return new BrandingResponse(null, null, null, null);
                    return new BrandingResponse(rs.getString("client_name"), rs.getString("logo_uri"), rs.getString("primary_color"), rs.getString("powered_by_text"));
                },
                clientId);
    }

    public record BrandingResponse(String clientName, String logoUrl, String primaryColor, String poweredByText) {}
}
