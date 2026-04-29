package com.demo.app1;

import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, Object> home(@AuthenticationPrincipal OidcUser user) {
        Map<String, Object> result = new HashMap<>();
        result.put("app", "Demo App 1 (Spring Boot)");
        result.put("message", "You are logged in via TuenSSO!");
        result.put("sub", user.getSubject());
        result.put("name", user.getPreferredUsername());
        result.put("email", user.getEmail());
        result.put("claims", user.getClaims());
        return result;
    }
}
