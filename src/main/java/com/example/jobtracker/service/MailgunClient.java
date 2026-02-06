package com.example.jobtracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class MailgunClient {

    private final RestTemplate restTemplate = new RestTemplate();

    // Render env vars kan leses via ${...} når du kjører på Spring Boot
    @Value("${MAILGUN_API_KEY:}")
    private String apiKey;

    @Value("${MAILGUN_DOMAIN:}")
    private String domain;

    // eks: https://api.eu.mailgun.net  (EU)  eller https://api.mailgun.net (US)
    @Value("${MAILGUN_BASE_URL:https://api.mailgun.net}")
    private String baseUrl;

    // eks: Jobbsøker-tracker <postmaster@sandboxXXXX.mailgun.org>
    @Value("${MAIL_FROM:}")
    private String fromDefault;

    private void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing env var: " + name);
        }
    }

    /**
     * Send mail via Mailgun.
     * @param to      mottaker
     * @param subject subject
     * @param text    plain text body
     */
    public void sendEmail(String to, String subject, String text) {
        sendEmail(fromDefault, to, subject, text);
    }

    /**
     * Send mail via Mailgun med custom from.
     */
    public void sendEmail(String from, String to, String subject, String text) {

        // Validate config
        require(apiKey, "MAILGUN_API_KEY");
        require(domain, "MAILGUN_DOMAIN");
        require(baseUrl, "MAILGUN_BASE_URL");
        require(from, "MAIL_FROM");
        require(to, "TO");

        String url = normalizeBaseUrl(baseUrl) + "/v3/" + domain + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set(HttpHeaders.AUTHORIZATION, basicAuthHeader("api", apiKey));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("from", from);
        form.add("to", to);
        form.add("subject", subject);
        form.add("text", text);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, req, String.class);

            // ✅ Render logs
            System.out.println("MAILGUN URL: " + url);
            System.out.println("MAILGUN STATUS: " + res.getStatusCode());
            System.out.println("MAILGUN BODY: " + (res.getBody() == null ? "" : res.getBody()));

            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Mailgun failed: HTTP " + res.getStatusCode()
                        + " body=" + res.getBody());
            }

        } catch (HttpStatusCodeException e) {
            // ✅ Render logs
            System.err.println("MAILGUN URL: " + url);
            System.err.println("MAILGUN ERROR STATUS: " + e.getStatusCode());
            System.err.println("MAILGUN ERROR BODY: " + e.getResponseBodyAsString());

            throw new RuntimeException("Mailgun error: HTTP " + e.getStatusCode()
                    + " body=" + e.getResponseBodyAsString(), e);

        } catch (Exception e) {
            // ✅ Render logs
            System.err.println("MAILGUN URL: " + url);
            System.err.println("MAILGUN REQUEST FAILED: " + e.getMessage());
            throw new RuntimeException("Mailgun request failed: " + e.getMessage(), e);
        }
    }

    private String normalizeBaseUrl(String base) {
        String b = base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b;
    }

    private String basicAuthHeader(String user, String pass) {
        String token = user + ":" + pass;
        String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}
