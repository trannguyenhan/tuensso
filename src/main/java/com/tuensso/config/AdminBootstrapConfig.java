package com.tuensso.config;

import com.tuensso.role.Role;
import com.tuensso.role.RoleRepository;
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
                                     RoleRepository roleRepo,
                                     PasswordEncoder passwordEncoder) {
        return args -> {
            if (adminUsername == null || adminUsername.isBlank() || adminPassword == null || adminPassword.isBlank()) return;

            Role adminRole = roleRepo.findByName("ADMIN").orElseGet(() -> {
                Role r = new Role();
                r.setName("ADMIN");
                r.setDescription("System administrator (built-in)");
                r.setPermissions(String.join(",", Role.ALL_PERMISSIONS));
                return roleRepo.save(r);
            });

            if (userRepo.findByUsername(adminUsername).isEmpty()) {
                UserAccount admin = new UserAccount();
                admin.setUsername(adminUsername);
                admin.setEmail(adminEmail);
                admin.setPasswordHash(passwordEncoder.encode(adminPassword));
                admin.setEnabled(true);
                admin.setSystemAccount(true);
                admin.getRoles().add(adminRole);
                userRepo.save(admin);
            }
        };
    }
}
