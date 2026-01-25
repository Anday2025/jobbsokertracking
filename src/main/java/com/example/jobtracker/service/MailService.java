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

    private void requireMailgunConfig() {
        require(apiKey, "MAILGUN_API_KEY");
        require(domain, "MAILGUN_DOMAIN");
        require(from, "MAIL_FROM");
    }

    // ✅ 1) Verification mail
    public void sendVerificationEmail(String to, String link) {
        requireMailgunConfig();
        require(to, "TO");

        String subject = "Bekreft e-post for Jobbsøker-tracker";

        String text = """
                Hei!

                Klikk her for å aktivere brukeren din:
                %s

                Hilsen
                Jobbsøker-tracker
                """.formatted(link);

        String html = """
                <div style="font-family:Arial,sans-serif;line-height:1.5">
                  <h2>Bekreft e-post</h2>
                  <p>Hei!</p>
                  <p>Klikk på knappen under for å aktivere brukeren din:</p>
                  <p>
                    <a href="%s"
                       style="display:inline-block;padding:12px 18px;background:#4f46e5;color:white;text-decoration:none;border-radius:8px">
                       Bekreft e-post
                    </a>
                  </p>
                  <p>Hvis knappen ikke fungerer, bruk denne linken:</p>
                  <p><a href="%s">%s</a></p>
                  <hr/>
                  <p>Hilsen<br/>Jobbsøker-tracker</p>
                </div>
                """.formatted(link, link, link);

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text, html);
    }

    // ✅ 2) Forgot password / reset link mail
    public void sendResetPasswordEmail(String to, String resetUrl) {
        requireMailgunConfig();
        require(to, "TO");

        String subject = "Reset passord – Jobbsøker-tracker";

        String text = """
                Hei!

                Du har bedt om å resette passordet ditt.
                Klikk her for å velge nytt passord:
                %s

                Denne linken utløper om 30 minutter.

                Hvis du ikke ba om dette, kan du ignorere e-posten.
                """.formatted(resetUrl);

        String html = """
                <div style="font-family:Arial,sans-serif;line-height:1.5">
                  <h2>Reset passord</h2>
                  <p>Hei!</p>
                  <p>Du har bedt om å resette passordet ditt.</p>
                  <p>Klikk på knappen under for å velge nytt passord:</p>
                  <p>
                    <a href="%s"
                       style="display:inline-block;padding:12px 18px;background:#7c3aed;color:white;text-decoration:none;border-radius:8px">
                       Reset passord
                    </a>
                  </p>
                  <p>Denne linken utløper om <b>30 minutter</b>.</p>
                  <p>Hvis knappen ikke fungerer, bruk denne linken:</p>
                  <p><a href="%s">%s</a></p>
                  <hr/>
                  <p>Hvis du ikke ba om dette, kan du ignorere e-posten.</p>
                </div>
                """.formatted(resetUrl, resetUrl, resetUrl);

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text, html);
    }

    // ✅ 3) Confirmation mail after password changed
    public void sendPasswordChangedEmail(String to) {
        requireMailgunConfig();
        require(to, "TO");

        String subject = "Passord endret – Jobbsøker-tracker";

        String text = """
                Hei!

                Passordet ditt er nå endret ✅

                Hvis dette ikke var deg, bør du resette passordet på nytt med én gang.

                Hilsen
                Jobbsøker-tracker
                """;

        String html = """
                <div style="font-family:Arial,sans-serif;line-height:1.5">
                  <h2>Passord endret ✅</h2>
                  <p>Hei!</p>
                  <p>Passordet ditt er nå endret.</p>
                  <p>Hvis dette ikke var deg, bør du resette passordet på nytt med én gang.</p>
                  <hr/>
                  <p>Hilsen<br/>Jobbsøker-tracker</p>
                </div>
                """;

        mailgunClient.sendEmail(apiKey, domain, from, to, subject, text, html);
    }
}
