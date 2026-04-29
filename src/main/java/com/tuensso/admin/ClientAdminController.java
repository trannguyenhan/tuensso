package com.tuensso.admin;

import java.util.List;

import com.tuensso.client.AppLogoStorageService;
import com.tuensso.client.OidcClientService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/admin/clients")
public class ClientAdminController {

    private final OidcClientService oidcClientService;
    private final AppLogoStorageService appLogoStorageService;

    public ClientAdminController(OidcClientService oidcClientService,
                                 AppLogoStorageService appLogoStorageService) {
        this.oidcClientService = oidcClientService;
        this.appLogoStorageService = appLogoStorageService;
    }

    @GetMapping
    public List<OidcClientService.ClientView> list() {
        return oidcClientService.findAll();
    }

    @GetMapping("/{clientId}")
    public OidcClientService.ClientView get(@PathVariable String clientId) {
        return oidcClientService.getByClientId(clientId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OidcClientService.ClientView register(@RequestBody RegisterClientRequest req) {
        return oidcClientService.create(new OidcClientService.CreateClientCommand(
                req.clientId(),
                req.clientName(),
                req.clientSecret(),
                req.redirectUris(),
                req.scopes(),
                req.requirePkce()));
    }

    @PostMapping(path = "/{clientId}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public OidcClientService.ClientView uploadLogo(@PathVariable String clientId,
                                                   @RequestParam("logo") MultipartFile logo) {
        String logoUri = appLogoStorageService.store(clientId, logo);
        oidcClientService.updateLogo(clientId, logoUri);
        return oidcClientService.getByClientId(clientId);
    }

    @PutMapping("/{clientId}")
    public OidcClientService.ClientView update(@PathVariable String clientId,
                                               @RequestBody UpdateClientRequest req) {
        return oidcClientService.update(clientId, new OidcClientService.UpdateClientCommand(
                req.clientName(), req.redirectUris(), req.scopes(), req.requirePkce(), req.primaryColor()));
    }

    @DeleteMapping("/{clientId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String clientId) {
        oidcClientService.delete(clientId);
    }

    public record RegisterClientRequest(
            String clientId,
            String clientName,
            String clientSecret,
            List<String> redirectUris,
            List<String> scopes,
            boolean requirePkce) {}

    public record UpdateClientRequest(
            String clientName,
            List<String> redirectUris,
            List<String> scopes,
            boolean requirePkce,
            String primaryColor) {}
}
