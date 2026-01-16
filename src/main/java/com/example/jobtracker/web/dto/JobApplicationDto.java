package com.example.jobtracker.web.dto;

import com.example.jobtracker.model.JobApplication;

public class JobApplicationDto {
    public Long id;
    public String company;
    public String role;
    public String link;
    public String deadline; // "2026-01-18"
    public String status;

    public static JobApplicationDto from(JobApplication a) {
        JobApplicationDto dto = new JobApplicationDto();
        dto.id = a.getId();
        dto.company = a.getCompany();
        dto.role = a.getRole();
        dto.link = a.getLink();
        dto.deadline = (a.getDeadline() == null) ? null : a.getDeadline().toString();
        dto.status = (a.getStatus() == null) ? null : a.getStatus().name();
        return dto;
    }
}
