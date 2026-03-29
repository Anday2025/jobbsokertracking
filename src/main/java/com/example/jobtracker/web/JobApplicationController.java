package com.example.jobtracker.web;

import com.example.jobtracker.model.JobApplication;
import com.example.jobtracker.model.Status;
import com.example.jobtracker.service.JobApplicationService;
import com.example.jobtracker.web.dto.CreateJobRequest;
import com.example.jobtracker.web.dto.JobApplicationDto;
import com.example.jobtracker.web.dto.UpdateStatusRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST-kontroller for håndtering av jobbsøknader.
 * <p>
 * Gir API-endepunkter for:
 * <ul>
 *     <li>Hente alle søknader for innlogget bruker</li>
 *     <li>Opprette ny søknad</li>
 *     <li>Oppdatere status</li>
 *     <li>Slette søknad</li>
 * </ul>
 * <p>
 * Alle operasjoner krever autentisering via JWT (bearer token / session).
 */
@Tag(name = "Job Applications", description = "Endpoints for managing job applications")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/apps")
public class JobApplicationController {

    private final JobApplicationService service;

    /**
     * Konstruktør for injisering av service-lag.
     *
     * @param service tjenestelag for jobbsøknader
     */
    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    /**
     * Henter alle jobbsøknader for innlogget bruker.
     *
     * @return liste av {@link JobApplicationDto}
     */
    @Operation(summary = "Get all job applications for the logged-in user")
    @GetMapping
    public List<JobApplicationDto> list() {
        return service.list().stream().map(JobApplicationDto::from).toList();
    }

    /**
     * Oppretter en ny jobbsøknad.
     *
     * @param req data for opprettelse av søknad
     * @return opprettet søknad eller feilmelding
     */
    @Operation(summary = "Create a new job application")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateJobRequest req) {
        if (req.getCompany() == null || req.getCompany().isBlank()
                || req.getRole() == null || req.getRole().isBlank()) {
            return ResponseEntity.badRequest().body("company og role er påkrevd");
        }

        LocalDate deadline = null;
        if (req.getDeadline() != null && !req.getDeadline().isBlank()) {
            deadline = LocalDate.parse(req.getDeadline());
        }

        JobApplication created = service.create(
                req.getCompany().trim(),
                req.getRole().trim(),
                req.getLink() == null ? "" : req.getLink().trim(),
                deadline
        );

        return ResponseEntity.ok(JobApplicationDto.from(created));
    }

    /**
     * Oppdaterer status på en eksisterende jobbsøknad.
     *
     * @param id  ID til søknaden
     * @param req ny status
     * @return oppdatert søknad eller feilmelding
     */
    @Operation(summary = "Update application status")
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable long id, @RequestBody UpdateStatusRequest req) {
        try {
            Status status = Status.valueOf(req.getStatus());
            return service.updateStatus(id, status)
                    .<ResponseEntity<?>>map(app -> ResponseEntity.ok(JobApplicationDto.from(app)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Ugyldig status");
        }
    }

    /**
     * Sletter en jobbsøknad.
     *
     * @param id ID til søknaden som skal slettes
     * @return HTTP 204 hvis slettet, ellers 404 hvis ikke funnet
     */
    @Operation(summary = "Delete a job application")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}