package com.example.jobtracker.service;

import com.example.jobtracker.model.JobApplication;
import com.example.jobtracker.model.Status;
import com.example.jobtracker.model.User;
import com.example.jobtracker.repository.JobApplicationRepository;
import com.example.jobtracker.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class JobApplicationService {

    private final JobApplicationRepository repo;
    private final UserRepository userRepo;

    public JobApplicationService(JobApplicationRepository repo, UserRepository userRepo) {
        this.repo = repo;
        this.userRepo = userRepo;
    }

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return null;
        return auth.getName(); // ✅ dette blir email når JwtAuthFilter setter auth
    }

    private User currentUser() {
        String email = currentEmail();
        if (email == null) throw new RuntimeException("Unauthorized");
        return userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    public List<JobApplication> list() {
        User u = currentUser();
        return repo.findAllByUser(u).stream()
                .sorted(Comparator.comparing(
                        JobApplication::getDeadline,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();
    }

    public JobApplication create(String company, String role, String link, LocalDate deadline) {
        User u = currentUser();

        JobApplication app = new JobApplication();
        app.setUser(u); // ✅ VIKTIG
        app.setCompany(company);
        app.setRole(role);
        app.setLink(link);
        app.setDeadline(deadline);
        app.setStatus(Status.PLANLAGT);

        return repo.save(app);
    }

    public Optional<JobApplication> updateStatus(long id, Status status) {
        User u = currentUser();

        Optional<JobApplication> found = repo.findByIdAndUser(id, u);
        if (found.isEmpty()) return Optional.empty();

        JobApplication app = found.get();
        app.setStatus(status);
        return Optional.of(repo.save(app));
    }

    public boolean delete(long id) {
        User u = currentUser();

        Optional<JobApplication> found = repo.findByIdAndUser(id, u);
        if (found.isEmpty()) return false;

        repo.delete(found.get());
        return true;
    }
}
