package com.melina.notes.controller;

import com.melina.notes.exception.DemoCapacityExceededException;
import com.melina.notes.service.DemoAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class DemoController {

    private final DemoAccountService demoAccountService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/demo-login")
    public String demoLogin(HttpServletRequest request, HttpServletResponse response) {
        DemoAccountService.DemoCredentials credentials;
        try {
            credentials = demoAccountService.createDemoAccount();
        } catch (DemoCapacityExceededException exception) {
            log.warn("Demo login rejected because capacity was reached");
            return "redirect:/login?demoUnavailable=true";
        }

        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            credentials.email(), credentials.password())
            );

            if (request.getSession(false) != null) {
                request.changeSessionId();
            }

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            securityContextRepository.saveContext(context, request, response);
            return "redirect:/notes";
        } catch (RuntimeException exception) {
            demoAccountService.deleteDemoAccount(credentials.userId());
            throw exception;
        }
    }
}
