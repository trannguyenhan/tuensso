package com.tuensso.audit;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthEventListener {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    private final AuditService auditService;
    private final UserAccountRepository userRepo;

    public AuthEventListener(AuditService auditService, UserAccountRepository userRepo) {
        this.auditService = auditService;
        this.userRepo = userRepo;
    }

    @EventListener
    public void onSuccess(AuthenticationSuccessEvent event) {
        String username = event.getAuthentication().getName();
        if (event.getAuthentication().getClass().getSimpleName().contains("OAuth2Client")) return;

        String ip = remoteIp();
        auditService.log("LOGIN_SUCCESS", username, "user", username, null, ip);

        // Reset failed attempts and track last login
        userRepo.findByUsername(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLastLoginAt(Instant.now());
            user.setLastLoginIp(ip);
            userRepo.save(user);
        });
    }

    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent event) {
        String username = event.getAuthentication().getName();
        String reason = event.getException().getClass().getSimpleName();
        String ip = remoteIp();
        auditService.log("LOGIN_FAILURE", username, "user", username, reason, ip);

        // Increment failed attempts and lock if threshold reached
        userRepo.findByUsername(username)
                .or(() -> userRepo.findByEmail(username))
                .ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS && !user.isLocked()) {
                user.setLocked(true);
                user.setLockedUntil(Instant.now().plus(LOCK_DURATION_MINUTES, ChronoUnit.MINUTES));
                auditService.log("ACCOUNT_LOCKED", username, "user", username,
                        "Locked after " + attempts + " failed attempts (" + LOCK_DURATION_MINUTES + " min)", ip);
            }
            userRepo.save(user);
        });
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
