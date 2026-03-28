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
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * REST-controller for autentisering og brukerhåndtering.
 *
 * <p>Denne kontrolleren eksponerer API-endepunkter for:
 * <ul>
 *     <li>Registrering av nye brukere</li>
 *     <li>E-postverifisering</li>
 *     <li>Innlogging og utlogging (JWT-basert)</li>
 *     <li>Henting av innlogget bruker</li>
 *     <li>Resending av verifiseringsmail</li>
 *     <li>Glemt passord (reset flow)</li>
 * </ul>
 *
 * <p>Autentisering er basert på JWT-token lagret i HTTP-only cookies
 * for økt sikkerhet.
 *
 * <p>Sikkerhetstiltak inkluderer:
 * <ul>
 *     <li>Passord hashing (BCrypt)</li>
 *     <li>Token-baserte verifiseringslenker</li>
 *     <li>Utløpstid på tokens</li>
 *     <li>Beskyttelse mot bruker-eksponering (generic responses)</li>
 * </ul>
 *
 * <p>Alle sensitive operasjoner validerer input og returnerer
 * standardiserte feilmeldinger.
 *
 * @author Anday
 * @version 1.0
 */
@Tag(name = "Authentication", description = "Authentication, verification, and password reset endpoints")
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

    /**
     * Oppretter en ny {@code AuthController}.
     *
     * @param userRepository repository for brukeroppslag og lagring
     * @param passwordEncoder komponent for hashing og validering av passord
     * @param jwtService tjeneste for generering av JWT-token
     * @param tokenRepo repository for verifiseringstokens
     * @param passwordResetRepo repository for passordreset-tokens
     * @param mailService tjeneste for utsending av e-post
     * @param authService tjeneste som håndterer autentiseringsrelatert forretningslogikk
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
     * Request-modell for autentiseringsrelaterte forespørsler som
     * registrering og innlogging.
     *
     * @param email brukerens e-postadresse
     * @param password brukerens passord i klartekst
     */
    public record AuthRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    /**
     * Normaliserer en e-postadresse ved å gjøre den til små bokstaver
     * og fjerne ledende og avsluttende mellomrom.
     *
     * @param email e-postadressen som skal normaliseres
     * @return normalisert e-postadresse, eller tom streng dersom input er {@code null}
     */
    private String normEmail(String email) {
        return email == null ? "" : email.toLowerCase().trim();
    }

    /**
     * Validerer om et passord oppfyller styrkekravene.
     * <p>
     * Et gyldig passord må inneholde minst 8 tegn, minst én liten bokstav,
     * minst én stor bokstav og minst ett tall.
     *
     * @param password passordet som skal valideres
     * @return {@code true} dersom passordet oppfyller kravene, ellers {@code false}
     */
    private boolean isStrongPassword(String password) {
        if (password == null) return false;
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    }

    /**
     * Oppretter og legger til en sikker sesjonscookie med JWT-token.
     * <p>
     * Cookien settes som {@code httpOnly} og får forskjellig
     * {@code SameSite}-policy avhengig av om forespørselen er secure.
     *
     * @param request HTTP-forespørselen som brukes for å avgjøre secure/same-site
     * @param response HTTP-responsen som cookie-headeren legges til på
     * @param token JWT-tokenet som skal lagres i cookien
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
     * Tømmer den eksisterende sesjonscookien.
     *
     * @param request HTTP-forespørselen som brukes for å avgjøre secure/same-site
     * @param response HTTP-responsen som cookie-headeren legges til på
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
     * <p>
     * Metoden bruker først miljøvariabelen {@code APP_BASE_URL} dersom den
     * finnes. Hvis ikke, bygges URL-en fra informasjon i HTTP-forespørselen.
     *
     * @param request HTTP-forespørselen som kan brukes til å bygge basis-URL
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
     * <p>
     * E-post normaliseres før lagring. Passordet må oppfylle fastsatte
     * styrkekrav. Dersom brukeren opprettes vellykket, sendes en
     * verifiseringsmail med lenke for aktivering av kontoen.
     *
     * @param req request-body med e-post og passord
     * @param request HTTP-forespørselen brukt til å bygge verifiseringslenke
     * @return respons som bekrefter at bruker er opprettet, eller feilmelding
     * dersom input er ugyldig eller brukeren allerede finnes
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
     * Verifiserer en bruker ved hjelp av et verifiseringstoken.
     * <p>
     * Dersom tokenet er gyldig, ikke utløpt og ikke allerede brukt,
     * aktiveres brukerkontoen og tokenet markeres som brukt.
     *
     * @param token tokenet som ble sendt til brukerens e-postadresse
     * @return respons som bekrefter verifisering, eller feilmelding dersom
     * tokenet er ugyldig, brukt eller utløpt
     */
    @Operation(summary = "Verify account with email token")
    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam String token) {
        VerificationToken vt = tokenRepo.findById(token).orElse(null);
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

    /**
     * Logger inn en aktivert bruker og oppretter en sesjonscookie med JWT.
     *
     * @param req request-body med e-post og passord
     * @param request HTTP-forespørselen
     * @param response HTTP-responsen hvor sesjonscookien legges til
     * @return respons med brukerens e-post ved vellykket innlogging,
     * eller feilmelding dersom legitimasjon er feil eller konto ikke er aktivert
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
     * Henter informasjon om den innloggede brukeren.
     *
     * @param auth autentiseringsobjektet for gjeldende bruker
     * @return respons med e-postadresse for innlogget bruker, eller
     * {@code 401 Unauthorized} dersom brukeren ikke er autentisert
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
     * Logger ut brukeren ved å tømme sesjonscookien.
     *
     * @param request HTTP-forespørselen
     * @param response HTTP-responsen hvor tømming av cookien legges til
     * @return respons som bekrefter vellykket utlogging
     */
    @Operation(summary = "Logout current user")
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        clearSessionCookie(request, response);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /**
     * Oppretter og sender en ny verifiseringslenke for en ikke-aktivert bruker.
     * <p>
     * Endepunktet returnerer alltid en generell OK-respons for å unngå å
     * avsløre om en e-postadresse finnes i systemet.
     *
     * @param body request-body som forventes å inneholde nøkkelen {@code email}
     * @param request HTTP-forespørselen brukt til å bygge verifiseringslenke
     * @return generell OK-respons, eventuelt med informasjon om at ny
     * bekreftelsesmail er sendt
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
     * Starter passordtilbakestilling for en bruker.
     * <p>
     * Dersom e-postadressen finnes, opprettes et reset-token og det sendes
     * en e-post med lenke for å sette nytt passord. Endepunktet returnerer
     * en generell OK-respons for å unngå å avsløre om e-postadressen finnes.
     *
     * @param body request-body som forventes å inneholde nøkkelen {@code email}
     * @param request HTTP-forespørselen brukt til å bygge reset-lenke
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
     * Fullfører passordtilbakestilling ved hjelp av et gyldig reset-token.
     * <p>
     * Tokenet må eksistere, være ubrukt og ikke utløpt. Dersom valideringen
     * lykkes, oppdateres brukerens passord og tokenet markeres som brukt.
     *
     * @param body request-body som forventes å inneholde {@code token} og nytt passord
     * @return respons som bekrefter at passordet er oppdatert, eller feilmelding
     * dersom token eller passord ikke er gyldig
     */
    @Operation(summary = "Reset password using token")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> body) {
        String token = body.getOrDefault("token", "").trim();
        String newPassword = body.getOrDefault("newPassword", "").trim();
        if (newPassword.isBlank()) newPassword = body.getOrDefault("password", "").trim();

        if (token.isBlank()) return ResponseEntity.badRequest().body(Map.of("error", "Token mangler"));

        if (!isStrongPassword(newPassword)) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Passordkrav: minst 8 tegn, stor/liten bokstav og tall"
            ));
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
        } catch (Exception ignored) {
        }

        return ResponseEntity.ok(Map.of("ok", true, "message", "Passord er oppdatert."));
    }
}