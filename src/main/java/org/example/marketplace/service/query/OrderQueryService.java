package org.example.marketplace.service.query;

import org.example.marketplace.domain.Order;
import org.example.marketplace.dto.OrderDTO;
import org.example.marketplace.repository.OrderRepository;
import org.example.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Get order by ID with buyer and seller names
     * @param id The order ID
     * @return The order if found
     */
    @Cacheable(value = "orders", key = "#id")
    public Optional<OrderDTO> getOrderById(String id) {
        return orderRepository.findById(id)
                .map(this::enrichWithUserInfo);
    }

    /**
     * Get orders by buyer ID
     * @param buyerId The buyer ID
     * @param pageable Pagination information
     * @return Page of orders for the buyer
     */
    @Cacheable(value = "orders", key = "'buyer:' + #buyerId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<OrderDTO> getOrdersByBuyer(String buyerId, Pageable pageable) {
        return orderRepository.findByBuyerId(buyerId, pageable)
                .map(this::enrichWithUserInfo);
    }

    /**
     * Get orders by seller ID
     * @param sellerId The seller ID
     * @param pageable Pagination information
     * @return Page of orders for the seller
     */
    @Cacheable(value = "orders", key = "'seller:' + #sellerId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<OrderDTO> getOrdersBySeller(String sellerId, Pageable pageable) {
        return orderRepository.findBySellerId(sellerId, pageable)
                .map(this::enrichWithUserInfo);
    }

    /**
     * Get orders by buyer ID and status
     * @param buyerId The buyer ID
     * @param status The order status
     * @param pageable Pagination information
     * @return Page of orders for the buyer with the given status
     */
    @Cacheable(value = "orders", key = "'buyer:' + #buyerId + ':status:' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<OrderDTO> getOrdersByBuyerAndStatus(String buyerId, Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findByBuyerIdAndStatus(buyerId, status, pageable)
                .map(this::enrichWithUserInfo);
    }

    /**
     * Get orders by seller ID and status
     * @param sellerId The seller ID
     * @param status The order status
     * @param pageable Pagination information
     * @return Page of orders for the seller with the given status
     */
    @Cacheable(value = "orders", key = "'seller:' + #sellerId + ':status:' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<OrderDTO> getOrdersBySellerAndStatus(String sellerId, Order.OrderStatus status, Pageable pageable) {
        return orderRepository.findBySellerIdAndStatus(sellerId, status, pageable)
                .map(this::enrichWithUserInfo);
    }

    /**
     * Get orders for a specific listing
     * @param listingId The listing ID
     * @return List of orders for the listing
     */
    @Cacheable(value = "orders", key = "'listing:' + #listingId")
    public List<OrderDTO> getOrdersForListing(String listingId) {
        return orderRepository.findByItemsListingId(listingId).stream()
                .map(this::enrichWithUserInfo)
                .collect(Collectors.toList());
    }

    /**
     * Enrich order with buyer and seller information
     * @param order The order to enrich
     * @return The enriched order DTO
     */
    private OrderDTO enrichWithUserInfo(Order order) {
        OrderDTO dto = convertToDto(order);

        // Add buyer name
        userRepository.findById(order.getBuyerId()).ifPresent(buyer -> {
            dto.setBuyerName(buyer.getFirstName() + " " + buyer.getLastName());
        });

        // Add seller name
        userRepository.findById(order.getSellerId()).ifPresent(seller -> {
            dto.setSellerName(seller.getFirstName() + " " + seller.getLastName());
        });

        return dto;
    }

    /**
     * Convert entity to DTO
     */
    private OrderDTO convertToDto(Order entity) {
        // Convert order items
        List<OrderDTO.OrderItemDTO> orderItems = entity.getItems().stream()
                .map(item -> OrderDTO.OrderItemDTO.builder()
                        .listingId(item.getListingId())
                        .title(item.getTitle())
                        .quantity(item.getQuantity())
                        .pricePerUnit(item.getPricePerUnit())
                        .imageUrl(item.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        // Convert shipping details if present
        OrderDTO.ShippingDetailsDTO shippingDetails = null;
        if (entity.getShippingDetails() != null) {
            shippingDetails = OrderDTO.ShippingDetailsDTO.builder()
                    .recipientName(entity.getShippingDetails().getRecipientName())
                    .address(entity.getShippingDetails().getAddress())
                    .city(entity.getShippingDetails().getCity())
                    .state(entity.getShippingDetails().getState())
                    .zipCode(entity.getShippingDetails().getZipCode())
                    .country(entity.getShippingDetails().getCountry())
                    .trackingNumber(entity.getShippingDetails().getTrackingNumber())
                    .carrier(entity.getShippingDetails().getCarrier())
                    .estimatedDelivery(entity.getShippingDetails().getEstimatedDelivery())
                    .build();
        }

        // Convert payment details if present
        OrderDTO.PaymentDetailsDTO paymentDetails = null;
        if (entity.getPaymentDetails() != null) {
            paymentDetails = OrderDTO.PaymentDetailsDTO.builder()
                    .paymentMethod(entity.getPaymentDetails().getPaymentMethod())
                    .transactionId(entity.getPaymentDetails().getTransactionId())
                    .status(entity.getPaymentDetails().getStatus())
                    .paidAt(entity.getPaymentDetails().getPaidAt())
                    .build();
        }

        return OrderDTO.builder()
                .id(entity.getId())
                .buyerId(entity.getBuyerId())
                .sellerId(entity.getSellerId())
                .items(orderItems)
                .totalAmount(entity.getTotalAmount())
                .currency(entity.getCurrency())
                .shippingDetails(shippingDetails)
                .paymentDetails(paymentDetails)
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
