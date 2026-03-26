package com.example.jobtracker.model;

import jakarta.persistence.*;

/**
 * Entitet som representerer en bruker i systemet.
 * <p>
 * En bruker kan autentisere seg i systemet og eie flere
 * jobbsøknader og tokens.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User {

    /**
     * Hash'et passord.
     * <p>
     * Lagres sikkert og aldri i klartekst.
     */
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    /**
     * Unik identifikator for brukeren.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Brukerens e-postadresse.
     * <p>
     * Må være unik.
     */
    @Column(nullable = false)
    private String email;

    /**
     * Angir om brukeren er aktivert (verifisert).
     */
    @Column(nullable = false)
    private boolean enabled = false;

    /**
     * Standard konstruktør for JPA.
     */
    public User() {}

    /**
     * Oppretter en ny bruker.
     *
     * @param email brukerens e-post
     * @param passwordHash hash'et passord
     */
    public User(String email, String passwordHash) {
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /** @return om bruker er aktivert */
    public boolean isEnabled() { return enabled; }

    /** @param enabled setter aktivert-status */
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /** @return bruker-ID */
    public Long getId() { return id; }

    /** @return e-post */
    public String getEmail() { return email; }

    /** @return hash'et passord */
    public String getPasswordHash() { return passwordHash; }

    /** @param id setter ID */
    public void setId(Long id) { this.id = id; }

    /** @param email setter e-post */
    public void setEmail(String email) { this.email = email; }

    /** @param passwordHash setter passord-hash */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}