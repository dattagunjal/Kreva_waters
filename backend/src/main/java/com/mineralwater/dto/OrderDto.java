package com.mineralwater.dto;

import lombok.Data;
import java.util.List;

@Data
public class OrderDto {
    private String address;
    private String paymentMethod;
    private String paymentId;
    private List<OrderItemDto> items;

    @Data
    public static class OrderItemDto {
        private Long productId;
        private Integer quantity;
        private String businessName;
        private String logoUrl;
        private String tagline;
        private String contactNumber;
        private String website;
        private String notes;
    }
}
