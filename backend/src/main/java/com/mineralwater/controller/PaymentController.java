package com.mineralwater.controller;

import com.mineralwater.service.OrderService;
import com.mineralwater.service.PaymentService;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final OrderService orderService;
    private final com.mineralwater.service.NotificationService notificationService;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/create-intent")
    public ResponseEntity<Map<String, Object>> createPaymentIntent(@RequestBody Map<String, Object> request) {
        Double amount = Double.valueOf(request.get("amount").toString());
        Map<String, Object> intentData = paymentService.createPaymentIntent(amount);
        return ResponseEntity.ok(intentData);
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String sigHeader) {

        // Support mock Webhook simulation for local testing/dev environments
        if (sigHeader == null || sigHeader.isBlank() || "mock_signature".equalsIgnoreCase(sigHeader)) {
            log.info("Received mock Stripe Webhook call with payload: {}", payload);
            try {
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Object> jsonMap = mapper.readValue(payload, Map.class);
                String eventType = (String) jsonMap.get("type");
                if ("payment_intent.succeeded".equals(eventType)) {
                    Map<String, Object> data = (Map<String, Object>) jsonMap.get("data");
                    Map<String, Object> dataObject = (Map<String, Object>) data.get("object");
                    String piId = (String) dataObject.get("id");
                    com.mineralwater.model.Order order = orderService.confirmPayment(piId);
                    log.info("Mock payment success verified for intent ID: {}", piId);
                    try {
                        notificationService.sendOrderNotification(order);
                    } catch (Exception e) {
                        log.error("Failed to send WhatsApp notification: {}", e.getMessage());
                    }
                }
                return ResponseEntity.ok(Map.of("status", "success", "message", "Mock Webhook handled successfully"));
            } catch (Exception e) {
                log.error("Failed to process mock Stripe webhook payload", e);
                return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Invalid mock payload format"));
            }
        }

        try {
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            if ("payment_intent.succeeded".equals(event.getType())) {
                PaymentIntent paymentIntent = (PaymentIntent) event.getDataObjectDeserializer().getObject().orElse(null);
                if (paymentIntent != null) {
                    com.mineralwater.model.Order order = orderService.confirmPayment(paymentIntent.getId());
                    log.info("Stripe payment verified and finalized for: {}", paymentIntent.getId());
                    try {
                        notificationService.sendOrderNotification(order);
                    } catch (Exception e) {
                        log.error("Failed to send WhatsApp notification: {}", e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(Map.of("status", "success", "message", "Webhook handled successfully"));

        } catch (com.stripe.exception.SignatureVerificationException e) {
            log.error("Stripe webhook signature verification failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "error", "message", "Invalid signature"));
        } catch (Exception e) {
            log.error("Error processing Stripe webhook: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("status", "error", "message", "Webhook handler error"));
        }
    }
}
