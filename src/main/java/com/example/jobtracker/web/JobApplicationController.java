package com.example.jobtracker.web;

import com.example.jobtracker.model.JobApplication;
import com.example.jobtracker.model.Status;
import com.example.jobtracker.service.JobApplicationService;
import com.example.jobtracker.web.dto.CreateJobRequest;
import com.example.jobtracker.web.dto.JobApplicationDto;
import com.example.jobtracker.web.dto.UpdateStatusRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/apps")
public class JobApplicationController {

    private final JobApplicationService service;

    public JobApplicationController(JobApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public List<JobApplicationDto> list() {
        return service.list().stream().map(JobApplicationDto::from).toList();
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable long id) {
        return service.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
