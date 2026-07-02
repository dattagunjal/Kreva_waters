package com.mineralwater.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * @JsonIgnore on the back-reference to Order prevents:
     *   OrderItem → order → items → OrderItem → …
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    /**
     * Product is safe to serialize — it has no back-reference to OrderItem.
     * We exclude imageUrl to keep the order response payload slim.
     */
    @JsonIgnoreProperties({"imageUrl", "stock"})
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer quantity;

    /** Price captured at time of order — not recalculated from Product. */
    private Double price;

    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String logoUrl;

    private String tagline;
    private String contactNumber;
    private String website;
    private String notes;
}
