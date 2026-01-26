package com.example.jobtracker.service;

import org.springframework.stereotype.Service;

@Service
public class MailService {

    private final MailgunClient mailgunClient;

    public MailService(MailgunClient mailgunClient) {
        this.mailgunClient = mailgunClient;
    }

    private static String env(String key) {
        return System.getenv(key) == null ? "" : System.getenv(key).trim();
    }

    private String from() {
        // Eksempel: "Jobbsøker-tracker <postmaster@sandbox...mailgun.org>"
        String from = env("MAIL_FROM");
        if (from.isBlank()) throw new RuntimeException("MAIL_FROM mangler");
        return from;
    }

    public void sendVerificationEmail(String to, String verifyUrl) {
        String subject = "Bekreft e-posten din";
        String text =
                "Hei!\n\n" +
                        "Klikk linken for å bekrefte kontoen din:\n" +
                        verifyUrl + "\n\n" +
                        "Hvis du ikke opprettet konto, kan du ignorere denne e-posten.\n";

        mailgunClient.sendEmail(from(), to, subject, text);
    }

    public void sendResetPasswordEmail(String to, String resetUrl) {
        String subject = "Nullstill passord";
        String text =
                "Hei!\n\n" +
                        "Klikk linken for å sette nytt passord:\n" +
                        resetUrl + "\n\n" +
                        "Linken utløper snart. Hvis du ikke ba om dette, ignorer e-posten.\n";

        mailgunClient.sendEmail(from(), to, subject, text);
    }

    public void sendPasswordChangedEmail(String to) {
        String subject = "Passord endret";
        String text =
                "Hei!\n\n" +
                        "Passordet ditt ble nettopp endret.\n" +
                        "Hvis dette ikke var deg, bør du resette passord umiddelbart.\n";

        mailgunClient.sendEmail(from(), to, subject, text);
    }
}
