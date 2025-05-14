package org.example.marketplace.dto;

import org.example.marketplace.domain.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private String id;

    private String buyerId;

    private String buyerName;

    private String sellerId;

    private String sellerName;

    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    @Builder.Default
    private List<OrderItemDTO> items = new ArrayList<>();

    private BigDecimal totalAmount;

    private String currency;

    @Valid
    private ShippingDetailsDTO shippingDetails;

    private PaymentDetailsDTO paymentDetails;

    private Order.OrderStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemDTO {
        @NotNull(message = "Listing ID is required")
        private String listingId;
        private String title;
        @NotNull(message = "Quantity is required")
        private Integer quantity;
        private BigDecimal pricePerUnit;
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShippingDetailsDTO {
        private String recipientName;
        private String address;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private String trackingNumber;
        private String carrier;
        private LocalDateTime estimatedDelivery;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentDetailsDTO {
        private String paymentMethod;
        private String transactionId;
        private Order.PaymentStatus status;
        private LocalDateTime paidAt;
    }
}