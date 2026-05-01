package com.tuensso.config;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.servlet.PathRequest;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
public class SecurityConfig {

    // -----------------------------------------------------------------------
    // Filter chain 1 — Authorization Server endpoints (OIDC/OAuth2)
    // /oauth2/authorize, /oauth2/token, /userinfo, /oauth2/jwks, v.v.
    // -----------------------------------------------------------------------
    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .oidc(Customizer.withDefaults());

        http.oauth2ResourceServer(rs -> rs.jwt(Customizer.withDefaults()));

        http.exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                        new SsoLoginEntryPoint(),
                        new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));

        return http.build();
    }

    // -----------------------------------------------------------------------
    // Filter chain 2 — Login form + Admin API
    // /admin/** requires ROLE_ADMIN
    // -----------------------------------------------------------------------
    @Bean
    @Order(2)
    SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
            .requestMatchers("/", "/login", "/sso-login", "/sso-logout", "/admin/login", "/index.html", "/error", "/api/auth/**", "/api/oidc/**", "/api/branding/**", "/api/sso/**").permitAll()
            .requestMatchers("/*.js", "/*.css", "/*.txt", "/*.map", "/assets/**").permitAll()
                .requestMatchers("/admin/**", "/api/admin/console/**").hasAnyAuthority(
                        "PERM_dashboard", "PERM_apps", "PERM_users", "PERM_groups",
                        "PERM_roles", "PERM_sessions", "PERM_audit", "PERM_integration")
                .requestMatchers("/api/admin/clients/**").hasAuthority("PERM_apps")
                .requestMatchers("/api/admin/users/**").hasAuthority("PERM_users")
                .requestMatchers("/api/admin/groups/**").hasAuthority("PERM_groups")
                .requestMatchers("/api/admin/roles/**").hasAuthority("PERM_roles")
                .requestMatchers("/api/admin/sessions/**").hasAuthority("PERM_sessions")
                .requestMatchers("/api/admin/audit/**").hasAuthority("PERM_audit")
                .requestMatchers("/api/admin/scopes/**").hasAuthority("PERM_apps")
                .requestMatchers("/account", "/dashboard", "/api/me").authenticated()
                .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                .failureHandler((request, response, exception) -> {
                    String clientId = request.getParameter("client_id");
                    String sessionCode = request.getParameter("session_code");
                    String tabId = request.getParameter("tab_id");

                    var saved = new org.springframework.security.web.savedrequest.HttpSessionRequestCache()
                            .getRequest(request, response);

                    if ((clientId == null || clientId.isBlank()) && saved != null) {
                        try {
                            clientId = UriComponentsBuilder.fromUriString(saved.getRedirectUrl())
                                    .build()
                                    .getQueryParams()
                                    .getFirst("client_id");
                        } catch (Exception ignored) {
                            // Keep graceful fallback to default login screens.
                        }
                    }

                    if (clientId != null && !clientId.isBlank()) {
                        StringBuilder ssoErrorUrl = new StringBuilder("/sso-login?error=")
                                .append("&client_id=")
                                .append(java.net.URLEncoder.encode(clientId, java.nio.charset.StandardCharsets.UTF_8));
                        if (sessionCode != null && !sessionCode.isBlank()) {
                            ssoErrorUrl.append("&session_code=")
                                    .append(java.net.URLEncoder.encode(sessionCode, java.nio.charset.StandardCharsets.UTF_8));
                        }
                        if (tabId != null && !tabId.isBlank()) {
                            ssoErrorUrl.append("&tab_id=")
                                    .append(java.net.URLEncoder.encode(tabId, java.nio.charset.StandardCharsets.UTF_8));
                        }
                        response.sendRedirect(ssoErrorUrl.toString());
                        return;
                    }

                    if ("1".equals(request.getParameter("admin"))) {
                        response.sendRedirect("/admin/login?error=");
                    } else {
                        response.sendRedirect("/login?error=");
                    }
                })
                .successHandler((request, response, authentication) -> {
                    var saved = new org.springframework.security.web.savedrequest.HttpSessionRequestCache()
                            .getRequest(request, response);
                    if (saved != null) {
                        response.sendRedirect(saved.getRedirectUrl());
                    } else {
                        String dest = "1".equals(request.getParameter("admin"))
                                ? "/admin/dashboard" : "/dashboard";
                        response.sendRedirect(dest);
                    }
                })
                        .permitAll())
                .sessionManagement(sm -> sm.maximumSessions(-1).sessionRegistry(sessionRegistry()))
                .logout(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(org.springframework.security.web.csrf.CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(new org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers("/api/sso/logout"))
                .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class);

        return http.build();
    }

    // Eagerly load CSRF token so the cookie is always set for SPA
    static class CsrfCookieFilter extends org.springframework.web.filter.OncePerRequestFilter {
        @Override
        protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response,
                                        jakarta.servlet.FilterChain filterChain) throws jakarta.servlet.ServletException, java.io.IOException {
            org.springframework.security.web.csrf.CsrfToken csrfToken =
                    (org.springframework.security.web.csrf.CsrfToken) request.getAttribute(org.springframework.security.web.csrf.CsrfToken.class.getName());
            if (csrfToken != null) csrfToken.getToken(); // force cookie write
            filterChain.doFilter(request, response);
        }
    }

    // -----------------------------------------------------------------------
    // Registered clients — stored in DB, managed via /api/admin/clients API
    // -----------------------------------------------------------------------
    @Bean
    RegisteredClientRepository registeredClientRepository(DataSource dataSource) {
        return new JdbcRegisteredClientRepository(new JdbcTemplate(dataSource));
    }

    // -----------------------------------------------------------------------
    // Authorization + Consent service — stored in DB
    // -----------------------------------------------------------------------
    @Bean
    OAuth2AuthorizationService authorizationService(DataSource dataSource,
                                                    RegisteredClientRepository clients) {
        JdbcOAuth2AuthorizationService service = new JdbcOAuth2AuthorizationService(new JdbcTemplate(dataSource), clients);

        JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper rowMapper =
                new JdbcOAuth2AuthorizationService.OAuth2AuthorizationRowMapper(clients);

        com.fasterxml.jackson.databind.ObjectMapper objectMapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
        ClassLoader classLoader = JdbcOAuth2AuthorizationService.class.getClassLoader();
        objectMapper.registerModules(org.springframework.security.jackson2.SecurityJackson2Modules.getModules(classLoader));
        objectMapper.registerModule(new org.springframework.security.oauth2.server.authorization.jackson2.OAuth2AuthorizationServerJackson2Module());

        // Allow immutable collection types produced by List.of(), Set.of(), Map.of()
        for (var collection : java.util.List.of(
                java.util.List.of("a"),
                java.util.List.of("a", "b"),
                java.util.List.of("a", "b", "c"),
                java.util.Set.of("a"),
                java.util.Set.of("a", "b"),
                java.util.Set.of("a", "b", "c"),
                java.util.Collections.unmodifiableList(java.util.List.of()),
                java.util.Collections.unmodifiableSet(java.util.Set.of()),
                java.util.Collections.unmodifiableMap(java.util.Map.of()))) {
            objectMapper.addMixIn(collection.getClass(), UnmodifiableCollectionMixin.class);
        }

        rowMapper.setObjectMapper(objectMapper);
        service.setAuthorizationRowMapper(rowMapper);
        return service;
    }

    @com.fasterxml.jackson.annotation.JsonTypeInfo(use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.CLASS)
    @com.fasterxml.jackson.annotation.JsonAutoDetect(
            fieldVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY,
            getterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE,
            isGetterVisibility = com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE)
    abstract static class UnmodifiableCollectionMixin {}

    @Bean
    OAuth2AuthorizationConsentService authorizationConsentService(DataSource dataSource,
                                                                  RegisteredClientRepository clients) {
        return new JdbcOAuth2AuthorizationConsentService(new JdbcTemplate(dataSource), clients);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    org.springframework.security.core.session.SessionRegistry sessionRegistry() {
        return new org.springframework.security.core.session.SessionRegistryImpl();
    }

    // -----------------------------------------------------------------------
    // RSA key for signing JWT — TODO Phase 2: load from file / KMS
    // -----------------------------------------------------------------------
    @Bean
    JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings(@Value("${tuensso.issuer}") String issuer) {
        return AuthorizationServerSettings.builder()
                .issuer(issuer)
                .build();
    }

    private static RSAKey generateRsa() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    private static KeyPair generateRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
