package com.tuensso.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
public class ConnectLogoutController {

    @GetMapping("/connect/logout")
    public RedirectView rpInitiatedLogout(
            @RequestParam(name = "client_id", required = false) String clientId,
            @RequestParam(name = "post_logout_redirect_uri", required = false) String postLogoutRedirectUri,
            @RequestParam(name = "redirect_uri", required = false) String redirectUri,
            @RequestParam(name = "state", required = false) String state,
            @RequestParam(name = "id_token_hint", required = false) String idTokenHint) {

        String resolvedRedirect = (postLogoutRedirectUri != null && !postLogoutRedirectUri.isBlank())
                ? postLogoutRedirectUri
                : redirectUri;

        UriComponentsBuilder b = UriComponentsBuilder.fromPath("/sso-logout");
        if (clientId != null && !clientId.isBlank()) {
            b.queryParam("client_id", clientId);
        }
        if (resolvedRedirect != null && !resolvedRedirect.isBlank()) {
            b.queryParam("redirect_uri", resolvedRedirect);
        }
        if (state != null && !state.isBlank()) {
            b.queryParam("state", state);
        }

        // Accepted for compatibility with OIDC RP-initiated logout callers.
        // This implementation does not validate id_token_hint currently.
        if (idTokenHint != null && !idTokenHint.isBlank()) {
            b.queryParam("id_token_hint", idTokenHint);
        }

        RedirectView redirectView = new RedirectView(b.build(true).toUriString());
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }
}
