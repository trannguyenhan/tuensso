package com.tuensso.web;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(CustomErrorController.class);

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int code = status != null ? Integer.parseInt(status.toString()) : 500;

        String uri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
        String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

        // Log the real error context so OAuth/OIDC request validation failures are traceable.
        if (code >= 400) {
            String clientId = request.getParameter("client_id");
            String redirectUri = request.getParameter("redirect_uri");
            String responseType = request.getParameter("response_type");
            if (exception != null) {
                log.warn("HTTP {} at uri={} client_id={} redirect_uri={} response_type={} message={}",
                        code, uri, clientId, redirectUri, responseType, message, exception);
            } else {
                log.warn("HTTP {} at uri={} client_id={} redirect_uri={} response_type={} message={}",
                        code, uri, clientId, redirectUri, responseType, message);
            }
        }

        model.addAttribute("status", code);
        model.addAttribute("is404", code == 404);
        return "error";
    }
}
