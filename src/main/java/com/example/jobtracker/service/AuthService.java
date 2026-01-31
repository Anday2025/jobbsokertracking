package com.example.jobtracker.service;

import com.example.jobtracker.model.User;
import com.example.jobtracker.model.VerificationToken;
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
    private final VerificationTokenRepository tokenRepo;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       VerificationTokenRepository tokenRepo,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public record PendingVerification(String email, String token) {}

    // ✅ KUN database-logikk her
    @Transactional
    public PendingVerification createPendingUser(String email, String rawPassword) {

        String normalizedEmail = email.toLowerCase().trim();

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalStateException("E-post er allerede registrert");
        }

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(rawPassword)
        );
        user.setEnabled(false);
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofHours(24));

        VerificationToken vt = new VerificationToken(token, user, expiresAt);
        tokenRepo.save(vt);

        return new PendingVerification(user.getEmail(), token);
    }
}
