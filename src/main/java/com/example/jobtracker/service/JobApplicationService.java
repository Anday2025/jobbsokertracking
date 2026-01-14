package com.example.jobtracker.service;

import com.example.jobtracker.model.JobApplication;
import com.example.jobtracker.model.Status;
import com.example.jobtracker.repository.JobApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class JobApplicationService {

    private final JobApplicationRepository repo;

    public JobApplicationService(JobApplicationRepository repo) {
        this.repo = repo;
    }

    public List<JobApplication> list() {
        return repo.findAll().stream()
                .sorted(Comparator.comparing(
                        JobApplication::getDeadline,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ))
                .toList();
    }

    public JobApplication create(String company, String role, String link, LocalDate deadline) {
        JobApplication app = new JobApplication();
        app.setCompany(company);
        app.setRole(role);
        app.setLink(link);
        app.setDeadline(deadline);
        app.setStatus(Status.PLANLAGT);
        return repo.save(app);
    }

    public Optional<JobApplication> updateStatus(long id, Status status) {
        Optional<JobApplication> found = repo.findById(id);
        if (found.isEmpty()) return Optional.empty();

        JobApplication app = found.get();
        app.setStatus(status);
        return Optional.of(repo.save(app));
    }

    public boolean delete(long id) {
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        return true;
    }
}
