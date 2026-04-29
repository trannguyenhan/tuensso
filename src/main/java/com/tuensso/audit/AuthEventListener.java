package com.tuensso.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthEventListener {

    private final AuditService auditService;

    public AuthEventListener(AuditService auditService) {
        this.auditService = auditService;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        // Skip OAuth2 client authentications (client_credentials)
        if (event.getAuthentication().getClass().getSimpleName().contains("OAuth2Client")) return;
        auditService.log("LOGIN_SUCCESS", username, "user", username, null, remoteIp());
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getClass().getSimpleName();
        auditService.log("LOGIN_FAILURE", username, "user", username, reason, remoteIp());
    }

    private String remoteIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest req = attrs.getRequest();
            String xff = req.getHeader("X-Forwarded-For");
            return xff != null ? xff.split(",")[0].trim() : req.getRemoteAddr();
        } catch (Exception e) { return null; }
    }
}
