package com.example.jobtracker.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entitet som representerer en jobbsøknad i systemet.
 * <p>
 * Klassen inneholder informasjon om en stilling brukeren har søkt på
 * eller planlegger å søke på, inkludert selskap, rolle, søknadsfrist
 * og nåværende status.
 */
@Table(name = "job_application")
@Entity
public class JobApplication {

    /**
     * Unik identifikator for jobbsøknaden.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Navn på selskapet det er søkt hos.
     */
    private String company;

    /**
     * Rollen eller stillingstittelen.
     */
    private String role;

    /**
     * Lenke til stillingsannonsen.
     * <p>
     * Tillater lengre tekst (opptil 1000 tegn).
     */
    @Column(length = 1000)
    private String link;

    /**
     * Søknadsfrist for stillingen.
     */
    private LocalDate deadline;

    /**
     * Status for jobbsøknaden.
     * <p>
     * Lagres som tekst i databasen (ENUM som String).
     */
    @Enumerated(EnumType.STRING)
    private Status status;

    /**
     * Brukeren som eier jobbsøknaden.
     * <p>
     * Mange jobbsøknader kan tilhøre én bruker.
     * Feltet er skjult i JSON-respons for å unngå eksponering av intern struktur.
     */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    /**
     * Standard konstruktør krevd av JPA.
     */
    public JobApplication() {}

    /** @return ID til jobbsøknaden */
    public Long getId() { return id; }

    /** @param id setter ID */
    public void setId(Long id) { this.id = id; }

    /** @return selskapsnavn */
    public String getCompany() { return company; }

    /** @param company setter selskapsnavn */
    public void setCompany(String company) { this.company = company; }

    /** @return rolle/stilling */
    public String getRole() { return role; }

    /** @param role setter rolle */
    public void setRole(String role) { this.role = role; }

    /** @return lenke til stilling */
    public String getLink() { return link; }

    /** @param link setter lenke */
    public void setLink(String link) { this.link = link; }

    /** @return søknadsfrist */
    public LocalDate getDeadline() { return deadline; }

    /** @param deadline setter søknadsfrist */
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    /** @return status for søknaden */
    public Status getStatus() { return status; }

    /** @param status setter status */
    public void setStatus(Status status) { this.status = status; }

    /**
     * Henter eieren av jobbsøknaden.
     *
     * @return bruker som eier søknaden
     */
    public User getUser() { return user; }

    /**
     * Setter eier av jobbsøknaden.
     *
     * @param user bruker som eier søknaden
     */
    public void setUser(User user) { this.user = user; }
}