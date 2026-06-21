package com.melina.notes.security;

import com.melina.notes.service.DemoAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoLogoutHandler implements LogoutHandler {

    private final DemoAccountService demoAccountService;

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails user)) {
            return;
        }

        try {
            demoAccountService.deleteDemoAccount(user.getId());
        } catch (RuntimeException exception) {
            log.warn("Could not delete demo account during logout - userId={}", user.getId(), exception);
        }
    }
}
