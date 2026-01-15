/*package com.example.jobtracker.web;

import com.example.jobtracker.model.User;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.security.JwtService;
//import jakarta.validation.constraints.Email;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    record AuthRequest(String email, String password) {}
    record AuthResponse(String token) {}

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req) {
        if (req.email() == null || req.password() == null || req.password().length() < 6) {
            return ResponseEntity.badRequest().body("Ugyldig email/passord (min 6 tegn).");
        }
        String email = req.email().trim().toLowerCase();

        if (users.existsByEmail(email)) {
            return ResponseEntity.badRequest().body("Bruker finnes allerede.");
        }

        User u = new User(email, encoder.encode(req.password()));
        users.save(u);

        return ResponseEntity.ok(new AuthResponse(jwt.createToken(email)));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req) {
        if (req.email() == null || req.password() == null) {
            return ResponseEntity.badRequest().body("Ugyldig email/passord.");
        }
        String email = req.email().trim().toLowerCase();

        var uOpt = users.findByEmail(email);
        if (uOpt.isEmpty() || !encoder.matches(req.password(), uOpt.get().getPasswordHash())) {
            return ResponseEntity.status(401).body("Feil email eller passord.");
        }

        return ResponseEntity.ok(new AuthResponse(jwt.createToken(email)));
    }
}
*/