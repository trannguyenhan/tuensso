package com.tuensso.config;

import java.util.ArrayList;
import java.util.List;

import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.oidc.endpoint.OidcParameterNames;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

@Configuration
public class TokenCustomizerConfig {

    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer(UserAccountRepository userRepo) {
        return context -> {
            String username = context.getPrincipal().getName();
            UserAccount user = userRepo.findByUsername(username)
                    .or(() -> userRepo.findByEmail(username))
                    .orElse(null);
            if (user == null) return;

            UserAccount withGroups = userRepo.findWithGroupsAndRolesById(user.getId()).orElse(user);
            List<String> groups = new ArrayList<>(withGroups.getGroups().stream()
                    .map(g -> g.getName()).sorted().toList());
            List<String> roles = new ArrayList<>(withGroups.getRoles().stream()
                    .map(r -> r.getName()).sorted().toList());

            if (context.getTokenType().getValue().equals(OidcParameterNames.ID_TOKEN)
                    || "access_token".equals(context.getTokenType().getValue())) {
                context.getClaims().claim("email", user.getEmail());
                context.getClaims().claim("preferred_username", user.getUsername());
                context.getClaims().claim("name", user.getUsername());
                context.getClaims().claim("groups", groups);
                context.getClaims().claim("roles", roles);
            }
        };
    }
}
