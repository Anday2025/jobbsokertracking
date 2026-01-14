package com.example.jobtracker.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String company;
    private String role;

    @Column(length = 1000)
    private String link;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public JobApplication() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public LocalDate getDeadline() { return deadline; }
    public void setDeadline(LocalDate deadline) { this.deadline = deadline; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
