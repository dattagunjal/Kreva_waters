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

    @Value("${fast2sms.api.key}")
    private String apiKey;

    @Value("${admin.mobile}")
    private String adminMobile;

    private static final String FAST2SMS_URL = "https://www.fast2sms.com/dev/bulkV2";
    private static final String FAST2SMS_WHATSAPP_URL = "https://www.fast2sms.com/dev/whatsapp";

    private final RestTemplate restTemplate;

    /**
     * Called when a new order is placed — sends both SMS + WhatsApp
     */
    public void sendOrderNotification(Order order) {
        String message = buildMessage(order);
        sendSMS(message);
        sendWhatsApp(message);
    }

    /**
     * Build notification message
     */
    private String buildMessage(Order order) {
        return String.format(
            "🚿 New Order Alert! - UgamWaters\n" +
            "Order ID  : #%d\n" +
            "Customer  : %s\n" +
            "Amount    : Rs.%.2f\n" +
            "Items     : %d item(s)\n" +
            "Address   : %s\n" +
            "Status    : %s\n" +
            "Login to admin panel to manage this order.",
            order.getId(),
            order.getUser().getName(),
            order.getTotalAmount(),
            order.getItems().size(),
            order.getAddress(),
            order.getStatus()
        );
    }

    /**
     * Send SMS via Fast2SMS
     */
    private void sendSMS(String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("route", "q");           // Quick transactional route
            body.put("message", message);
            body.put("language", "english");
            body.put("flash", 0);
            body.put("numbers", adminMobile);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                FAST2SMS_URL, request, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ SMS sent to admin: {}", adminMobile);
            } else {
                log.error("❌ SMS failed: {}", response.getBody());
            }

        } catch (Exception e) {
            log.error("❌ SMS error: {}", e.getMessage());
        }
    }

    /**
     * Send WhatsApp via Fast2SMS
     */
    private void sendWhatsApp(String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("authorization", apiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("mobile", adminMobile);
            body.put("message", message);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                FAST2SMS_WHATSAPP_URL, request, String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ WhatsApp sent to admin: {}", adminMobile);
            } else {
                log.error("❌ WhatsApp failed: {}", response.getBody());
            }

        } catch (Exception e) {
            log.error("❌ WhatsApp error: {}", e.getMessage());
        }
    }
}
