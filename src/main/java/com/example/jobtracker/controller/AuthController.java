package com.example.jobtracker.controller;

import com.example.jobtracker.model.PasswordResetToken;
import com.example.jobtracker.model.User;
import com.example.jobtracker.model.VerificationToken;
import com.example.jobtracker.repository.PasswordResetTokenRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.repository.VerificationTokenRepository;
import com.example.jobtracker.security.JwtService;
import com.example.jobtracker.service.AuthService;
import com.example.jobtracker.service.MailService;
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
import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationTokenRepository tokenRepo;
    private final PasswordResetTokenRepository passwordResetRepo;
    private final MailService mailService;
    private final AuthService authService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          VerificationTokenRepository tokenRepo,
                          PasswordResetTokenRepository passwordResetRepo,
                          MailService mailService,
                          AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRepo = tokenRepo;
        this.passwordResetRepo = passwordResetRepo;
        this.mailService = mailService;
        this.authService = authService;
    }

    public record AuthRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    private String normEmail(String email) {
        return email == null ? "" : email.toLowerCase().trim();
    }

    // Minst 8 tegn, minst én stor bokstav, én liten bokstav og ett tall
    private boolean isStrongPassword(String password) {
        if (password == null) return false;
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    }

    private void setSessionCookie(HttpServletRequest request, HttpServletResponse response, String token) {
        boolean secure = request.isSecure();
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

    private String getBaseUrl(HttpServletRequest request) {
        String baseUrl = System.getenv().getOrDefault("APP_BASE_URL", "").trim();
        if (!baseUrl.isBlank()) return baseUrl;

        String scheme = request.isSecure() ? "https" : "http";
        return scheme + "://" + request.getServerName() + ":" + request.getServerPort();
    }

    // ---------------- REGISTER ----------------
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req, HttpServletRequest request) {
        String email = normEmail(req.email());
        String password = req.password() == null ? "" : req.password().trim();

        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "E-post mangler"));
        }
        if (!isStrongPassword(password)) {
            return ResponseEntity.badRequest().body(Map.of("error",
                    "Passordkrav: minst 8 tegn, stor/liten bokstav og tall"));
        }

        final AuthService.PendingVerification pending;
        try {
            // ✅ DB først (Transactional inni AuthService)
            pending = authService.createPendingUser(email, password);
        } catch (IllegalStateException e) {
            // eksisterer / mangler epost
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("allerede")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        String verifyUrl = getBaseUrl(request) + "/api/auth/verify?token=" + pending.token();

        // Debug: Render -> Logs
        System.out.println("VERIFY URL: " + verifyUrl);

        // ✅ Mail etterpå (ingen rollback)
        try {
            mailService.sendVerificationEmail(pending.email(), verifyUrl);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", "Bruker opprettet. Sjekk e-posten din for bekreftelse, også i søppelpost."
            ));
        } catch (Exception e) {
            // Bruker er opprettet, men epost feilet
            return ResponseEntity.status(201).body(Map.of(
                    "ok", true,
                    "emailSent", false,
                    "message", "Bruker opprettet, men vi klarte ikke å sende verifiseringsmail. Prøv 'Resend verification'.",
                    "details", e.getMessage()
            ));
        }
    }

    // ---------------- VERIFY ----------------
    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        VerificationToken vt = tokenRepo.findByToken(token).orElse(null);
        if (vt == null) return ResponseEntity.badRequest().body(Map.of("error", "Ugyldig token"));

        if (vt.isUsed()) return ResponseEntity.badRequest().body(Map.of("error", "Token er allerede brukt"));
        if (vt.getExpiresAt().isBefore(Instant.now())) return ResponseEntity.badRequest().body(Map.of("error", "Token er utløpt"));

        User u = vt.getUser();
        u.setEnabled(true);
        userRepository.save(u);

        vt.setUsed(true);
        tokenRepo.save(vt);

        return ResponseEntity.ok(Map.of("ok", true, "message", "Bruker aktivert. Du kan logge inn nå."));
    }

    // ---------------- LOGIN ----------------
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest req,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        String email = normEmail(req.email());
        String password = req.password() == null ? "" : req.password().trim();

        User u = userRepository.findByEmail(email).orElse(null);

        if (u == null || !passwordEncoder.matches(password, u.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Feil e-post eller passord"));
        }

        if (!u.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Du må bekrefte e-posten din før du kan logge inn."));
        }

        String token = jwtService.generateToken(u.getEmail());
        setSessionCookie(request, response, token);

        return ResponseEntity.ok(Map.of("email", u.getEmail()));
    }

    // ---------------- ME ----------------
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        return ResponseEntity.ok(Map.of("email", auth.getName()));
    }

    // ---------------- LOGOUT ----------------
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        clearSessionCookie(request, response);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    // ---------------- RESEND VERIFICATION ----------------
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = normEmail(body.get("email"));
        if (email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "E-post mangler"));

        String token = authService.createNewVerificationToken(email);

        // Returner OK uansett (ikke avslør)
        if (token.isBlank()) return ResponseEntity.ok(Map.of("ok", true));

        String verifyUrl = getBaseUrl(request) + "/api/auth/verify?token=" + token;

        try {
            mailService.sendVerificationEmail(email, verifyUrl);
            return ResponseEntity.ok(Map.of("ok", true, "message", "Ny bekreftelse sendt på e-post."));
        } catch (Exception e) {
            return ResponseEntity.status(200).body(Map.of(
                    "ok", true,
                    "emailSent", false,
                    "message", "Token ble laget, men epost kunne ikke sendes. Prøv igjen senere.",
                    "details", e.getMessage()
            ));
        }
    }

    // ---------------- FORGOT PASSWORD ----------------
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = normEmail(body.get("email"));
        if (email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "E-post mangler"));

        String token = authService.createPasswordResetToken(email);

        // Returner OK uansett (ikke avslør)
        if (token.isBlank()) return ResponseEntity.ok(Map.of("ok", true));

        String resetUrl = getBaseUrl(request) + "/?token=" + token;

        try {
            mailService.sendResetPasswordEmail(email, resetUrl);
        } catch (Exception e) {
            // Ikke avslør, men gi “soft fail”
            return ResponseEntity.status(200).body(Map.of(
                    "ok", true,
                    "emailSent", false,
                    "message", "Hvis e-post finnes, er reset-link sendt (eller prøv igjen senere).",
                    "details", e.getMessage()
            ));
        }

        return ResponseEntity.ok(Map.of("ok", true, "message", "Hvis e-post finnes, er reset-link sendt, Sjekk e-posten din for bekreftelse, også i søppelpost."));
    }

    // ---------------- RESET PASSWORD ----------------
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("token", "").trim();
        String newPassword = body.getOrDefault("newPassword", "").trim();
        if (newPassword.isBlank()) newPassword = body.getOrDefault("password", "").trim();

        if (token.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Token mangler"));

        if (!isStrongPassword(newPassword)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Passordkrav: minst 8 tegn, stor/liten bokstav og tall"));
        }

        PasswordResetToken prt = passwordResetRepo.findById(token).orElse(null);
        if (prt == null) return ResponseEntity.badRequest().body(Map.of("error", "Ugyldig token"));
        if (prt.isUsed()) return ResponseEntity.badRequest().body(Map.of("error", "Token er allerede brukt"));
        if (prt.getExpiresAt().isBefore(Instant.now())) return ResponseEntity.badRequest().body(Map.of("error", "Token er utløpt"));

        User u = prt.getUser();
        u.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(u);

        prt.setUsed(true);
        passwordResetRepo.save(prt);

        try {
            mailService.sendPasswordChangedEmail(u.getEmail());
        } catch (Exception ignored) { }

        return ResponseEntity.ok(Map.of("ok", true, "message", "Passord er oppdatert."));
    }
}
