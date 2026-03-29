package com.example.jobtracker.web.dto;
import io.swagger.v3.oas.annotations.media.Schema;
/**
 * DTO for oppdatering av status på en jobbsøknad.
 */
@Schema(description = "Request body for updating an application status")
public class UpdateStatusRequest {

    /**
     * Ny status som tekst, for eksempel {@code SOKT} eller {@code INTERVJU}.
     */

    @Schema(description = "New status", example = "SOKT")
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