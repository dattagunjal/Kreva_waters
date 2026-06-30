package com.mineralwater.controller;

import com.mineralwater.model.Enquiry;
import com.mineralwater.repository.EnquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contact")
@RequiredArgsConstructor
@Slf4j
public class ContactController {

    private final EnquiryRepository enquiryRepository;

    @Value("${brevo.api.key:}")
    private String brevoApiKey;

    @Value("${brevo.sender.email:info@ugamwaters.in}")
    private String brevoSenderEmail;

    @Value("${contact.notification.email:dattagunjal286@gmail.com}")
    private String contactNotificationEmail;

    @PostMapping
    public ResponseEntity<?> submitEnquiry(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String emailOrMobile = payload.get("emailOrMobile");
        String subject = payload.get("subject");
        String message = payload.get("message");

        if (name == null || name.isBlank() ||
                emailOrMobile == null || emailOrMobile.isBlank() ||
                subject == null || subject.isBlank() ||
                message == null || message.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "All fields are required"));
        }

        Enquiry enquiry = Enquiry.builder()
                .name(name)
                .emailOrMobile(emailOrMobile)
                .subject(subject)
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        enquiryRepository.save(enquiry);

        // Send Email Notification to Admin
        sendEmailNotification(enquiry);

        return ResponseEntity.ok(Map.of("message", "Enquiry submitted successfully"));
    }

    private void sendEmailNotification(Enquiry enquiry) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.warn("[Contact Notification Stub] Brevo API key is not configured. Simulating email notification for enquiry from: {}", enquiry.getName());
            return;
        }

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String url = "https://api.brevo.com/v3/smtp/email";

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("api-key", brevoApiKey);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("sender", Map.of("name", "Ugam Waters Contact", "email", brevoSenderEmail));
            body.put("to", List.of(Map.of("email", contactNotificationEmail)));
            body.put("subject", "New Contact Enquiry: " + enquiry.getSubject());
            
            String textContent = String.format(
                "You have received a new contact enquiry on Ugam Waters.\n\n" +
                "Name: %s\n" +
                "Email/Mobile: %s\n" +
                "Subject: %s\n" +
                "Message:\n%s\n\n" +
                "Date: %s",
                enquiry.getName(),
                enquiry.getEmailOrMobile(),
                enquiry.getSubject(),
                enquiry.getMessage(),
                enquiry.getCreatedAt().toString()
            );
            body.put("textContent", textContent);

            org.springframework.http.HttpEntity<Map<String, Object>> request = new org.springframework.http.HttpEntity<>(body, headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ Contact enquiry email notification successfully sent to: {}", contactNotificationEmail);
            } else {
                log.error("❌ Contact enquiry email notification failed. Response: {}", response.getBody());
            }
        } catch (Exception e) {
            log.error("❌ Error sending contact enquiry email notification", e);
        }
    }
}
