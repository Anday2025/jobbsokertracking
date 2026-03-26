package com.example.jobtracker.web.dto;

/**
 * DTO for oppdatering av status på en jobbsøknad.
 */
public class UpdateStatusRequest {

    /**
     * Ny status som tekst, for eksempel {@code SOKT} eller {@code INTERVJU}.
     */
    private String status;

    /**
     * Henter ny status.
     *
     * @return status som tekst
     */
    public String getStatus() { return status; }

    /**
     * Setter ny status.
     *
     * @param status ny status som tekst
     */
    public void setStatus(String status) { this.status = status; }
}