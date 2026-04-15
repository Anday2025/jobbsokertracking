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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.servlet.view.RedirectView;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * REST-controller for autentisering og brukerhåndtering.
 *
 * <p>Kontrolleren eksponerer endepunkter for:
 * <ul>
 *     <li>registrering av nye brukere</li>
 *     <li>e-postverifisering</li>
 *     <li>innlogging og utlogging</li>
 *     <li>henting av innlogget bruker</li>
 *     <li>sending av ny verifiseringsmail</li>
 *     <li>glemt passord og tilbakestilling av passord</li>
 * </ul>
 *
 * <p>Autentisering er basert på JWT-token lagret i HTTP-only cookie.
 * Kontrolleren samarbeider med repositories og tjenester for å validere
 * brukere, opprette og kontrollere tokens, og sende e-post.
 */
@Tag(name = "Authentication", description = "Authentication, verification, and password reset endpoints")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * Repository for oppslag og lagring av brukere.
     */
    private final UserRepository userRepository;

    /**
     * Komponent for hashing og validering av passord.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Tjeneste for generering av JWT-token.
     */
    private final JwtService jwtService;

    /**
     * Repository for verifiseringstokens brukt ved aktivering av konto.
     */
    private final VerificationTokenRepository tokenRepo;

    /**
     * Repository for passordreset-tokens.
     */
    private final PasswordResetTokenRepository passwordResetRepo;

    /**
     * Tjeneste for utsending av e-post.
     */
    private final MailService mailService;

    /**
     * Tjeneste som håndterer autentiseringsrelatert forretningslogikk.
     */
    private final AuthService authService;

    /**
     * Oppretter en ny {@code AuthController}.
     *
     * @param userRepository repository for brukere
     * @param passwordEncoder encoder for hashing og validering av passord
     * @param jwtService tjeneste for generering av JWT
     * @param tokenRepo repository for verifiseringstokens
     * @param passwordResetRepo repository for passordreset-tokens
     * @param mailService tjeneste for utsending av e-post
     * @param authService tjeneste for autentisering og tokenlogikk
     */
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

    /**
     * Request-modell for registrering og innlogging.
     *
     * @param email brukerens e-postadresse
     * @param password brukerens passord i klartekst
     */
    public record AuthRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    /**
     * Normaliserer e-postadresse ved å fjerne mellomrom og gjøre teksten til små bokstaver.
     *
     * @param email e-postadresse som skal normaliseres
     * @return normalisert e-postadresse, eller tom streng dersom input er {@code null}
     */
    private String normEmail(String email) {
        return email == null ? "" : email.toLowerCase().trim();
    }

    /**
     * Validerer om passordet oppfyller styrkekravene.
     *
     * <p>Et gyldig passord må inneholde:
     * <ul>
     *     <li>minst 8 tegn</li>
     *     <li>minst én liten bokstav</li>
     *     <li>minst én stor bokstav</li>
     *     <li>minst ett tall</li>
     * </ul>
     *
     * @param password passord som skal valideres
     * @return {@code true} dersom passordet oppfyller kravene, ellers {@code false}
     */
    private boolean isStrongPassword(String password) {
        if (password == null) return false;
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    }

    /**
     * Oppretter og legger til sesjonscookie med JWT-token i responsen.
     *
     * <p>Cookien settes som {@code httpOnly} og får {@code SameSite=None}
     * ved secure forespørsler, ellers {@code SameSite=Lax}.
     *
     * @param request HTTP-forespørsel brukt for å avgjøre secure-policy
     * @param response HTTP-respons hvor cookie skal legges til
     * @param token JWT-token som skal lagres i cookien
     */
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

    /**
     * Tømmer eksisterende sesjonscookie ved utlogging.
     *
     * @param request HTTP-forespørsel brukt for å avgjøre secure-policy
     * @param response HTTP-respons hvor cookie-header legges til
     */
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

    /**
     * Finner basis-URL for applikasjonen.
     *
     * <p>Metoden bruker først miljøvariabelen {@code APP_BASE_URL}. Hvis den
     * ikke finnes eller er tom, bygges URL-en fra innkommende HTTP-forespørsel.
     *
     * @param request HTTP-forespørsel brukt til å bygge URL dersom miljøvariabel mangler
     * @return basis-URL for applikasjonen
     */
    private String getBaseUrl(HttpServletRequest request) {
        String baseUrl = System.getenv().getOrDefault("APP_BASE_URL", "").trim();
        if (!baseUrl.isBlank()) return baseUrl;

        String scheme = request.isSecure() ? "https" : "http";
        return scheme + "://" + request.getServerName() + ":" + request.getServerPort();
    }

    /**
     * Registrerer en ny bruker og oppretter et verifiseringstoken.
     *
     * <p>Metoden validerer e-post og passord, oppretter en deaktivert bruker
     * via {@link AuthService}, bygger verifiseringslenke og forsøker å sende
     * verifiseringsmail.
     *
     * @param req request-body med e-post og passord
     * @param request HTTP-forespørsel brukt til å bygge verifiseringslenke
     * @return respons med bekreftelse eller feilmelding dersom input er ugyldig
     */
    @Operation(summary = "Register a new user")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRequest req, HttpServletRequest request) {
        String email = normEmail(req.email());
        String password = req.password() == null ? "" : req.password().trim();

        if (email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "E-post mangler"));
        }
        if (!isStrongPassword(password)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Passordkrav: minst 8 tegn, stor/liten bokstav og tall"
            ));
        }

        final AuthService.PendingVerification pending;
        try {
            pending = authService.createPendingUser(email, password);
        } catch (IllegalStateException e) {
            String msg = (e.getMessage() == null) ? "Ugyldig forespørsel" : e.getMessage();
            if (msg.toLowerCase().contains("allerede")) {
                return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", msg));
            }
            return ResponseEntity.badRequest().body(Map.of("error", msg));
        }

        String verifyUrl = getBaseUrl(request) + "/api/auth/verify?token=" + pending.token();

        System.out.println("VERIFY URL: " + verifyUrl);

        try {
            mailService.sendVerificationEmail(pending.email(), verifyUrl);
            return ResponseEntity.ok(Map.of(
                    "ok", true,
                    "message", "Bruker opprettet. Sjekk e-posten din for bekreftelse, også i søppelpost."
            ));
        } catch (Exception e) {
            return ResponseEntity.status(201).body(Map.of(
                    "ok", true,
                    "emailSent", false,
                    "message", "Bruker opprettet, men vi klarte ikke å sende verifiseringsmail. Prøv 'Resend verification'.",
                    "details", e.getMessage()
            ));
        }
    }

    /**
     * Verifiserer brukerkonto basert på token sendt i e-post.
     *
     * <p>Dersom tokenet er gyldig og ikke brukt eller utløpt, aktiveres brukeren,
     * tokenet markeres som brukt, og klienten redirectes til frontend med
     * suksessstatus i query-parameter.
     *
     * @param token verifiseringstoken fra e-postlenken
     * @return redirect til frontend med status for verifisering
     */
    @Transactional
    @Operation(summary = "Verify account with email token")
    @GetMapping("/verify")
    public RedirectView verify(@RequestParam String token) {
        VerificationToken vt = tokenRepo.findById(token).orElse(null);

        if (vt == null) {
            return new RedirectView("/?verified=invalid");
        }

        if (vt.isUsed()) {
            return new RedirectView("/?verified=used");
        }

        if (vt.getExpiresAt().isBefore(Instant.now())) {
            return new RedirectView("/?verified=expired");
        }

        User u = vt.getUser();
        if (u == null) {
            return new RedirectView("/?verified=error");
        }

        u.setEnabled(true);
        userRepository.save(u);

        vt.setUsed(true);
        tokenRepo.save(vt);

        return new RedirectView("/?verified=success");
    }

    /**
     * Logger inn en aktivert bruker og setter JWT i sesjonscookie.
     *
     * <p>Metoden validerer e-post og passord, kontrollerer at kontoen er aktivert,
     * genererer JWT og lagrer tokenet i en HTTP-only cookie.
     *
     * @param req request-body med e-post og passord
     * @param request HTTP-forespørsel
     * @param response HTTP-respons hvor cookie legges til
     * @return respons med brukerens e-post ved vellykket innlogging
     */
    @Operation(summary = "Login and create session cookie")
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

    /**
     * Henter informasjon om innlogget bruker.
     *
     * @param auth autentiseringsobjekt for gjeldende bruker
     * @return respons med e-postadresse til innlogget bruker, eller {@code 401 Unauthorized}
     */
    @Operation(summary = "Get current authenticated user")
    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));
        }
        return ResponseEntity.ok(Map.of("email", auth.getName()));
    }

    /**
     * Logger ut brukeren ved å slette sesjonscookie.
     *
     * @param request HTTP-forespørsel
     * @param response HTTP-respons hvor cookie-nullstilling legges til
     * @return respons som bekrefter utlogging
     */
    @Operation(summary = "Logout current user")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        clearSessionCookie(request, response);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * Oppretter og sender en ny verifiseringsmail til en ikke-aktivert bruker.
     *
     * <p>Metoden returnerer generisk respons for å unngå å avsløre om en e-postadresse
     * finnes i systemet.
     *
     * @param body request-body som forventes å inneholde nøkkelen {@code email}
     * @param request HTTP-forespørsel brukt til å bygge verifiseringslenke
     * @return generell respons, eventuelt med melding om at ny verifiseringsmail er sendt
     */
    @Operation(summary = "Resend verification email")
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = normEmail(body.get("email"));
        if (email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "E-post mangler"));

        String token = authService.createNewVerificationToken(email);

        if (token == null || token.isBlank()) {
            return ResponseEntity.ok(Map.of("ok", true));
        }

        User u = userRepository.findByEmail(email).orElse(null);
        if (u == null) return ResponseEntity.ok(Map.of("ok", true));
        if (u.isEnabled()) return ResponseEntity.ok(Map.of("ok", true));

        String verifyUrl = getBaseUrl(request) + "/api/auth/verify?token=" + token;

        try {
            mailService.sendVerificationEmail(u.getEmail(), verifyUrl);
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


    /**
     * Starter glemt-passord-flyten ved å opprette reset-token og sende e-post.
     *
     * <p>Metoden returnerer generell respons uavhengig av om e-postadressen finnes,
     * for å unngå bruker-eksponering.
     *
     * @param body request-body som forventes å inneholde nøkkelen {@code email}
     * @param request HTTP-forespørsel brukt til å bygge reset-lenke
     * @return respons som bekrefter at forespørselen er behandlet
     */
    @Operation(summary = "Start forgot-password flow")
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String email = normEmail(body.get("email"));
        if (email.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "E-post mangler"));

        String token = authService.createPasswordResetToken(email);

        if (token == null || token.isBlank()) return ResponseEntity.ok(Map.of("ok", true));

        String resetUrl = getBaseUrl(request) + "/?token=" + token;

        try {
            mailService.sendResetPasswordEmail(email, resetUrl);
        } catch (Exception e) {
            return ResponseEntity.status(200).body(Map.of(
                    "ok", true,
                    "emailSent", false,
                    "message", "Hvis e-post finnes, er reset-link sendt (eller prøv igjen senere).",
                    "details", e.getMessage()
            ));
        }

        return ResponseEntity.ok(Map.of(
                "ok", true,
                "message", "Hvis e-post finnes, er reset-link sendt. Sjekk e-posten din."
        ));
    }

    /**
     * Fullfører passordtilbakestilling ved hjelp av gyldig reset-token.
     *
     * <p>Metoden validerer token og nytt passord, oppdaterer brukerens passord,
     * markerer tokenet som brukt og forsøker å sende bekreftelsesmail.
     *
     * @param body request-body som forventes å inneholde token og nytt passord
     * @return respons som bekrefter oppdatert passord, eller feilmelding ved feil
     */
    @Transactional
    @Operation(summary = "Reset password using token")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        try {
            String token = body.getOrDefault("token", "").trim();
            String newPassword = body.getOrDefault("newPassword", "").trim();
            if (newPassword.isBlank()) newPassword = body.getOrDefault("password", "").trim();

            if (token.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token mangler"));
            }

            if (!isStrongPassword(newPassword)) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Passordkrav: minst 8 tegn, stor/liten bokstav og tall"
                ));
            }

            PasswordResetToken prt = passwordResetRepo.findById(token).orElse(null);
            if (prt == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Ugyldig token"));
            }

            if (prt.isUsed()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token er allerede brukt"));
            }

            if (prt.getExpiresAt().isBefore(Instant.now())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Token er utløpt"));
            }

            User u = prt.getUser();
            if (u == null) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Ingen bruker er knyttet til reset-tokenet"));
            }

            u.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(u);

            prt.setUsed(true);
            passwordResetRepo.save(prt);

            try {
                mailService.sendPasswordChangedEmail(u.getEmail());
            } catch (Exception ignored) {
            }

            return ResponseEntity.ok(Map.of("ok", true, "message", "Passord er oppdatert."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Passordreset feilet",
                    "details", e.getMessage()
            ));
        }
    }
}