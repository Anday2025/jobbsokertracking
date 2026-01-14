package com.example.jobtracker.web.dto;

public class CreateJobRequest {
    private String company;
    private String role;
    private String link;
    private String deadline; // ISO: "2026-01-13"

    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public String getDeadline() { return deadline; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
}
