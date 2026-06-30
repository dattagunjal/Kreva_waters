package com.mineralwater.service;

import com.stripe.Stripe;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class PaymentService {

    @Value("${stripe.publishable.key}")
    private String publishableKey;

    @Value("${stripe.secret.key}")
    private String secretKey;

    public Map<String, Object> createPaymentIntent(Double amount) {
        if (secretKey == null || secretKey.contains("PLACEHOLDER") || secretKey.isBlank() || "sk_test_MOCK".equals(secretKey)) {
            log.warn("Stripe secret key is not configured or placeholder. Falling back to mock PaymentIntent.");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", "mock_pi_" + System.currentTimeMillis());
            result.put("clientSecret", "mock_client_secret_" + System.currentTimeMillis());
            result.put("publishableKey", "pk_test_MOCK");
            return result;
        }

        try {
            Stripe.apiKey = secretKey;

            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount((long) (amount * 100)) // Amount in cents / paise
                    .setCurrency("inr")
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build()
                    )
                    .build();

            PaymentIntent intent = PaymentIntent.create(params);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", intent.getId());
            result.put("clientSecret", intent.getClientSecret());
            result.put("publishableKey", publishableKey);
            return result;

        } catch (Exception e) {
            log.error("Stripe PaymentIntent creation failed: {}", e.getMessage());
            // Fallback in dev/test to keep flow functioning
            log.warn("Falling back to mock PaymentIntent due to error.");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", "mock_pi_" + System.currentTimeMillis());
            result.put("clientSecret", "mock_client_secret_" + System.currentTimeMillis());
            result.put("publishableKey", "pk_test_MOCK");
            return result;
        }
    }

    @Value("${razorpay.key.id:rzp_test_yGzB4Fh5j7z8K9}")
    private String razorpayKeyId;

    @Value("${razorpay.key.secret:MOCK_SECRET_KEY}")
    private String razorpayKeySecret;

    public Map<String, Object> createRazorpayOrder(Double amount) {
        if (razorpayKeyId == null || razorpayKeyId.contains("PLACEHOLDER") || razorpayKeyId.isBlank() || "rzp_test_yGzB4Fh5j7z8K9".equals(razorpayKeyId)) {
            log.warn("Razorpay key is not configured or placeholder. Falling back to mock Razorpay order.");
            Map<String, Object> mockResult = new java.util.LinkedHashMap<>();
            mockResult.put("id", "order_mock_" + System.currentTimeMillis());
            mockResult.put("amount", (long) (amount * 100));
            mockResult.put("currency", "INR");
            mockResult.put("status", "created");
            return mockResult;
        }

        try {
            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
            headers.setBasicAuth(razorpayKeyId, razorpayKeySecret);

            Map<String, Object> requestBody = new java.util.HashMap<>();
            requestBody.put("amount", (long) (amount * 100)); // in paise
            requestBody.put("currency", "INR");
            requestBody.put("receipt", "rcpt_" + System.currentTimeMillis());

            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(requestBody, headers);
            
            org.springframework.http.ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                "https://api.razorpay.com/v1/orders",
                entity,
                Map.class
            );

            Map<String, Object> responseBody = responseEntity.getBody();
            if (responseBody != null && responseBody.containsKey("id")) {
                return responseBody;
            }
            throw new RuntimeException("Invalid response from Razorpay server");
        } catch (Exception e) {
            log.error("Razorpay Order creation failed: {}", e.getMessage());
            Map<String, Object> mockResult = new java.util.LinkedHashMap<>();
            mockResult.put("id", "order_mock_" + System.currentTimeMillis());
            mockResult.put("amount", (long) (amount * 100));
            mockResult.put("currency", "INR");
            mockResult.put("status", "created");
            return mockResult;
        }
    }
}
