package com.tuensso.user;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DbUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public DbUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userAccountRepository.findByUsername(username)
                .or(() -> userAccountRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // Auto-unlock if lock has expired
        if (user.isLocked() && user.getLockedUntil() != null && Instant.now().isAfter(user.getLockedUntil())) {
            user.setLocked(false);
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userAccountRepository.save(user);
        }

        // Eagerly load groups and roles
        UserAccount full = userAccountRepository.findWithGroupsAndRolesById(user.getId()).orElse(user);

        List<String> roles = new ArrayList<>();
        roles.add("USER");
        boolean isAdmin = full.getGroups().stream()
                .anyMatch(g -> "admins".equalsIgnoreCase(g.getName()));
        if (isAdmin) {
            roles.add("ADMIN");
        }
        // Add custom roles from role assignments
        full.getRoles().forEach(r -> {
            String name = r.getName().toUpperCase().replace(" ", "_");
            if (!roles.contains(name)) roles.add(name);
        });

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .accountLocked(user.isLocked())
                .roles(roles.toArray(String[]::new))
                .build();
    }
}
