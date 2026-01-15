package com.example.jobtracker.auth;

import com.example.jobtracker.model.User;
import com.example.jobtracker.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ===== DTOs =====
    public record LoginRequest(String email, String password) {}
    public record RegisterRequest(String email, String password) {}
    public record MeResponse(String email) {}

    // ===== REGISTER =====
    @PostMapping("/register")
    public MeResponse register(@RequestBody RegisterRequest req) {

        if (userRepository.existsByEmail(req.email())) {
            throw new RuntimeException("E-post er allerede i bruk");
        }

        String hash = passwordEncoder.encode(req.password());
        User user = new User(req.email(), hash);
        userRepository.save(user);

        return new MeResponse(user.getEmail());
    }

    // ===== LOGIN =====
    @PostMapping("/login")
    public MeResponse login(@RequestBody LoginRequest req, HttpServletRequest request) {

        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        SecurityContextHolder.getContext().setAuthentication(auth);

        // ⭐ Viktig: opprett session -> JSESSIONID cookie
        request.getSession(true);

        return new MeResponse(req.email());
    }

    // ===== LOGOUT =====
    @PostMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        request.logout(); // invalidates session
        response.setStatus(204);
    }

    // ===== ME =====
    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        return new MeResponse(authentication.getName());
    }
}
