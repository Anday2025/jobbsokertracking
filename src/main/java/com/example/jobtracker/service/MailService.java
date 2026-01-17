package com.example.jobtracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final MailgunClient mailgunClient;

    @Value("${MAILGUN_API_KEY:}")
    private String apiKey;

    @Value("${MAILGUN_DOMAIN:}")
    private String domain;

    @Value("${MAIL_FROM:}")
    private String from;

    public MailService(MailgunClient mailgunClient) {
        this.mailgunClient = mailgunClient;
    }

    public void sendVerificationEmail(String to, String link) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("MAILGUN_API_KEY mangler i environment");
        }
        if (domain == null || domain.isBlank()) {
            throw new IllegalStateException("MAILGUN_DOMAIN mangler i environment");
        }
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("MAIL_FROM mangler i environment");
        }

        String subject = "Bekreft e-post for Jobbsøker-tracker";
        String text = """
                Hei!

                Klikk her for å aktivere brukeren din:
                %s

                Hilsen
                Jobbsøker-tracker
                """.formatted(link);

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text);
    }
}
