package com.mineralwater.service;

import com.mineralwater.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    @Value("${whatsapp.access.token}")
    private String accessToken;

    @Value("${whatsapp.phone.number.id}")
    private String phoneId;

    @Value("${whatsapp.template.name}")
    private String templateName;

    @Value("${admin.mobiles}")
    private String adminMobiles;

    private final RestTemplate restTemplate;

    /**
     * Called to notify multiple admins of a new completed/paid order
     */
    public void sendOrderNotification(Order order) {
        if (adminMobiles == null || adminMobiles.isBlank()) {
            log.warn("No admin mobile numbers configured for notifications.");
            return;
        }

        String[] numbers = adminMobiles.split(",");
        for (String number : numbers) {
            String trimmedNumber = number.trim();
            if (!trimmedNumber.isEmpty()) {
                sendWhatsAppNotification(trimmedNumber, order);
            }
        }
    }

    /**
     * Send WhatsApp Template Notification via Meta Cloud API
     */
    private void sendWhatsAppNotification(String mobileNumber, Order order) {
        if (accessToken == null || accessToken.contains("PLACEHOLDER") || accessToken.isBlank()) {
            log.warn("[Mock WhatsApp] WhatsApp is not configured. Simulating template message to: {} for Order #{}", mobileNumber, order.getId());
            return;
        }

        try {
            String url = "https://graph.facebook.com/v17.0/" + phoneId + "/messages";

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("messaging_product", "whatsapp");
            body.put("to", mobileNumber);
            body.put("type", "template");

            Map<String, Object> template = new HashMap<>();
            template.put("name", templateName);

            Map<String, String> language = new HashMap<>();
            language.put("code", "en_US");
            template.put("language", language);

            java.util.List<Map<String, Object>> parameters = new java.util.ArrayList<>();
            parameters.add(Map.of("type", "text", "text", "#" + order.getId()));
            parameters.add(Map.of("type", "text", "text", order.getUser().getName()));
            parameters.add(Map.of("type", "text", "text", String.format("Rs.%.2f", order.getTotalAmount())));
            parameters.add(Map.of("type", "text", "text", String.valueOf(order.getItems().size())));
            parameters.add(Map.of("type", "text", "text", order.getAddress()));
            parameters.add(Map.of("type", "text", "text", order.getStatus().toString()));

            template.put("components", java.util.List.of(Map.of(
                "type", "body",
                "parameters", parameters
            )));

            body.put("template", template);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ WhatsApp notification sent to: {}", mobileNumber);
            } else {
                log.error("❌ WhatsApp notification failed for: {}. Response: {}", mobileNumber, response.getBody());
            }

        } catch (Exception e) {
            log.error("❌ WhatsApp error for: {}: {}", mobileNumber, e.getMessage());
        }
    }
}
