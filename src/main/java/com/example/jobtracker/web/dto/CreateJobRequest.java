package com.example.jobtracker.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for opprettelse av en ny jobbsøknad.
 * <p>
 * Klassen representerer data sendt fra klienten når en ny
 * jobbsøknad skal opprettes.
 */
@Schema(description = "Request body for creating a new job application")
public class CreateJobRequest {

    /**
     * Navn på selskapet det søkes hos.
     */
    @Schema(description = "Company name", example = "DNB")
    private String company;

    /**
     * Rollen eller stillingstittelen.
     */
    @Schema(description = "Job title or role", example = "Junior Developer")
    private String role;

    /**
     * Lenke til stillingsannonse eller relevant side.
     */
    @Schema(description = "Link to job posting", example = "https://example.com/job/123")
    private String link;

    /**
     * Søknadsfrist i ISO-format, for eksempel {@code 2026-01-13}.
     */
    @Schema(description = "Application deadline (ISO format)", example = "2026-04-06")
    private String deadline;

    /**
     * Henter selskapsnavn.
     *
     * @return selskapsnavn
     */
    public String getCompany() { return company; }

    /**
     * Setter selskapsnavn.
     *
     * @param company selskapsnavn
     */
    public void setCompany(String company) { this.company = company; }

    /**
     * Henter rollenavn.
     *
     * @return rolle eller stillingstittel
     */
    public String getRole() { return role; }

    /**
     * Setter rollenavn.
     *
     * @param role rolle eller stillingstittel
     */
    public void setRole(String role) { this.role = role; }

    /**
     * Henter lenke til stilling.
     *
     * @return lenke til annonse eller relevant ressurs
     */
    public String getLink() { return link; }

    /**
     * Setter lenke til stilling.
     *
     * @param link lenke til annonse eller relevant ressurs
     */
    public void setLink(String link) { this.link = link; }

    /**
     * Henter søknadsfrist som streng i ISO-format.
     *
     * @return søknadsfrist
     */
    public String getDeadline() { return deadline; }

    /**
     * Setter søknadsfrist som streng i ISO-format.
     *
     * @param deadline søknadsfrist
     */
    public void setDeadline(String deadline) { this.deadline = deadline; }
}