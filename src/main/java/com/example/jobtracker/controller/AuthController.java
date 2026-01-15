package com.example.jobtracker.controller;

import com.example.jobtracker.model.User;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.security.JwtService;
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

    // ---------- REGISTER ----------
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("E-post er allerede registrert");
        }
        if (req.password().length() < 6) {
            return ResponseEntity.badRequest().body("Passord må være minst 6 tegn");
        }

        User u = new User(req.email().toLowerCase().trim(), passwordEncoder.encode(req.password()));
        userRepository.save(u);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ---------- LOGIN (setter cookie) ----------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req, HttpServletResponse res) {
        User u = userRepository.findByEmail(req.email().toLowerCase().trim())
                .orElse(null);

        if (u == null || !passwordEncoder.matches(req.password(), u.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Feil e-post eller passord");
        }

        String token = jwtService.generateToken(u.getEmail()); // eller generateToken(UserDetails)

        // Render = HTTPS => Secure=true. SameSite=None for cookie i moderne browsere når det er "cross-site".
        // (Her er frontend og backend på samme domene, så Lax kunne også funket, men None er tryggest i praksis.)
        ResponseCookie cookie = ResponseCookie.from("SESSION", token)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofDays(7))
                .build();

        res.addHeader("Set-Cookie", cookie.toString());

        // Returner gjerne litt info (ikke token)
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
    public ResponseEntity<?> logout(HttpServletResponse res) {
        ResponseCookie cookie = ResponseCookie.from("SESSION", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();

        res.addHeader("Set-Cookie", cookie.toString());
        return ResponseEntity.ok(Map.of("ok", true));
    }
}
