package com.mineralwater.controller;

import com.mineralwater.dto.OrderDto;
import com.mineralwater.model.Order;
import com.mineralwater.service.OrderService;
import com.mineralwater.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final NotificationService notificationService;

    @PostMapping
    public ResponseEntity<Order> placeOrder(
            @RequestBody OrderDto dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Order order = orderService.placeOrder(userDetails.getUsername(), dto);

        if ("COD".equalsIgnoreCase(dto.getPaymentMethod())) {
            try {
                notificationService.sendOrderNotification(order);
            } catch (Exception e) {
                log.error("Failed to send order notification for COD order: {}", e.getMessage());
            }
        }

        return ResponseEntity.ok(order);
    }

    @GetMapping("/my")
    public ResponseEntity<List<Order>> getMyOrders(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.getMyOrders(userDetails.getUsername()));
    }

    @PutMapping("/{id}/cancel")
    public ResponseEntity<Order> cancelOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(orderService.cancelOrder(id, userDetails.getUsername()));
    }

    // Admin endpoints
    @GetMapping("/admin/all")
    public ResponseEntity<List<Order>> getAllOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PutMapping("/admin/{id}/status")
    public ResponseEntity<Order> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(orderService.updateStatus(id, body.get("status")));
    }
}
