package com.example.jobtracker.service;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;

@Component
public class MailgunClient {

    private final RestTemplate restTemplate;

    public MailgunClient(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .requestFactory(HttpComponentsClientHttpRequestFactory::new)
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(20))
                .build();
    }

    public void sendEmail(
            String apiKey,
            String baseUrl,
            String domain,
            String from,
            String to,
            String subject,
            String text
    ) {
        if (isBlank(apiKey)) throw new IllegalStateException("MAILGUN_API_KEY mangler");
        if (isBlank(domain)) throw new IllegalStateException("MAILGUN_DOMAIN mangler");
        if (isBlank(from)) throw new IllegalStateException("MAIL_FROM mangler");
        if (isBlank(to)) throw new IllegalArgumentException("to mangler");

        String apiBase = normalizeMailgunBaseUrl(baseUrl);
        String url = apiBase + "/v3/" + domain.trim() + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth("api", apiKey.trim()); // Mailgun: username = "api"
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("from", from);
        form.add("to", to);
        form.add("subject", subject);
        form.add("text", text);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, req, String.class);

            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Mailgun failed: HTTP " + res.getStatusCode().value() + " body=" + res.getBody());
            }
        } catch (HttpStatusCodeException e) {
            // ✅ fikser “rød” linje / deprecated: getRawStatusCode -> getStatusCode().value()
            throw new RuntimeException(
                    "Mailgun error: HTTP " + e.getStatusCode().value() + " body=" + e.getResponseBodyAsString(),
                    e
            );
        } catch (ResourceAccessException e) {
            throw new RuntimeException("Mailgun connection failed: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Mailgun request failed: " + e.getMessage(), e);
        }
    }

    private String normalizeMailgunBaseUrl(String baseUrl) {
        String b = baseUrl == null ? "" : baseUrl.trim();

        // Tillat at du setter "eu" / "us" også
        if (b.equalsIgnoreCase("eu")) return "https://api.eu.mailgun.net";
        if (b.equalsIgnoreCase("us")) return "https://api.mailgun.net";

        // Default hvis tom
        if (b.isEmpty()) return "https://api.mailgun.net";

        // Hvis noen skriver "api.eu.mailgun.net"
        if (!b.startsWith("http://") && !b.startsWith("https://")) {
            b = "https://" + b;
        }

        // Fjern trailing /
        while (b.endsWith("/")) b = b.substring(0, b.length() - 1);
        return b;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
