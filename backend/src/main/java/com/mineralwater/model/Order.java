package com.mineralwater.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Order {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Hidden from JSON — we never want to serialize the full User graph
     * (it would expose password hash and cause circular refs with future
     * Order collections on the User entity).
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * @JsonIgnoreProperties breaks the circular link:
     *   Order → OrderItem.order → Order → …
     * We tell Jackson: when serializing the items list, ignore the
     * back-reference field named "order" on each OrderItem.
     */
    @JsonIgnoreProperties("order")
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items;

    @Column(nullable = false)
    private Double totalAmount;

    @Column(nullable = false)
    private String address;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @Column
    private String paymentId;

    @Column
    private String paymentMethod;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    /**
     * Use @PrePersist so the timestamp is set by Hibernate at INSERT time,
     * not at object construction time (builder pattern skips field initialisers).
     */
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = java.time.ZonedDateTime.now(java.time.ZoneId.of("Asia/Kolkata")).toLocalDateTime();
        }
    }

    public enum Status { PENDING, CONFIRMED, DELIVERED, CANCELLED }

    public enum PaymentStatus { PENDING, PAID, FAILED }
}
