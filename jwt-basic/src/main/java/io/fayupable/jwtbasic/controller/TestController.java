package io.fayupable.jwtbasic.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test Controller
 *
 * Simple endpoint to verify JWT authentication is working.
 * This endpoint requires authentication.
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    /**
     * Test endpoint - requires JWT token
     *
     * Returns greeting with authenticated user's email
     */
    @GetMapping("/hello")
    public String hello() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assert auth != null;
        return "Hello, " + auth.getName() + "! Your JWT token is working. Roles: " + auth.getAuthorities();
    }
}
