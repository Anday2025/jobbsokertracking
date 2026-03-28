package com.example.jobtracker.repository;

import com.example.jobtracker.model.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

/**
 * Repository for håndtering av {@link PasswordResetToken}-entiteter.
 * <p>
 * Brukes for å opprette, hente og rydde opp i passordreset-tokens.
 */
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, String> {

    /**
     * Sletter alle tokens som har utløpt før gitt tidspunkt.
     *
     * @param now nåværende tidspunkt
     * @return antall slettede rader
     */
    long deleteByExpiresAtBefore(Instant now);

    /**
     * Sletter tokens som både er brukt og utløpt før gitt tidspunkt.
     * <p>
     * Brukes for å rydde opp gamle og ubrukelige tokens.
     *
     * @param cutoff tidspunkt for hva som anses som gammelt
     * @return antall slettede rader
     */
    long deleteByUsedTrueAndExpiresAtBefore(Instant cutoff);
}