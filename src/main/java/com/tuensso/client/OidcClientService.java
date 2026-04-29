package com.tuensso.client;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OidcClientService {

    private static final String DEFAULT_ISSUER = "http://localhost:8080";

    private final RegisteredClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public OidcClientService(RegisteredClientRepository clientRepository,
                             PasswordEncoder passwordEncoder,
                             JdbcTemplate jdbcTemplate) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    public ClientView getByClientId(String clientId) {
        RegisteredClient client = clientRepository.findByClientId(clientId);
        if (client == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        ClientView base = ClientView.from(client);
        // Fetch extra columns not in RegisteredClient
        return jdbcTemplate.queryForObject(
                "select logo_uri, primary_color from oauth2_registered_client where client_id = ?",
                (rs, n) -> new ClientView(base.id(), base.clientId(), base.clientName(),
                        base.redirectUris(), base.scopes(), base.requirePkce(),
                        rs.getString("logo_uri"), rs.getString("primary_color")),
                clientId);
    }

    public List<ClientView> findAll() {
        return jdbcTemplate.query(
                        """
                select id, client_id, client_name, redirect_uris, scopes, client_settings, logo_uri, primary_color
                        from oauth2_registered_client
                        order by client_id asc
                        """,
                        (rs, rowNum) -> new ClientView(
                                rs.getString("id"),
                                rs.getString("client_id"),
                                rs.getString("client_name"),
                                splitCsv(rs.getString("redirect_uris")),
                                splitCsv(rs.getString("scopes")),
                    parseRequirePkce(rs.getString("client_settings")),
                    rs.getString("logo_uri"),
                    rs.getString("primary_color")))
                .stream()
                .sorted(Comparator.comparing(ClientView::clientId))
                .toList();
    }

    public ClientView create(CreateClientCommand command) {
        if (command.clientId() == null || !command.clientId().matches("^[a-zA-Z0-9_-]{1,64}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientId must be 1-64 alphanumeric/dash/underscore characters");
        }
        if (clientRepository.findByClientId(command.clientId()) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "clientId already exists");
        }

        List<String> redirectUris = command.redirectUris().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
        List<String> scopes = command.scopes().stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();

        if (redirectUris.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one redirect URI is required");
        }
        if (scopes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one scope is required");
        }

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(command.clientId())
                .clientName(command.clientName() == null || command.clientName().isBlank()
                        ? command.clientId()
                        : command.clientName().trim())
                .clientSecret(passwordEncoder.encode(command.clientSecret()))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(command.requirePkce())
                        .build())
                .tokenSettings(TokenSettings.builder().build());

        for (String redirectUri : redirectUris) {
            builder.redirectUri(redirectUri);
        }
        for (String scope : scopes) {
            builder.scope(scope);
        }
        if (!scopes.contains("openid")) {
            builder.scope("openid");
        }

        RegisteredClient client = builder.build();
        clientRepository.save(client);
        return ClientView.from(client);
    }

    public ClientView update(String clientId, UpdateClientCommand command) {
        RegisteredClient existing = clientRepository.findByClientId(clientId);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }

        String clientName = (command.clientName() == null || command.clientName().isBlank())
                ? existing.getClientName() : command.clientName().trim();

        List<String> redirectUris = command.redirectUris().stream()
                .map(String::trim).filter(v -> !v.isBlank()).toList();
        List<String> scopes = command.scopes().stream()
                .map(String::trim).filter(v -> !v.isBlank()).distinct().toList();

        if (redirectUris.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one redirect URI is required");
        }

        RegisteredClient.Builder builder = RegisteredClient.withId(existing.getId())
                .clientId(existing.getClientId())
                .clientName(clientName)
                .clientSecret(existing.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(command.requirePkce())
                        .build())
                .tokenSettings(TokenSettings.builder().build());

        redirectUris.forEach(builder::redirectUri);
        scopes.forEach(builder::scope);
        if (!scopes.contains("openid")) builder.scope("openid");

        RegisteredClient updated = builder.build();
        clientRepository.save(updated);
        if (command.primaryColor() != null) {
            jdbcTemplate.update("update oauth2_registered_client set primary_color = ? where client_id = ?",
                    command.primaryColor().isBlank() ? null : command.primaryColor().trim(), clientId);
        }
        return getByClientId(clientId);
    }

    public record UpdateClientCommand(String clientName, List<String> redirectUris,
                                      List<String> scopes, boolean requirePkce, String primaryColor) {}

    public void delete(String clientId) {
        if (clientRepository.findByClientId(clientId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
        jdbcTemplate.update("DELETE FROM client_user_assignment WHERE client_id = ?", clientId);
        jdbcTemplate.update("DELETE FROM oauth2_registered_client WHERE client_id = ?", clientId);
    }

    public void updateLogo(String clientId, String logoUri) {
        if (clientRepository.findByClientId(clientId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }

        int updated = jdbcTemplate.update(
                "update oauth2_registered_client set logo_uri = ? where client_id = ?",
                logoUri,
                clientId);

        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found");
        }
    }

    private static List<String> splitCsv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(","));
    }

    private static boolean parseRequirePkce(String clientSettingsJson) {
        return clientSettingsJson != null && clientSettingsJson.contains("\"requireProofKey\":true");
    }

    public record CreateClientCommand(String clientId,
                                      String clientName,
                                      String clientSecret,
                                      List<String> redirectUris,
                                      List<String> scopes,
                                      boolean requirePkce) {
    }

    public record ClientView(String id,
                             String clientId,
                             String clientName,
                             List<String> redirectUris,
                             List<String> scopes,
                             boolean requirePkce,
                             String logoUrl,
                             String primaryColor) {
        public static ClientView from(RegisteredClient client) {
            return new ClientView(
                    client.getId(),
                    client.getClientId(),
                    client.getClientName(),
                    client.getRedirectUris().stream().toList(),
                    client.getScopes().stream().toList(),
                    client.getClientSettings().isRequireProofKey(),
                    null,
                    null);
        }

        public String primaryRedirectUri() {
            return redirectUris.isEmpty() ? "" : redirectUris.getFirst();
        }

        public String scopesDisplay() {
            return String.join(", ", scopes);
        }

        public String authorizeExampleUrl() {
            String scopeValue = String.join(" ", scopes);
            return DEFAULT_ISSUER
                    + "/oauth2/authorize?client_id=" + clientId
                    + "&response_type=code"
                    + "&scope=" + scopeValue
                    + "&redirect_uri=" + primaryRedirectUri();
        }

        public String logoUrlOrDefault() {
            return logoUrl == null || logoUrl.isBlank() ? "/assets/default-app-logo.svg" : logoUrl;
        }

        public boolean hasLogo() {
            return logoUrl != null && !logoUrl.isBlank();
        }
    }
}