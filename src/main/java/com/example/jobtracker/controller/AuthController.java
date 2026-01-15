package com.example.jobtracker.controller;

import com.example.jobtracker.model.User;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    // ---------- DTO ----------
    public record AuthRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    // ---------- REGISTER (lager bruker + auto-login cookie) ----------
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req,
                                      HttpServletRequest request,
                                      HttpServletResponse response) {

        String email = req.email().toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("E-post er allerede registrert");
        }
        if (req.password() == null || req.password().length() < 6) {
            return ResponseEntity.badRequest().body("Passord må være minst 6 tegn");
        }

        User u = new User(email, passwordEncoder.encode(req.password()));
        userRepository.save(u);

        // Auto-login etter register (brukeropplevelse som "ekte" nettsider)
        setSessionCookie(request, response, jwtService.generateToken(u.getEmail()));

        return ResponseEntity.ok(Map.of("email", u.getEmail()));
    }

    // ---------- LOGIN (setter cookie) ----------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {

        String email = req.email().toLowerCase().trim();

        User u = userRepository.findByEmail(email).orElse(null);

        if (u == null || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Feil e-post eller passord");
        }

        setSessionCookie(request, response, jwtService.generateToken(u.getEmail()));

        return ResponseEntity.ok(Map.of("email", u.getEmail()));
    }

    // ---------- ME ----------
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized");
        }
        return ResponseEntity.ok(Map.of("email", auth.getName()));
    }

    // ---------- LOGOUT (sletter cookie) ----------
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        clearSessionCookie(request, response);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ==================== COOKIE HELPERS ====================

    private void setSessionCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        boolean secure = request.isSecure(); // true på Render (https), false lokalt (http)

        // SameSite=None krever Secure=true. Lokalt må vi bruke Lax.
        String sameSite = secure ? "None" : "Lax";

        ResponseCookie cookie = ResponseCookie.from("SESSION", token)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }

    private void clearSessionCookie(HttpServletRequest request, HttpServletResponse response) {
        boolean secure = request.isSecure();
        String sameSite = secure ? "None" : "Lax";

        ResponseCookie cookie = ResponseCookie.from("SESSION", "")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(0)
                .build();

        response.addHeader("Set-Cookie", cookie.toString());
    }
}
