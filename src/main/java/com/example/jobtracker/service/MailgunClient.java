package com.example.jobtracker.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class MailgunClient {

    private final RestTemplate restTemplate = new RestTemplate();

    // ✅ "eu" eller "us" eller tom (default us)
    @Value("${MAILGUN_BASE_URL:}")
    private String mailgunBase;

    private String resolveBaseUrl() {
        String v = (mailgunBase == null ? "" : mailgunBase.trim().toLowerCase());

        // Render env: MAILGUN_BASE_URL="eu"
        if (v.equals("eu")) return "https://api.eu.mailgun.net";
        if (v.equals("us") || v.isBlank()) return "https://api.mailgun.net";

        // hvis noen skriver full URL
        if (v.startsWith("http")) return v;

        // fallback
        return "https://api.mailgun.net";
    }

    public void sendEmail(String apiKey, String domain, String from, String to, String subject, String text) {
        String baseUrl = resolveBaseUrl();
        String url = baseUrl + "/v3/" + domain + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAcceptCharset(java.util.List.of(StandardCharsets.UTF_8));

        // Basic auth: api:APIKEY
        String basic = "api:" + apiKey;
        String encoded = Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("from", from);
        form.add("to", to);
        form.add("subject", subject);
        form.add("text", text);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, req, String.class);

            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Mailgun failed: " + res.getStatusCode() + " body=" + res.getBody());
            }
        } catch (RestClientResponseException e) {
            // ✅ FIX: ikke bruk getRawStatusCode()
            int code = e.getStatusCode().value();
            String body = e.getResponseBodyAsString();
            throw new RuntimeException("Mailgun error: HTTP " + code + " body=" + body, e);
        } catch (Exception e) {
            throw new RuntimeException("Mailgun request failed: " + e.getMessage(), e);
        }
    }
}
