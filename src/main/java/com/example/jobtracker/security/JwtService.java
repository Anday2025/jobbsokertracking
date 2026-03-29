package com.example.jobtracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Tjeneste for opprettelse, lesing og validering av JWT-token.
 * <p>
 * Klassen bruker en hemmelig nøkkel fra miljøvariabelen {@code JWT_SECRET}
 * for å signere og validere token.
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMs = 1000L * 60 * 60 * 24 * 7; // 7 dager

    /**
     * Oppretter en ny JWT-tjeneste og initialiserer signeringsnøkkelen
     * fra miljøvariabelen {@code JWT_SECRET}.
     * <p>
     * Nøkkelen må være minst 32 tegn lang.
     *
     * @throws IllegalStateException dersom {@code JWT_SECRET} er for kort
     */
    public JwtService() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET må være satt og minst 32 tegn");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Genererer et nytt JWT-token for en bruker basert på e-postadresse.
     *
     * @param email brukerens e-postadresse
     * @return signert JWT-token
     */
    public String generateToken(String email) {
        return buildToken(email);
    }

    /**
     * Alias for token-generering brukt av eventuell eldre kode.
     *
     * @param email brukerens e-postadresse
     * @return signert JWT-token
     */
    public String createToken(String email) {
        return buildToken(email);
    }

    /**
     * Bygger et nytt JWT-token med subject, issued-at og expiration.
     *
     * @param email brukerens e-postadresse
     * @return signert JWT-token
     */
    private String buildToken(String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    /**
     * Henter brukernavn/e-postadresse fra et token.
     *
     * @param token JWT-token
     * @return subject fra tokenet
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Validerer at tokenet tilhører riktig bruker og ikke er utløpt.
     *
     * @param token JWT-token
     * @param userDetails brukerdata som tokenet skal matches mot
     * @return {@code true} dersom tokenet er gyldig, ellers {@code false}
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        String email = extractUsername(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Sjekker om et token er utløpt.
     *
     * @param token JWT-token
     * @return {@code true} dersom tokenet er utløpt, ellers {@code false}
     */
    private boolean isTokenExpired(String token) {
        Date exp = extractAllClaims(token).getExpiration();
        return exp.before(new Date());
    }

    /**
     * Leser alle claims fra et signert JWT-token.
     *
     * @param token JWT-token
     * @return claims fra tokenet
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}