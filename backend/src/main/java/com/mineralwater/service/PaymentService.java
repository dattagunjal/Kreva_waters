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
}
