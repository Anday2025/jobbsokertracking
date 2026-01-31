package com.example.jobtracker.controller;

import com.example.jobtracker.model.User;
import com.example.jobtracker.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserRepository userRepository;

    public AccountController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    private void clearSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = request.isSecure();
        String sameSite = secure ? "None" : "Lax";

        ResponseCookie cookie = ResponseCookie.from("SESSION", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ZERO)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    @DeleteMapping("/me")
    public ResponseEntity<?> deleteMe(Authentication auth,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String email = auth.getName().toLowerCase().trim();
        User u = userRepository.findByEmail(email).orElse(null);
        if (u == null) return ResponseEntity.status(404).body(Map.of("error", "User not found"));

        userRepository.delete(u); // CASCADE tar token/apps
        clearSessionCookie(request, response);

        return ResponseEntity.ok(Map.of("ok", true));
    }
}
