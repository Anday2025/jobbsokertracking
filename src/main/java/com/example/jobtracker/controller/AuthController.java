package com.example.jobtracker.controller;

import com.example.jobtracker.model.PasswordResetToken;
import com.example.jobtracker.model.User;
import com.example.jobtracker.model.VerificationToken;
import com.example.jobtracker.repository.PasswordResetTokenRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.repository.VerificationTokenRepository;
import com.example.jobtracker.security.JwtService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final VerificationTokenRepository tokenRepo;
    private final MailService mailService;
    private final PasswordResetTokenRepository passwordResetRepo;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          VerificationTokenRepository tokenRepo,
                          PasswordResetTokenRepository passwordResetRepo,
                          MailService mailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.tokenRepo = tokenRepo;
        this.passwordResetRepo = passwordResetRepo;
        this.mailService = mailService;
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
        boolean secure = request.isSecure(); // Render => ofte true
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
    @Transactional
    public ResponseEntity<?> register(@RequestBody AuthRequest req, HttpServletRequest request) {
        String email = normEmail(req.email());
        String password = req.password() == null ? "" : req.password().trim();

        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "E-post mangler"));
        }

        if (userRepository.existsByEmail(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "E-post er allerede registrert"));
        }

        if (!isStrongPassword(password)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Passordkrav: minst 8 tegn, stor/liten bokstav og tall"));
        }

        User u = new User(email, passwordEncoder.encode(password));
        u.setEnabled(false);
        userRepository.save(u);

        // token gyldig 24 timer
        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofHours(24));
        VerificationToken vt = new VerificationToken(token, u, expiresAt);
        tokenRepo.save(vt);

        String verifyUrl = getBaseUrl(request) + "/api/auth/verify?token=" + token;

        // ✅ Viktig: Hvis Mailgun feiler, returner tydelig feilmelding i JSON
        try {
            mailService.sendVerificationEmail(u.getEmail(), verifyUrl);
        } catch (Exception e) {
            // @Transactional vil rulle tilbake user + token (bra!)
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Kunne ikke sende verifiseringsmail. Sjekk Mailgun settings/region.",
                    "details", e.getMessage()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Bruker opprettet. Sjekk e-posten din for bekreftelse."
        ));
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
    @Transactional
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = normEmail(body.get("email"));
        if (email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "E-post mangler"));

        User u = userRepository.findByEmail(email).orElse(null);

        // Returner OK uansett (ikke avslør om epost finnes)
        if (u == null) return ResponseEntity.ok(Map.of("ok", true));
        if (u.isEnabled()) return ResponseEntity.ok(Map.of("ok", true, "message", "Brukeren er allerede aktivert"));

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofHours(24));
        VerificationToken vt = new VerificationToken(token, u, expiresAt);
        tokenRepo.save(vt);

        String verifyUrl = getBaseUrl(request) + "/api/auth/verify?token=" + token;

        try {
            mailService.sendVerificationEmail(u.getEmail(), verifyUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Kunne ikke sende verifiseringsmail.",
                    "details", e.getMessage()
            ));
        }

        return ResponseEntity.ok(Map.of("ok", true, "message", "Ny bekreftelse sendt på e-post."));
    }

    // ---------------- FORGOT PASSWORD ----------------
    @PostMapping("/forgot-password")
    @Transactional
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = normEmail(body.get("email"));
        if (email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "E-post mangler"));

        User u = userRepository.findByEmail(email).orElse(null);

        // Returner OK uansett (ikke avslør om epost finnes)
        if (u == null) return ResponseEntity.ok(Map.of("ok", true));

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(30));

        PasswordResetToken prt = new PasswordResetToken(token, u, expiresAt);
        passwordResetRepo.save(prt);

        // ✅ frontend åpner reset modal automatisk med ?token=
        String resetUrl = getBaseUrl(request) + "/?token=" + token;

        try {
            mailService.sendResetPasswordEmail(u.getEmail(), resetUrl);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Kunne ikke sende reset-mail.",
                    "details", e.getMessage()
            ));
        }

        return ResponseEntity.ok(Map.of("ok", true, "message", "Hvis e-post finnes, er reset-link sendt."));
    }

    // ---------------- RESET PASSWORD ----------------
    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("token", "").trim();

        // ✅ frontend kan sende "newPassword"
        // vi støtter også "password"
        String newPassword = body.getOrDefault("newPassword", "").trim();
        if (newPassword.isBlank()) {
            newPassword = body.getOrDefault("password", "").trim();
        }

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

        // ✅ Send email confirmation (men ikke la det ødelegge reset)
        try {
            mailService.sendPasswordChangedEmail(u.getEmail());
        } catch (Exception ignored) {
            // passord er allerede endret, så vi returnerer OK uansett
        }

        return ResponseEntity.ok(Map.of("ok", true, "message", "Passord er oppdatert."));
    }
}
