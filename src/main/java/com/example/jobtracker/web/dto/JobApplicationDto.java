package com.example.jobtracker.web.dto;

import com.example.jobtracker.model.JobApplication;

/**
 * DTO for visning av en jobbsøknad til klienten.
 * <p>
 * Klassen brukes for å sende et forenklet og frontend-vennlig
 * objekt tilbake i API-responser.
 */
public class JobApplicationDto {

    /**
     * ID til jobbsøknaden.
     */
    public Long id;

    /**
     * Navn på selskapet.
     */
    public String company;

    /**
     * Rollen eller stillingstittelen.
     */
    public String role;

    /**
     * Lenke til stillingsannonse.
     */
    public String link;

    /**
     * Søknadsfrist som tekst i ISO-format, for eksempel {@code 2026-01-18}.
     */
    public String deadline;

    /**
     * Status for jobbsøknaden som tekst.
     */
    public String status;

    /**
     * Oppretter en DTO basert på en {@link JobApplication}-entitet.
     *
     * @param a jobbsøknaden som skal konverteres
     * @return DTO-representasjon av jobbsøknaden
     */
    public static JobApplicationDto from(JobApplication a) {
        JobApplicationDto dto = new JobApplicationDto();
        dto.id = a.getId();
        dto.company = a.getCompany();
        dto.role = a.getRole();
        dto.link = a.getLink();
        dto.deadline = (a.getDeadline() == null) ? null : a.getDeadline().toString();
        dto.status = (a.getStatus() == null) ? null : a.getStatus().name();
        return dto;
    }
}