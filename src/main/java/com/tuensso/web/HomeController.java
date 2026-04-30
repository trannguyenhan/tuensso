package com.tuensso.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/login", "/sso-login", "/sso-logout", "/account", "/dashboard",
                 "/admin/login", "/admin/dashboard", "/admin/docs",
                 "/admin/apps", "/admin/apps/{id}",
                 "/admin/users", "/admin/users/{id}",
                 "/admin/groups", "/admin/groups/{id}",
                 "/admin/integration",
                 "/admin/roles", "/admin/roles/{id}", "/admin/sessions", "/admin/audit"})
    public String spa() {
        return "forward:/index.html";
    }
}
