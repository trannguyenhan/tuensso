package com.tuensso.web;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getParameterName(), csrfToken.getToken());
    }

    @GetMapping("/session")
    public SessionResponse session(Authentication authentication, HttpServletRequest request) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return new SessionResponse(false, null, List.of(), request.getParameter("client_id"));
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        return new SessionResponse(true, authentication.getName(), roles, request.getParameter("client_id"));
    }

    public record CsrfResponse(String headerName, String parameterName, String token) {
    }

    public record SessionResponse(boolean authenticated, String username, List<String> roles, String clientId) {
    }
}
