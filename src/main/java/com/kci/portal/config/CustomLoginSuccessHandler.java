package com.kci.portal.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {

        String redirectUrl = "/dashboard";

        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if (auth.getAuthority().equals("ROLE_ADMIN")) {
                redirectUrl = "/admin/dashboard";
                break;
            } else if (auth.getAuthority().equals("ROLE_TUTOR")) {
                redirectUrl = "/tutor/dashboard";
                break;
            } else if (auth.getAuthority().equals("ROLE_STUDENT") || auth.getAuthority().equals("ROLE_USER")) {
                redirectUrl = "/dashboard";
                break;
            }
        }

        response.sendRedirect(redirectUrl);
    }
}