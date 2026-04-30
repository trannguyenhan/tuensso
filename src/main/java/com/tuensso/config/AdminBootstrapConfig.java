package com.tuensso.config;

import com.tuensso.group.UserGroup;
import com.tuensso.group.UserGroupRepository;
import com.tuensso.user.UserAccount;
import com.tuensso.user.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfig {

    @Value("${tuensso.admin.username:}")
    private String adminUsername;

    @Value("${tuensso.admin.password:}")
    private String adminPassword;

    @Value("${tuensso.admin.email:admin@tuensso.local}")
    private String adminEmail;

    @Bean
    CommandLineRunner bootstrapAdmin(UserAccountRepository userRepo,
                                     UserGroupRepository groupRepo,
                                     PasswordEncoder passwordEncoder) {
        return args -> {
            if (adminUsername == null || adminUsername.isBlank() || adminPassword == null || adminPassword.isBlank()) return;

            // Ensure admins group exists
            UserGroup adminsGroup = groupRepo.findByName("admins").orElseGet(() -> {
                UserGroup g = new UserGroup();
                g.setName("admins");
                g.setDescription("Administrators");
                return groupRepo.save(g);
            });

            // Create admin user if not exists
            if (userRepo.findByUsername(adminUsername).isEmpty()) {
                UserAccount admin = new UserAccount();
                admin.setUsername(adminUsername);
                admin.setEmail(adminEmail);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setEnabled(true);
                admin.getGroups().add(adminsGroup);
                userRepo.save(admin);
            }
        };
    }
}
