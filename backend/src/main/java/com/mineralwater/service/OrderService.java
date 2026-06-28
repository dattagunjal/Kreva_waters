package com.mineralwater.service;

import com.mineralwater.dto.OrderDto;
import com.mineralwater.model.*;
import com.mineralwater.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    private User findUserByPrincipal(String principal) {
        if (EMAIL_PATTERN.matcher(principal).matches()) {
            return userRepository.findByEmail(principal)
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        return userRepository.findByMobileNumber(principal)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public Order placeOrder(String principal, OrderDto dto) {
        User user = findUserByPrincipal(principal);

        if (dto.getItems() == null || dto.getItems().isEmpty())
            throw new RuntimeException("Order must contain at least one item.");

        Order order = Order.builder()
                .user(user)
                .address(dto.getAddress())
                .status(Order.Status.PENDING)
                .build();

        List<OrderItem> items = dto.getItems().stream().map(itemDto -> {
            if (itemDto.getProductId() == null)
                throw new RuntimeException("Invalid item: missing product ID.");

            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found: ID " + itemDto.getProductId()));

            int qty = itemDto.getQuantity() == null ? 1 : itemDto.getQuantity();
            if (qty < 1)
                throw new RuntimeException("Quantity must be at least 1 for: " + product.getName());

            if (product.getStock() < qty)
                throw new RuntimeException("Insufficient stock for: " + product.getName()
                        + " (available: " + product.getStock() + ")");

            product.setStock(product.getStock() - qty);
            productRepository.save(product);

            return OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(qty)
                    .price(product.getPrice())
                    .businessName(itemDto.getBusinessName())
                    .logoUrl(itemDto.getLogoUrl())
                    .tagline(itemDto.getTagline())
                    .contactNumber(itemDto.getContactNumber())
                    .website(itemDto.getWebsite())
                    .notes(itemDto.getNotes())
                    .build();
        }).collect(Collectors.toList());

        double total = items.stream()
                .mapToDouble(i -> i.getPrice() * i.getQuantity()).sum();

        order.setItems(items);
        order.setTotalAmount(total);

        if (dto.getPaymentId() != null && !dto.getPaymentId().isBlank()) {
            order.setPaymentId(dto.getPaymentId());
            order.setPaymentStatus(com.mineralwater.model.Order.PaymentStatus.PENDING);
        }

        return orderRepository.save(order);
    }

    @Transactional
    public Order confirmPayment(String paymentIntentId) {
        Order order = orderRepository.findByPaymentId(paymentIntentId)
                .orElseThrow(() -> new RuntimeException("Order not found for payment ID: " + paymentIntentId));

        if (order.getPaymentStatus() != com.mineralwater.model.Order.PaymentStatus.PAID) {
            order.setPaymentStatus(com.mineralwater.model.Order.PaymentStatus.PAID);
            order = orderRepository.save(order);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public List<Order> getMyOrders(String principal) {
        User user = findUserByPrincipal(principal);
        return orderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Transactional
    public Order updateStatus(Long id, String status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));
        try {
            order.setStatus(Order.Status.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid status value: " + status);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Order cancelOrder(Long id, String principal) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found: " + id));

        if (!order.getUser().getPrincipal().equals(principal))
            throw new RuntimeException("Unauthorized: this order does not belong to you.");

        if (order.getStatus() == Order.Status.CANCELLED) {
            throw new RuntimeException("This order has already been cancelled.");
        }

        if (order.getStatus() == Order.Status.DELIVERED) {
            throw new RuntimeException("This order has already been delivered and cannot be cancelled.");
        }

        if (order.getStatus() != Order.Status.PENDING) {
            throw new RuntimeException("Only PENDING orders can be cancelled. Status: " + order.getStatus());
        }

        // Restore stock
        order.getItems().forEach(item -> {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
        });

        order.setStatus(Order.Status.CANCELLED);
        return orderRepository.save(order);
    }
}
