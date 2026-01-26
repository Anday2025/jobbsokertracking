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

    public void sendEmail(
            String apiKey,
            String domain,
            String baseUrl,
            String from,
            String to,
            String subject,
            String text
    ) {

        String url = normalizeBaseUrl(baseUrl) + "/v3/" + domain + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAcceptCharset(java.util.List.of(StandardCharsets.UTF_8));

        // Basic Auth: api:<key>
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
            ResponseEntity<String> res =
                    restTemplate.exchange(url, HttpMethod.POST, req, String.class);

            if (!res.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException(
                        "Mailgun failed: HTTP " + res.getStatusCode() + " body=" + res.getBody()
                );
            }

        } catch (RestClientResponseException e) {
            throw new RuntimeException(
                    "Mailgun error: HTTP " + e.getStatusCode().value() +
                            " body=" + e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            throw new RuntimeException("Mailgun request failed: " + e.getMessage(), e);
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.mailgun.net";
        }

        String b = baseUrl.trim().toLowerCase();

        if (b.equals("eu")) return "https://api.eu.mailgun.net";
        if (b.equals("us")) return "https://api.mailgun.net";

        if (b.startsWith("http")) return baseUrl.trim();

        return "https://api.mailgun.net";
    }
}
