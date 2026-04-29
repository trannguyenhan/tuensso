package com.tuensso.admin;

import java.util.List;

import com.tuensso.client.OidcClientService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/console")
public class AdminConsoleApiController {

    private final AdminConsoleService adminConsoleService;
    private final OidcClientService oidcClientService;
    private final String issuer;

    public AdminConsoleApiController(AdminConsoleService adminConsoleService,
                                     OidcClientService oidcClientService,
                                     @Value("${tuensso.issuer}") String issuer) {
        this.adminConsoleService = adminConsoleService;
        this.oidcClientService = oidcClientService;
        this.issuer = issuer;
    }

    @GetMapping("/bootstrap")
    public BootstrapResponse bootstrap() {
        return new BootstrapResponse(
                issuer,
                oidcClientService.findAll(),
                adminConsoleService.users(),
                adminConsoleService.groups());
    }

    public record BootstrapResponse(String issuer,
                                    List<OidcClientService.ClientView> apps,
                                    List<AdminConsoleService.UserRow> users,
                                    List<AdminConsoleService.GroupRow> groups) {
    }
}
