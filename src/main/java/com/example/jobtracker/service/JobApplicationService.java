package com.example.jobtracker.service;

import com.example.jobtracker.model.JobApplication;
import com.example.jobtracker.model.Status;
import com.example.jobtracker.model.User;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Tjeneste for håndtering av jobbsøknader for innlogget bruker.
 * <p>
 * Klassen kapsler inn logikk for opprettelse, henting, oppdatering
 * og sletting av jobbsøknader, og sikrer at brukeren kun får tilgang
 * til egne data.
 */
@Service
public class JobApplicationService {

    private final JobApplicationRepository repo;
    private final UserRepository userRepo;

    /**
     * Oppretter en ny {@code JobApplicationService}.
     *
     * @param repo repository for jobbsøknader
     * @param userRepo repository for brukere
     */
    public JobApplicationService(JobApplicationRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    /**
     * Finner den innloggede brukeren fra Spring Security-konteksten.
     *
     * @return innlogget bruker
     * @throws RuntimeException dersom brukeren ikke finnes i databasen
     */
    private User currentUser() {
        String email = SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();

        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Bruker ikke funnet"));
    }

    /**
     * Henter alle jobbsøknader for innlogget bruker.
     *
     * @return liste over brukerens jobbsøknader
     */
    public List<JobApplication> list() {
        return repo.findAllByUser(currentUser());
    }

    /**
     * Oppretter en ny jobbsøknad for innlogget bruker.
     * <p>
     * Nye søknader opprettes med standardstatus {@link Status#PLANLAGT}.
     *
     * @param company navn på selskapet
     * @param role rolle eller stillingstittel
     * @param link lenke til stillingsannonse
     * @param deadline søknadsfrist
     * @return lagret jobbsøknad
     */
    public JobApplication create(String company, String role, String link, LocalDate deadline) {
        JobApplication app = new JobApplication();
        app.setCompany(company);
        app.setRole(role);
        app.setLink(link);
        app.setDeadline(deadline);
        app.setStatus(Status.PLANLAGT);
        app.setUser(currentUser());

        return repo.save(app);
    }

    /**
     * Oppdaterer status på en eksisterende jobbsøknad for innlogget bruker.
     *
     * @param id ID til jobbsøknaden
     * @param status ny status
     * @return oppdatert jobbsøknad dersom den finnes og tilhører brukeren
     */
    public Optional<JobApplication> updateStatus(long id, Status status) {
        return repo.findByIdAndUser(id, currentUser())
                .map(app -> {
                    app.setStatus(status);
                    return repo.save(app);
                });
    }

    /**
     * Sletter en jobbsøknad dersom den tilhører innlogget bruker.
     *
     * @param id ID til jobbsøknaden som skal slettes
     * @return {@code true} dersom søknaden ble slettet, ellers {@code false}
     */
    public boolean delete(long id) {
        return repo.findByIdAndUser(id, currentUser())
                .map(app -> {
                    repo.delete(app);
                    return true;
                })
                .orElse(false);
    }
}