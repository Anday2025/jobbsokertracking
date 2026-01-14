package com.example.jobtracker.repository;

import com.example.jobtracker.model.JobApplication;
import com.example.jobtracker.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JobApplicationRepository extends JpaRepository<JobApplication, Long> {
    List<JobApplication> findAllByUser(User user);
    Optional<JobApplication> findByIdAndUser(Long id, User user);
}
