package com.example.jobtracker.service;

import com.example.jobtracker.model.PasswordResetToken;
import com.example.jobtracker.model.User;
import com.example.jobtracker.model.VerificationToken;
import com.example.jobtracker.repository.PasswordResetTokenRepository;
import com.example.jobtracker.repository.UserRepository;
import com.example.jobtracker.repository.VerificationTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       VerificationTokenRepository verificationTokenRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.verificationTokenRepository = verificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record PendingVerification(String email, String token) {}

    private String normEmail(String email) {
        return email == null ? "" : email.toLowerCase().trim();
    }

    @Transactional
    public PendingVerification createPendingUser(String email, String rawPassword) {
        String normalizedEmail = normEmail(email);

        if (normalizedEmail.isBlank()) {
            throw new IllegalStateException("E-post mangler");
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("E-post er allerede registrert");
        }

        User user = new User(normalizedEmail, passwordEncoder.encode(rawPassword));
        user.setEnabled(false);
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofHours(24));
        VerificationToken vt = new VerificationToken(token, user, expiresAt);
        verificationTokenRepository.save(vt);

        return new PendingVerification(user.getEmail(), token);
    }

    @Transactional
    public String createNewVerificationToken(String email) {
        String normalizedEmail = normEmail(email);

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) return "";                // ikke avslør
        if (user.isEnabled()) return "";            // allerede aktiv

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofHours(24));
        VerificationToken vt = new VerificationToken(token, user, expiresAt);
        verificationTokenRepository.save(vt);

        return token;
    }

    @Transactional
    public String createPasswordResetToken(String email) {
        String normalizedEmail = normEmail(email);

        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        if (user == null) return ""; // ikke avslør

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(30));

        PasswordResetToken prt = new PasswordResetToken(token, user, expiresAt);
        passwordResetTokenRepository.save(prt);

        return token;
    }
}
