package com.tuensso.config;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Redirects to /login with Keycloak-style query parameters:
 * /login?client_id=xxx&tab_id=yyy&session_code=zzz
 */
public class SsoLoginEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        String clientId = request.getParameter("client_id");
        String sessionCode = UUID.randomUUID().toString().substring(0, 8);
        String tabId = UUID.randomUUID().toString().substring(0, 6);

        StringBuilder url = new StringBuilder(clientId != null && !clientId.isBlank()
                ? "/sso-login?session_code=" : "/login?session_code=").append(sessionCode)
                .append("&tab_id=").append(tabId);
        if (clientId != null && !clientId.isBlank()) {
            url.append("&client_id=").append(URLEncoder.encode(clientId, StandardCharsets.UTF_8));
        }

        response.sendRedirect(url.toString());
    }
}
