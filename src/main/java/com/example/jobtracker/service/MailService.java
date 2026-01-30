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

    // ✅ Du sa du bruker: MAILGUN_BASE_URL = https://api.eu.mailgun.net
    @Value("${MAILGUN_BASE_URL:https://api.mailgun.net}")
    private String baseUrl;

    public MailService(MailgunClient mailgunClient) {
        this.mailgunClient = mailgunClient;
    }

    private void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " mangler i Environment (Render)");
        }
    }

    private void sendViaMailgun(String to, String subject, String text) {
        require(apiKey, "MAILGUN_API_KEY");
        require(domain, "MAILGUN_DOMAIN");
        require(from, "MAIL_FROM");
        require(to, "TO");
        require(baseUrl, "MAILGUN_BASE_URL");

        mailgunClient.sendEmail(baseUrl, apiKey, domain, from, to, subject, text);
    }

    public void sendVerificationEmail(String to, String link) {
        String subject = "Bekreft e-post for Jobbsøker-tracker";
        String text = """
                Hei!

                Klikk her for å aktivere brukeren din:
                %s

                Hilsen
                Jobbsøker-tracker
                """.formatted(link);

        sendViaMailgun(to, subject, text);
    }

    public void sendResetPasswordEmail(String to, String resetUrl) {
        String subject = "Reset passord";
        String text = """
                Hei!

                Klikk her for å resette passordet ditt:
                %s

                Denne linken utløper om 30 minutter.

                Hilsen
                Jobbsøker-tracker
                """.formatted(resetUrl);

        sendViaMailgun(to, subject, text);
    }

    // ✅ Sendes etter vellykket reset (AuthController kaller denne)
    public void sendPasswordChangedEmail(String to) {
        String subject = "Passordet ditt er endret";
        String text = """
                Hei!

                Passordet ditt er nå endret.

                Hvis dette ikke var deg, anbefaler vi at du reseter passordet umiddelbart.

                Hilsen
                Jobbsøker-tracker
                """;

        sendViaMailgun(to, subject, text);
    }
}
