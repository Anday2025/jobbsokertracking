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

    private void requireMailgunConfig(String to) {
        require(apiKey, "MAILGUN_API_KEY");
        require(domain, "MAILGUN_DOMAIN");
        require(from, "MAIL_FROM");
        require(to, "TO");
    }

    public void sendVerificationEmail(String to, String link) {
        requireMailgunConfig(to);

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

    public void sendResetPasswordEmail(String to, String resetUrl) {
        requireMailgunConfig(to);

        String subject = "Reset passord";
        String text = """
                Hei!

                Klikk her for å resette passordet ditt:
                %s

                Denne linken utløper om 30 minutter.

                Hvis du ikke ba om dette, kan du ignorere e-posten.

                Hilsen
                Jobbsøker-tracker
                """.formatted(resetUrl);

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text);
    }
}
