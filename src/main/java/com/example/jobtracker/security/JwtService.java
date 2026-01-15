package com.example.jobtracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMs = 1000L * 60 * 60 * 24; // 24 timer

    public JwtService(@Value("${JWT_SECRET:CHANGE_ME_CHANGE_ME_CHANGE_ME_32CHARS!}") String secret) {
        if (secret == null) secret = "";
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET må være minst 32 tegn (HS256).");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // ====== LAGE TOKEN ======
    public String generateToken(String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + ttlMs);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(exp)
                .signWith(key)
                .compact();
    }

    // (valgfritt) bakoverkompatibilitet om du fortsatt kaller createToken noe sted
    public String createToken(String email) {
        return generateToken(email);
    }

    // ====== HENTE EMAIL / USERNAME FRA TOKEN ======
    public String extractUsername(String token) {
        return getAllClaims(token).getSubject();
    }

    // ====== VALIDERING ======
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username != null
                    && username.equals(userDetails.getUsername())
                    && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        Date exp = getAllClaims(token).getExpiration();
        return exp == null || exp.before(new Date());
    }

    private Claims getAllClaims(String token) {
        if (token == null) throw new IllegalArgumentException("Token mangler");

        token = token.trim();
        // hvis noen sender Bearer selv om du bruker cookie
        if (token.startsWith("Bearer ")) {
            token = token.substring("Bearer ".length()).trim();
        }

        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException e) {
            throw new IllegalArgumentException("Ugyldig eller utløpt token");
        }
    }

    // Hvis du har brukt validateAndGetEmail i andre steder, behold den også
    public String validateAndGetEmail(String token) {
        return extractUsername(token);
    }
}
