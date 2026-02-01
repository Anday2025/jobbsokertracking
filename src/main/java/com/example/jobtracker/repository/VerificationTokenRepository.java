package com.example.jobtracker.repository;

import com.example.jobtracker.model.User;
import com.example.jobtracker.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {

    Optional<VerificationToken> findByToken(String token);

    // Siden DB har UNIQUE på user_id (verification_token_user_id_key)
    Optional<VerificationToken> findByUser(User user);
}
