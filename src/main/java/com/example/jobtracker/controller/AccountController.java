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

/**
 * REST-controller for kontooperasjoner for autentiserte brukere.
 * <p>
 * Klassen håndterer handlinger relatert til brukerens egen konto,
 * som for eksempel sletting av den innloggede brukeren.
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final UserRepository userRepository;

    /**
     * Oppretter en ny {@code AccountController}.
     *
     * @param userRepository repository for oppslag og sletting av brukere
     */
    public AccountController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Tømmer sesjonscookien for den aktuelle klienten.
     * <p>
     * Metoden oppretter en ny {@code SESSION}-cookie med tom verdi
     * og utløp umiddelbart, slik at eksisterende innloggingssesjon
     * fjernes i nettleseren.
     *
     * @param request HTTP-forespørselen som brukes for å avgjøre om cookien skal være secure
     * @param response HTTP-responsen som den nye cookie-headeren legges til på
     */
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

    /**
     * Sletter den innloggede brukerens konto.
     * <p>
     * Endepunktet henter brukeren fra autentiseringskonteksten, finner
     * tilhørende bruker i databasen, sletter brukeren og tømmer deretter
     * sesjonscookien. Dersom brukeren ikke er autentisert, returneres
     * {@code 401 Unauthorized}. Dersom brukeren ikke finnes i databasen,
     * returneres {@code 404 Not Found}.
     *
     * @param auth autentiseringsobjektet for gjeldende bruker
     * @param request HTTP-forespørselen
     * @param response HTTP-responsen
     * @return en respons som bekrefter sletting, eller feilmelding dersom
     * brukeren ikke er autentisert eller ikke finnes
     */
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