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

    // ✅ Du har denne i Render (du sa den er "eu")
    @Value("${MAILGUN_BASE_URL:}")
    private String mailgunBaseUrl;

    private String resolveHost() {
        // Default US
        String host = "https://api.mailgun.net";

        if (mailgunBaseUrl == null || mailgunBaseUrl.isBlank()) {
            return host;
        }

        // Hvis du setter "eu" i Render -> EU endpoint
        if ("eu".equalsIgnoreCase(mailgunBaseUrl.trim())) {
            return "https://api.eu.mailgun.net";
        }

        // Hvis du setter en full URL i env, f.eks:
        // MAILGUN_BASE_URL=https://api.eu.mailgun.net
        if (mailgunBaseUrl.startsWith("http")) {
            return mailgunBaseUrl.trim().replaceAll("/$", "");
        }

        return host;
    }

    public void sendEmail(String apiKey, String domain, String from, String to, String subject, String text) {
        String url = resolveHost() + "/v3/" + domain + "/messages";

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

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, req, String.class);

            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Mailgun failed: " + res.getStatusCode() + " body=" + res.getBody());
            }

        } catch (RestClientResponseException e) {
            // ✅ FIX: getRawStatusCode() -> getStatusCode().value()
            throw new RuntimeException(
                    "Mailgun error: HTTP " + e.getStatusCode().value() + " body=" + e.getResponseBodyAsString(),
                    e
            );

        } catch (Exception e) {
            throw new RuntimeException("Mailgun request failed: " + e.getMessage(), e);
        }
    }
}
