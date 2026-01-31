package com.example.jobtracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final MailgunClient mailgunClient;

    @Value("${MAIL_FROM:}")
    private String from;

    public MailService(MailgunClient mailgunClient) {
        this.mailgunClient = mailgunClient;
    }

    private void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing env var: " + name);
        }
    }

    private void sendViaMailgun(String to, String subject, String text) {
        require(from, "MAIL_FROM");
        require(to, "to");

        mailgunClient.sendEmail(from, to, subject, text);
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

    public void sendResetPasswordEmail(String to, String link) {
        String subject = "Reset passord – Jobbsøker-tracker";
        String text = """
                Hei!

                Klikk her for å resette passordet ditt:
                %s

                Hvis du ikke ba om dette, kan du ignorere e-posten.

                Hilsen
                Jobbsøker-tracker
                """.formatted(link);

        sendViaMailgun(to, subject, text);
    }

    public void sendPasswordChangedEmail(String to) {
        String subject = "Passord endret – Jobbsøker-tracker";
        String text = """
                Hei!

                Passordet ditt ble nettopp endret.
                Hvis dette ikke var deg, anbefaler vi at du reseter passordet umiddelbart.

                Hilsen
                Jobbsøker-tracker
                """;

        sendViaMailgun(to, subject, text);
    }
}
