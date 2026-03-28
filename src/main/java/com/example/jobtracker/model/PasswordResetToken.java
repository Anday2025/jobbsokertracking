package com.example.jobtracker.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entitet som representerer et token for passordtilbakestilling.
 * <p>
 * Tokenet brukes for å gi brukeren en sikker engangslenke for å
 * sette nytt passord.
 */
@Entity
public class PasswordResetToken {

    /**
     * Selve tokenverdien (unik).
     */
    @Id
    private String token;

    /**
     * Brukeren tokenet tilhører.
     */
    @ManyToOne(optional = false)
    private User user;

    /**
     * Tidspunktet tokenet utløper.
     */
    private Instant expiresAt;

    /**
     * Angir om tokenet allerede er brukt.
     */
    private boolean used = false;

    /**
     * Standard konstruktør for JPA.
     */
    public PasswordResetToken() {}

    /**
     * Oppretter et nytt passordreset-token.
     *
     * @param token tokenverdi
     * @param user bruker
     * @param expiresAt utløpstidspunkt
     */
    public PasswordResetToken(String token, User user, Instant expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    /** @return token */
    public String getToken() { return token; }

    /** @return bruker */
    public User getUser() { return user; }

    /** @return utløpstid */
    public Instant getExpiresAt() { return expiresAt; }

    /** @return true hvis brukt */
    public boolean isUsed() { return used; }

    /** @param used marker token som brukt */
    public void setUsed(boolean used) { this.used = used; }
}