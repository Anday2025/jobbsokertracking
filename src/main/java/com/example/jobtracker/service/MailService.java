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

    private void validateCommon(String to) {
        require(apiKey, "MAILGUN_API_KEY");
        require(domain, "MAILGUN_DOMAIN");
        require(from, "MAIL_FROM");
        require(to, "TO");
    }

    // ✅ 1) Verification mail
    public void sendVerificationEmail(String to, String link) {
        validateCommon(to);

        String subject = "Bekreft e-post for Jobbsøker-tracker";
        String text = """
                Hei!

                Klikk her for å aktivere brukeren din:
                %s

                Denne linken utløper om 24 timer.

                Hilsen
                Jobbsøker-tracker
                """.formatted(link);

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text);
    }

    // ✅ 2) Reset password mail
    public void sendResetPasswordEmail(String to, String resetUrl) {
        validateCommon(to);

        String subject = "Reset passord";
        String text = """
                Hei!

                Du har bedt om å resette passordet ditt.
                Klikk på linken under for å velge nytt passord:

                %s

                Denne linken utløper om 30 minutter.
                Hvis du ikke ba om dette, kan du ignorere e-posten.

                Hilsen
                Jobbsøker-tracker
                """.formatted(resetUrl);

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text);
    }

    // ✅ 3) Confirmation after password change
    public void sendPasswordChangedEmail(String to) {
        validateCommon(to);

        String subject = "Passordet ditt er endret ✅";
        String text = """
                Hei!

                Passordet ditt for Jobbsøker-tracker ble nettopp endret.

                Hvis du ikke gjorde dette selv, anbefaler vi at du:
                1) Resetter passordet umiddelbart
                2) Kontakter support

                Hilsen
                Jobbsøker-tracker
                """;

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text);
    }
}
