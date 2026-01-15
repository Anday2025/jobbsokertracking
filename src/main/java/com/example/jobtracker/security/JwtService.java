package com.example.jobtracker.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long ttlMs = 1000L * 60 * 60 * 24 * 7; // 7 dager

    public JwtService() {
        // LES FRA ENV: JWT_SECRET (minst 32 tegn)
        String secret = System.getenv().getOrDefault("JWT_SECRET", "CHANGE_ME_CHANGE_ME_CHANGE_ME_32CHARS!");
        if (secret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET må være minst 32 tegn");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Brukes av AuthController
    public String generateToken(String email) {
        return buildToken(email);
    }

    // (valgfritt alias hvis du har gammel kode som bruker createToken)
    public String createToken(String email) {
        return buildToken(email);
    }

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

    // Brukes av JwtAuthFilter
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        String email = extractUsername(token);
        return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        Date exp = extractAllClaims(token).getExpiration();
        return exp.before(new Date());
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
