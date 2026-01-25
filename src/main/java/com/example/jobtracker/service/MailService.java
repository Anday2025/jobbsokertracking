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

    private void requireMailConfig() {
        require(apiKey, "MAILGUN_API_KEY");
        require(domain, "MAILGUN_DOMAIN");
        require(from, "MAIL_FROM");
    }

    // ✅ VERIFICATION
    public void sendVerificationEmail(String to, String verifyUrl) {
        requireMailConfig();
        require(to, "TO");

        String subject = "Bekreft e-post for Jobbsøker-tracker";

        String text = """
                Hei!

                Klikk her for å aktivere brukeren din:
                %s

                Hilsen
                Jobbsøker-tracker
                """.formatted(verifyUrl);

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text);
    }

    // ✅ FORGOT PASSWORD -> sender reset-link
    public void sendResetPasswordEmail(String to, String resetUrl) {
        requireMailConfig();
        require(to, "TO");

        String subject = "Reset passord – Jobbsøker-tracker";

        String text = """
                Hei!

                Du ba om å resette passordet ditt.
                Klikk på linken under for å lage et nytt passord:

                %s

                Linken utløper om 30 minutter.

                Hvis du ikke ba om dette, kan du ignorere denne e-posten.
                """.formatted(resetUrl);

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text);
    }

    // ✅ PASSORD ENDRET -> bekreftelse
    public void sendPasswordChangedEmail(String to) {
        requireMailConfig();
        require(to, "TO");

        String subject = "Passordet ditt er endret – Jobbsøker-tracker";

        String text = """
                Hei!

                Passordet ditt ble nylig endret.

                Hvis dette ikke var deg, anbefaler vi at du:
                1) Resetter passordet ditt igjen umiddelbart
                2) Kontakter support

                Hilsen
                Jobbsøker-tracker
                """;

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text);
    }
}
