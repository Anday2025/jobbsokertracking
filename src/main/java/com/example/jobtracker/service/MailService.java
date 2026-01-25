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

    private void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(name + " mangler i Environment (Render)");
        }
    }

    private void requireMailgun() {
        require(apiKey, "MAILGUN_API_KEY");
        require(domain, "MAILGUN_DOMAIN");
        require(from, "MAIL_FROM");
    }

    public void sendVerificationEmail(String to, String link) {
        requireMailgun();
        require(to, "TO");

        String subject = "Bekreft e-post for Jobbsøker-tracker";
        String text = """
                Hei!

                Klikk her for å aktivere brukeren din:
                %s

                Hvis du ikke har opprettet konto, kan du ignorere denne e-posten.

                Hilsen
                Jobbsøker-tracker
                """.formatted(link);

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text);
    }

    public void sendResetPasswordEmail(String to, String resetUrl) {
        requireMailgun();
        require(to, "TO");

        String subject = "Reset passord - Jobbsøker-tracker";
        String text = """
                Hei!

                Klikk her for å resette passordet ditt:
                %s

                Denne linken utløper om 30 minutter.
                Hvis du ikke ba om å resette passord, kan du ignorere denne e-posten.

                Hilsen
                Jobbsøker-tracker
                """.formatted(resetUrl);

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text);
    }

    // ✅ ny: Send bekreftelse når passord er endret
    public void sendPasswordChangedEmail(String to) {
        requireMailgun();
        require(to, "TO");

        String subject = "Passord endret - Jobbsøker-tracker";
        String text = """
                Hei!

                Passordet ditt er nå endret ✅

                Hvis det ikke var deg, anbefaler vi at du:
                1) resetter passord på nytt
                2) kontakter support / eier av appen

                Hilsen
                Jobbsøker-tracker
                """;

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text);
    }
}
