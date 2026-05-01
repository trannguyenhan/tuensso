package com.tuensso.user;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
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

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        Set<String> permissions = new LinkedHashSet<>();
        full.getRoles().forEach(r -> {
            String roleName = r.getName().toUpperCase().replace(" ", "_");
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            // Collect permissions from all roles
            r.getPermissionList().forEach(p -> permissions.add(p.toLowerCase()));
        });

        // Add permissions as PERM_* authorities
        permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority("PERM_" + p)));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .disabled(!user.isEnabled())
                .accountLocked(user.isLocked())
                .authorities(authorities)
                .build();
    }
}
