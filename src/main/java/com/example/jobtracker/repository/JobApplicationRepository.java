package com.example.jobtracker.repository;

import com.example.jobtracker.model.JobApplication;
import com.example.jobtracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for håndtering av {@link JobApplication}-entiteter.
 * <p>
 * Gir tilgang til CRUD-operasjoner og spesialiserte spørringer
 * relatert til jobbsøknader.
 */
public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {

    /**
     * Henter alle jobbsøknader som tilhører en spesifikk bruker.
     *
     * @param user brukeren som eier jobbsøknadene
     * @return liste over jobbsøknader for brukeren
     */
    List<JobApplication> findAllByUser(User user);

    /**
     * Henter en spesifikk jobbsøknad basert på ID og bruker.
     * <p>
     * Brukes for å sikre at brukeren kun får tilgang til egne data.
     *
     * @param id ID til jobbsøknaden
     * @param user brukeren som eier jobbsøknaden
     * @return valgfri jobbsøknad dersom den finnes og tilhører brukeren
     */
    Optional<JobApplication> findByIdAndUser(Long id, User user);
}