package org.example.marketplace.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "orders")
public class Order {

    @Id
    private String id;

    private String buyerId;

    private String sellerId;

    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private BigDecimal totalAmount;

    private String currency;

    private ShippingDetails shippingDetails;

    private PaymentDetails paymentDetails;

    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItem {
        private String listingId;
        private String title;
        private Integer quantity;
        private BigDecimal pricePerUnit;
        private String imageUrl;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ShippingDetails {
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
    public static class PaymentDetails {
        private String paymentMethod;
        private String transactionId;
        private PaymentStatus status;
        private LocalDateTime paidAt;
    }

    public enum OrderStatus {
        PENDING, PAID, SHIPPED, DELIVERED, CANCELLED, REFUNDED
    }

    public enum PaymentStatus {
        PENDING, COMPLETED, FAILED, REFUNDED
    }
}
