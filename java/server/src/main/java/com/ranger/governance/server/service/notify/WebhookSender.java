package com.ranger.governance.server.service.notify;

import com.ranger.governance.server.config.NotificationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Component
public class WebhookSender {
    private final RestTemplate restTemplate;

    public WebhookSender(NotificationProperties properties, RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getReadTimeoutMs()))
                .build();
    }

    public void postJson(String webhookUrl, Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = restTemplate.postForEntity(
                webhookUrl,
                new HttpEntity<Map<String, Object>>(body, headers),
                String.class
        );
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("webhook call failed with status=" + response.getStatusCodeValue());
        }
    }
}
