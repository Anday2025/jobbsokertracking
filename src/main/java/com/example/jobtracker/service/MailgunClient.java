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

/**
 * Lavnivå-klient for utsending av e-post via Mailgun API.
 * <p>
 * Klassen bygger og sender HTTP-forespørsler til Mailgun og leser
 * nødvendig konfigurasjon fra miljøvariabler / Spring properties.
 */
@Component
public class MailgunClient {

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Mailgun API-nøkkel.
     */
    @Value("${MAILGUN_API_KEY:}")
    private String apiKey;

    /**
     * Mailgun-domene.
     */
    @Value("${MAILGUN_DOMAIN:}")
    private String domain;

    /**
     * Basis-URL for Mailgun API.
     * <p>
     * Eksempel: {@code https://api.eu.mailgun.net}
     */
    @Value("${MAILGUN_BASE_URL:https://api.eu.mailgun.net}")
    private String baseUrl;

    /**
     * Standard avsenderadresse.
     */
    @Value("${MAIL_FROM:}")
    private String fromDefault;

    /**
     * Validerer at en påkrevd konfigurasjonsverdi finnes.
     *
     * @param value verdien som skal sjekkes
     * @param name navn på miljøvariabel/property
     * @throws IllegalStateException dersom verdien mangler eller er tom
     */
    private void require(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing env var: " + name);
        }
    }

    /**
     * Sender e-post via Mailgun med standard avsenderadresse.
     *
     * @param to mottaker
     * @param subject emnefelt
     * @param text plain text-innhold
     */
    public void sendEmail(String to, String subject, String text) {
        sendEmail(fromDefault, to, subject, text);
    }

    /**
     * Sender e-post via Mailgun med eksplisitt avsenderadresse.
     *
     * @param from avsender
     * @param to mottaker
     * @param subject emnefelt
     * @param text plain text-innhold
     * @throws RuntimeException dersom Mailgun-kallet feiler
     */
    public void sendEmail(String from, String to, String subject, String text) {
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

            System.out.println("MAILGUN URL: " + url);
            System.out.println("MAILGUN STATUS: " + res.getStatusCode());
            System.out.println("MAILGUN BODY: " + (res.getBody() == null ? "" : res.getBody()));

            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Mailgun failed: HTTP " + res.getStatusCode()
                        + " body=" + res.getBody());
            }

        } catch (HttpStatusCodeException e) {
            System.err.println("MAILGUN URL: " + url);
            System.err.println("MAILGUN ERROR STATUS: " + e.getStatusCode());
            System.err.println("MAILGUN ERROR BODY: " + e.getResponseBodyAsString());

            throw new RuntimeException("Mailgun error: HTTP " + e.getStatusCode()
                    + " body=" + e.getResponseBodyAsString(), e);

        } catch (Exception e) {
            System.err.println("MAILGUN URL: " + url);
            System.err.println("MAILGUN REQUEST FAILED: " + e.getMessage());
            throw new RuntimeException("Mailgun request failed: " + e.getMessage(), e);
        }
    }

    /**
     * Fjerner eventuelle avsluttende skråstreker fra basis-URL.
     *
     * @param base rå basis-URL
     * @return normalisert basis-URL uten avsluttende skråstrek
     */
    private String normalizeBaseUrl(String base) {
        String b = base.trim();
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b;
    }

    /**
     * Lager HTTP Basic Authorization-headerverdi.
     *
     * @param user brukernavn
     * @param pass passord eller API-nøkkel
     * @return ferdig {@code Authorization}-headerverdi
     */
    private String basicAuthHeader(String user, String pass) {
        String token = user + ":" + pass;
        String encoded = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}