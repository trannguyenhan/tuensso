package com.tuensso.web;

import java.util.List;

import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oidc")
public class OidcMetadataController {

    private final AuthorizationServerSettings authorizationServerSettings;

    public OidcMetadataController(AuthorizationServerSettings authorizationServerSettings) {
        this.authorizationServerSettings = authorizationServerSettings;
    }

    @GetMapping("/endpoints")
    public OidcEndpointsResponse endpoints() {
        String issuer = authorizationServerSettings.getIssuer();
        return new OidcEndpointsResponse(
                issuer,
                issuer + "/.well-known/openid-configuration",
                List.of(
                        new OidcEndpoint("authorization", issuer + "/oauth2/authorize"),
                        new OidcEndpoint("token", issuer + "/oauth2/token"),
                        new OidcEndpoint("jwks", issuer + "/oauth2/jwks"),
                        new OidcEndpoint("userinfo", issuer + "/userinfo"),
                        new OidcEndpoint("introspection", issuer + "/oauth2/introspect"),
                        new OidcEndpoint("revocation", issuer + "/oauth2/revoke")
                ),
                List.of(
                        new SampleClient("app1", "http://127.0.0.1:8081/login/oauth2/code/app1")
                ));
    }

    public record OidcEndpointsResponse(String issuer,
                                        String discovery,
                                        List<OidcEndpoint> endpoints,
                                        List<SampleClient> sampleClients) {
    }

    public record OidcEndpoint(String name, String url) {
    }

    public record SampleClient(String clientId, String redirectUri) {
    }
}