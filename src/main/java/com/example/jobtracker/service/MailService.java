package com.example.jobtracker.service;

import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final MailgunClient mailgunClient;

    private final String apiKey;
    private final String domain;
    private final String baseUrl;
    private final String from;

    public MailService(MailgunClient mailgunClient) {
        this.mailgunClient = mailgunClient;

        this.apiKey = env("MAILGUN_API_KEY");
        this.domain = env("MAILGUN_DOMAIN");

        // Du bruker dette: https://api.eu.mailgun.net ✅
        this.baseUrl = envOr("MAILGUN_BASE_URL", "https://api.mailgun.net");

        // Du har MAIL_FROM i Render; ellers fallback til postmaster@<domain>
        this.from = envOr("MAIL_FROM", "Jobbsøker-tracker <postmaster@" + domain + ">");
    }

    public void sendVerificationEmail(String to, String verifyUrl) {
        String subject = "Bekreft e-posten din";
        String text =
                "Hei!\n\n" +
                        "Klikk på lenken for å bekrefte e-posten din:\n" +
                        verifyUrl + "\n\n" +
                        "Hvis du ikke opprettet brukeren, kan du ignorere denne e-posten.\n";
        send(to, subject, text);
    }

    public void sendResetPasswordEmail(String to, String resetUrl) {
        String subject = "Tilbakestill passord";
        String text =
                "Hei!\n\n" +
                        "Klikk på lenken for å sette nytt passord:\n" +
                        resetUrl + "\n\n" +
                        "Lenken utløper om 30 minutter.\n";
        send(to, subject, text);
    }

    public void sendPasswordChangedEmail(String to) {
        String subject = "Passordet ditt ble endret";
        String text =
                "Hei!\n\n" +
                        "Passordet ditt ble nettopp endret.\n" +
                        "Hvis det ikke var deg, bør du tilbakestille passordet umiddelbart.\n";
        send(to, subject, text);
    }

    private void send(String to, String subject, String text) {
        mailgunClient.sendEmail(apiKey, baseUrl, domain, from, to, subject, text);
    }

    private static String env(String key) {
        String v = System.getenv(key);
        if (v == null || v.trim().isEmpty()) {
            throw new IllegalStateException("Env var mangler: " + key);
        }
        return v.trim();
    }

    private static String envOr(String key, String def) {
        String v = System.getenv(key);
        return (v == null || v.trim().isEmpty()) ? def : v.trim();
    }
}
