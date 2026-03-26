package com.example.jobtracker.repository;

import com.example.jobtracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for håndtering av {@link User}-entiteter.
 * <p>
 * Gir tilgang til brukerdata og spesialiserte oppslag basert på e-post.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finner en bruker basert på e-postadresse.
     *
     * @param email e-postadresse
     * @return valgfri bruker dersom funnet
     */
    Optional<User> findByEmail(String email);

    /**
     * Sjekker om en bruker eksisterer med gitt e-postadresse.
     *
     * @param email e-postadresse
     * @return {@code true} dersom bruker finnes, ellers {@code false}
     */
    boolean existsByEmail(String email);
}