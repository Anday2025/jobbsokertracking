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

    private static String env(String key) {
        return System.getenv(key) == null ? "" : System.getenv(key).trim();
    }

    private static String normalizeBaseUrl(String raw) {
        if (raw == null) return "https://api.mailgun.net";
        String v = raw.trim();
        if (v.isBlank()) return "https://api.mailgun.net";

        // hvis du har satt "eu" / "us"
        if (v.equalsIgnoreCase("eu")) return "https://api.eu.mailgun.net";
        if (v.equalsIgnoreCase("us")) return "https://api.mailgun.net";

        // hvis du har satt full URL
        if (v.startsWith("http://") || v.startsWith("https://")) {
            // fjern trailing slash
            return v.endsWith("/") ? v.substring(0, v.length() - 1) : v;
        }

        // fallback hvis noen har skrevet "api.eu.mailgun.net"
        if (v.startsWith("api.")) return "https://" + v;

        return "https://api.mailgun.net";
    }

    public void sendEmail(String from, String to, String subject, String text) {
        String apiKey = env("MAILGUN_API_KEY");
        String domain = env("MAILGUN_DOMAIN");
        String baseUrl = normalizeBaseUrl(env("MAILGUN_BASE_URL"));

        if (apiKey.isBlank()) throw new RuntimeException("MAILGUN_API_KEY mangler");
        if (domain.isBlank()) throw new RuntimeException("MAILGUN_DOMAIN mangler");
        if (from == null || from.isBlank()) throw new RuntimeException("MAIL_FROM mangler");
        if (to == null || to.isBlank()) throw new RuntimeException("to mangler");

        String url = baseUrl + "/v3/" + domain + "/messages";

        // Basic auth: api:<key>
        String basic = Base64.getEncoder().encodeToString(("api:" + apiKey).getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.set("Authorization", "Basic " + basic);

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
            // Her får du status + body fra Mailgun (veldig nyttig)
            throw new RuntimeException(
                    "Mailgun error: HTTP " + e.getStatusCode() + " body=" + e.getResponseBodyAsString(),
                    e
            );
        } catch (Exception e) {
            throw new RuntimeException("Mailgun request failed: " + e.getMessage(), e);
        }
    }
}
