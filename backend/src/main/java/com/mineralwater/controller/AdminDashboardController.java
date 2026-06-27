package com.mineralwater.controller;

import com.mineralwater.model.Order;
import com.mineralwater.model.User;
import com.mineralwater.repository.OrderRepository;
import com.mineralwater.repository.ProductRepository;
import com.mineralwater.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        long totalUsers = userRepository.count();
        long totalProducts = productRepository.count();

        List<Order> orders = orderRepository.findAll();
        long totalOrders = orders.size();

        long pendingOrders = orders.stream().filter(o -> o.getStatus() == Order.Status.PENDING).count();
        long deliveredOrders = orders.stream().filter(o -> o.getStatus() == Order.Status.DELIVERED).count();
        long cancelledOrders = orders.stream().filter(o -> o.getStatus() == Order.Status.CANCELLED).count();

        double totalRevenue = orders.stream()
                .filter(o -> o.getStatus() == Order.Status.DELIVERED)
                .mapToDouble(Order::getTotalAmount)
                .sum();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalOrders", totalOrders);
        stats.put("totalRevenue", totalRevenue);
        stats.put("totalProducts", totalProducts);
        stats.put("pendingOrders", pendingOrders);
        stats.put("deliveredOrders", deliveredOrders);
        stats.put("cancelledOrders", cancelledOrders);

        return ResponseEntity.ok(stats);
    }

    // User Management CRUD for Admin
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<User> updateRole(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));
        String roleStr = body.get("role");
        if (roleStr != null) {
            user.setRole(User.Role.valueOf(roleStr.toUpperCase()));
        }
        return ResponseEntity.ok(userRepository.save(user));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found: " + id);
        }
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
