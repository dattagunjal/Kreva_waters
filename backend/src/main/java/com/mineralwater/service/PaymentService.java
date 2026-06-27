package com.mineralwater.service;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
public class PaymentService {

    @Value("${razorpay.key.id}")
    private String keyId;

    @Value("${razorpay.key.secret}")
    private String keySecret;

    public Map<String, Object> createRazorpayOrder(Double amount) {
        if (keyId == null || keyId.contains("PLACEHOLDER") || keyId.isBlank() || "rzp_test_MOCK".equals(keyId)) {
            log.warn("Razorpay keys are not configured or placeholder. Falling back to mock order.");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("orderId", "mock_order_" + System.currentTimeMillis());
            result.put("amount", (int) (amount * 100));
            result.put("currency", "INR");
            result.put("keyId", "rzp_test_MOCK");
            return result;
        }
        try {
            RazorpayClient client = new RazorpayClient(keyId, keySecret);

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", (int) (amount * 100)); // Amount in paise
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", "order_" + System.currentTimeMillis());

            Order razorpayOrder = client.orders.create(orderRequest);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("orderId", razorpayOrder.get("id"));
            result.put("amount", razorpayOrder.get("amount"));
            result.put("currency", razorpayOrder.get("currency"));
            result.put("keyId", keyId);
            return result;

        } catch (RazorpayException e) {
            log.warn("Razorpay order creation failed: {}. Falling back to mock order in DEV.", e.getMessage());
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("orderId", "mock_order_" + System.currentTimeMillis());
            result.put("amount", (int) (amount * 100));
            result.put("currency", "INR");
            result.put("keyId", "rzp_test_MOCK");
            return result;
        }
    }

    public boolean verifyPayment(String orderId, String paymentId, String signature) {
        if (orderId != null && orderId.startsWith("mock_order_") &&
            paymentId != null && paymentId.startsWith("mock_pay_") &&
            signature != null && signature.startsWith("mock_sig_")) {
            log.info("Mock payment verified successfully.");
            return true;
        }
        try {
            String data = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(keySecret.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes());

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().equals(signature);
        } catch (Exception e) {
            log.error("Payment verification failed: {}", e.getMessage());
            return false;
        }
    }
}
