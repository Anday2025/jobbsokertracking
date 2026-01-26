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

    @Value("${MAILGUN_BASE_URL:https://api.mailgun.net}")
    private String baseUrl;

    @Value("${MAIL_FROM:}")
    private String from;

    public MailService(MailgunClient mailgunClient) {
        this.mailgunClient = mailgunClient;
    }

    private void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " mangler i Render env");
        }
    }

    // ---------- VERIFY ----------
    public void sendVerificationEmail(String to, String link) {
        require(apiKey, "MAILGUN_API_KEY");
        require(domain, "MAILGUN_DOMAIN");
        require(from, "MAIL_FROM");

        String subject = "Bekreft e-post for Jobbsøker-tracker";
        String text = """
                Hei!

                Klikk her for å aktivere brukeren din:
                %s

                Hvis du ikke opprettet konto, ignorer denne e-posten.

                Hilsen
                Jobbsøker-tracker
                """.formatted(link);

        mailgunClient.sendEmail(apiKey, domain, baseUrl, from, to, subject, text);
    }

    // ---------- RESET PASSWORD ----------
    public void sendResetPasswordEmail(String to, String resetUrl) {
        require(apiKey, "MAILGUN_API_KEY");
        require(domain, "MAILGUN_DOMAIN");
        require(from, "MAIL_FROM");

        String subject = "Reset passord";
        String text = """
                Hei!

                Klikk her for å resette passordet ditt:
                %s

                Linken utløper om 30 minutter.
                Hvis du ikke ba om dette, ignorer meldingen.

                Hilsen
                Jobbsøker-tracker
                """.formatted(resetUrl);

        mailgunClient.sendEmail(apiKey, domain, baseUrl, from, to, subject, text);
    }

    // ---------- PASSWORD CHANGED CONFIRM ----------
    public void sendPasswordChangedEmail(String to) {
        require(apiKey, "MAILGUN_API_KEY");
        require(domain, "MAILGUN_DOMAIN");
        require(from, "MAIL_FROM");

        String subject = "Passordet ditt ble endret";
        String text = """
                Hei!

                Passordet ditt ble nylig endret.
                Hvis dette ikke var deg, kontakt support umiddelbart.

                Hilsen
                Jobbsøker-tracker
                """;

        mailgunClient.sendEmail(apiKey, domain, baseUrl, from, to, subject, text);
    }
}
