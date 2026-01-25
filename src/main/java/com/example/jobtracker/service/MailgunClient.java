package com.example.jobtracker.service;

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

    // US: https://api.mailgun.net
    // EU: https://api.eu.mailgun.net
    private String baseUrl() {
        String base = System.getenv().getOrDefault("MAILGUN_BASE_URL", "https://api.mailgun.net").trim();
        if (base.isBlank()) base = "https://api.mailgun.net";
        return base;
    }

    public void sendEmail(String apiKey, String domain, String from, String to, String subject, String text) {
        sendEmail(apiKey, domain, from, to, subject, text, null);
    }

    public void sendEmail(String apiKey, String domain, String from, String to,
                          String subject, String text, String html) {

        String url = baseUrl() + "/v3/" + domain + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAcceptCharset(java.util.List.of(StandardCharsets.UTF_8));

        // Basic Auth: username = "api", password = apiKey
        String basic = "api:" + apiKey;
        String encoded = Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("from", from);
        form.add("to", to);
        form.add("subject", subject);

        if (text != null && !text.isBlank()) {
            form.add("text", text);
        }
        if (html != null && !html.isBlank()) {
            form.add("html", html);
        }

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, req, String.class);

            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Mailgun failed: " + res.getStatusCode() + " body=" + res.getBody());
            }

        } catch (RestClientResponseException e) {
            // ✅ Mailgun gir ofte nyttig feilmelding i body (f.eks 401/403)
            throw new RuntimeException(
                    "Mailgun error: HTTP " + e.getStatusCode().value() + " body=" + e.getResponseBodyAsString(),
                    e
            );

        } catch (Exception e) {
            throw new RuntimeException("Mailgun request failed: " + e.getMessage(), e);
        }
    }
}
