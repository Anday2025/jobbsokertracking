package com.example.jobtracker.service;

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

    /**
     * baseUrl eksempel:
     *  - https://api.mailgun.net
     *  - https://api.eu.mailgun.net   (EU region)
     */
    public void sendEmail(String baseUrl,
                          String apiKey,
                          String domain,
                          String from,
                          String to,
                          String subject,
                          String text) {

        // fallback hvis baseUrl ikke er satt
        String effectiveBaseUrl = (baseUrl == null || baseUrl.isBlank())
                ? "https://api.mailgun.net"
                : baseUrl.trim();

        // sørg for at baseUrl ikke ender med /
        if (effectiveBaseUrl.endsWith("/")) {
            effectiveBaseUrl = effectiveBaseUrl.substring(0, effectiveBaseUrl.length() - 1);
        }

        String url = effectiveBaseUrl + "/v3/" + domain + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Basic auth: username=api, password=apiKey
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
                throw new RuntimeException("Mailgun failed: HTTP " + res.getStatusCode() + " body=" + res.getBody());
            }
        } catch (HttpStatusCodeException e) {
            // ✅ dette er det du prøvde å gjøre: raw status + body
            throw new RuntimeException(
                    "Mailgun error: HTTP " + e.getStatusCode().value() + " body=" + e.getResponseBodyAsString());

        } catch (Exception e) {
            throw new RuntimeException("Mailgun request failed: " + e.getMessage(), e);
        }
    }
}
