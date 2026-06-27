package com.mineralwater.controller;

import com.mineralwater.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<Map<String, Object>> createOrder(@RequestBody Map<String, Object> request) {
        Double amount = Double.valueOf(request.get("amount").toString());
        Map<String, Object> orderData = paymentService.createRazorpayOrder(amount);
        return ResponseEntity.ok(orderData);
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyPayment(@RequestBody Map<String, String> request) {
        boolean verified = paymentService.verifyPayment(
                request.get("razorpay_order_id"),
                request.get("razorpay_payment_id"),
                request.get("razorpay_signature")
        );
        if (verified) {
            return ResponseEntity.ok(Map.of("status", "success", "message", "Payment verified successfully"));
        } else {
            return ResponseEntity.badRequest().body(Map.of("status", "failed", "message", "Payment verification failed"));
        }
    }
}
