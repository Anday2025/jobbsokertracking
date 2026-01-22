package com.example.jobtracker.web.dto;

public class UpdateStatusRequest {
    private String status; // "SOKT", "INTERVJU" ...

    public String getStatus() { return status; }


    public void setStatus(String status) { this.status = status; }
}
