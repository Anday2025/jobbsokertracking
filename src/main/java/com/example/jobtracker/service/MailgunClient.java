package com.example.jobtracker.service;

import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class MailgunClient {

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendEmail(String apiKey, String domain, String from, String to, String subject, String text) {
        String url = "https://api.mailgun.net/v3/" + domain + "/messages";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Basic Auth: username = "api", password = apiKey
        String basic = "api:" + apiKey;
        String encoded = Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("from", from);
        form.add("to", to);
        form.add("subject", subject);
        form.add("text", text);

        HttpEntity<MultiValueMap<String, String>> req = new HttpEntity<>(form, headers);

        ResponseEntity<String> res = restTemplate.exchange(url, HttpMethod.POST, req, String.class);

        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Mailgun failed: " + res.getStatusCode() + " body=" + res.getBody());
        }
    }
}
