package com.tuensso.config;

import java.util.UUID;

import com.tuensso.group.UserGroup;
import com.tuensso.group.UserGroupRepository;
import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

@Configuration
@Profile("dev")
public class BootstrapDataConfig {

    @Bean
    org.springframework.boot.CommandLineRunner bootstrapUsers(UserAccountRepository userAccountRepository,
                                                              UserGroupRepository userGroupRepository,
                                                              PasswordEncoder passwordEncoder) {
        return args -> {
            UserGroup adminsGroup = seedGroup(userGroupRepository, "admins", "Admin operators");
            seedUser(userAccountRepository, passwordEncoder, "admin", "admin@tuensso.local", "123456", adminsGroup);
            seedUser(userAccountRepository, passwordEncoder, "user", "user@tuensso.local", "123456");
        };
    }

    @Bean
    org.springframework.boot.CommandLineRunner bootstrapClients(RegisteredClientRepository registeredClientRepository,
                                                                PasswordEncoder passwordEncoder) {
        return args -> {
            seedClient(registeredClientRepository, passwordEncoder,
                    "app1", "Demo App 1", "app1-secret",
                    "http://127.0.0.1:8081/login/oauth2/code/app1");

            seedClient(registeredClientRepository, passwordEncoder,
                    "demo-app1", "Demo App 1", "demo-app1-secret",
                    "http://localhost:8081/login/oauth2/code/tuensso");

            seedClient(registeredClientRepository, passwordEncoder,
                    "demo-app2", "Demo App 2", "demo-app2-secret",
                    "http://localhost:8082/callback");
        };
    }

    private static void seedClient(RegisteredClientRepository repo,
                                   PasswordEncoder encoder,
                                   String clientId, String clientName,
                                   String rawSecret, String redirectUri) {
        if (repo.findByClientId(clientId) != null) return;
        RegisteredClient client = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientName(clientName)
                .clientSecret(encoder.encode(rawSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(redirectUri)
                .scope("openid").scope("profile").scope("email")
                .clientSettings(ClientSettings.builder().requireProofKey(false).build())
                .tokenSettings(TokenSettings.builder().build())
                .build();
        repo.save(client);
    }

    private static void seedUser(UserAccountRepository userAccountRepository,
                                 PasswordEncoder passwordEncoder,
                                 String username,
                                 String email,
                                 String rawPassword,
                                 UserGroup... groups) {
        java.util.Optional<UserAccount> existing = userAccountRepository.findByUsername(username)
                .flatMap(user -> userAccountRepository.findWithGroupsById(user.getId()));
        if (existing.isPresent()) {
            UserAccount user = existing.get();
            for (UserGroup group : groups) {
                user.getGroups().add(group);
            }
            userAccountRepository.save(user);
            return;
        }

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setEnabled(true);
        for (UserGroup group : groups) {
            user.getGroups().add(group);
        }
        userAccountRepository.save(user);
    }

    private static UserGroup seedGroup(UserGroupRepository userGroupRepository,
                                       String name,
                                       String description) {
        return userGroupRepository.findByName(name).orElseGet(() -> {
            UserGroup group = new UserGroup();
            group.setName(name);
            group.setDescription(description);
            return userGroupRepository.save(group);
        });
    }
}