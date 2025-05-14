package org.example.marketplace.service.command;

import org.example.marketplace.domain.Listing;
import org.example.marketplace.domain.Order;
import org.example.marketplace.dto.OrderDTO;
import org.example.marketplace.repository.ListingRepository;
import org.example.marketplace.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final ListingRepository listingRepository;
    private final ListingCommandService listingCommandService;

    /**
     * Create a new order with transaction support to ensure listing and order are updated atomically
     * @param orderDTO The order data
     * @return The created order
     */
    @Transactional
    public OrderDTO createOrder(OrderDTO orderDTO) {
        log.info("Creating order for buyer: {}", orderDTO.getBuyerId());

        // Validate all items and compute total
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderDTO.OrderItemDTO> validatedItems = new ArrayList<>();

        // Process each item in the order
        for (OrderDTO.OrderItemDTO itemDTO : orderDTO.getItems()) {
            Listing listing = listingRepository.findById(itemDTO.getListingId())
                    .orElseThrow(() -> new IllegalArgumentException("Listing not found: " + itemDTO.getListingId()));

            // Check if listing is active and has enough quantity
            if (listing.getStatus() != Listing.ListingStatus.ACTIVE) {
                throw new IllegalStateException("Listing is not active: " + listing.getId());
            }

            if (listing.getQuantity() < itemDTO.getQuantity()) {
                throw new IllegalStateException("Not enough quantity available for: " + listing.getTitle());
            }

            // Update the item with listing details
            OrderDTO.OrderItemDTO validatedItem = OrderDTO.OrderItemDTO.builder()
                    .listingId(listing.getId())
                    .title(listing.getTitle())
                    .quantity(itemDTO.getQuantity())
                    .pricePerUnit(listing.getPrice())
                    .imageUrl(!listing.getImageUrls().isEmpty() ? listing.getImageUrls().get(0) : null)
                    .build();

            validatedItems.add(validatedItem);

            // Add to total
            BigDecimal itemTotal = listing.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);

            // Update listing quantity
            listing.setQuantity(listing.getQuantity() - itemDTO.getQuantity());

            // If quantity becomes 0, mark as sold
            if (listing.getQuantity() <= 0) {
                listing.setStatus(Listing.ListingStatus.SOLD);
            }

            // Save the updated listing
            listingRepository.save(listing);
        }

        // Set validated items and total
        orderDTO.setItems(validatedItems);
        orderDTO.setTotalAmount(totalAmount);
        orderDTO.setCurrency(validatedItems.get(0).getPricePerUnit().equals(BigDecimal.ZERO) ? "EUR" : "EUR"); // Default to EUR
        orderDTO.setStatus(Order.OrderStatus.PENDING);

        // Create payment details if not present
        if (orderDTO.getPaymentDetails() == null) {
            orderDTO.setPaymentDetails(OrderDTO.PaymentDetailsDTO.builder()
                    .status(Order.PaymentStatus.PENDING)
                    .build());
        }

        // Convert DTO to entity and save
        Order order = convertToEntity(orderDTO);
        Order savedOrder = orderRepository.save(order);

        return convertToDto(savedOrder);
    }

    /**
     * Update order status
     * @param id The order ID
     * @param status The new status
     * @return The updated order
     */
    @Transactional
    public Optional<OrderDTO> updateOrderStatus(String id, Order.OrderStatus status) {
        log.info("Updating order status: {} to {}", id, status);

        return orderRepository.findById(id)
                .map(order -> {
                    order.setStatus(status);
                    Order updatedOrder = orderRepository.save(order);
                    return convertToDto(updatedOrder);
                });
    }

    /**
     * Update payment details
     * @param id The order ID
     * @param paymentDetails The payment details
     * @return The updated order
     */
    @Transactional
    public Optional<OrderDTO> updatePaymentDetails(String id, OrderDTO.PaymentDetailsDTO paymentDetails) {
        log.info("Updating payment details for order: {}", id);

        return orderRepository.findById(id)
                .map(order -> {
                    Order.PaymentDetails entityPaymentDetails = Order.PaymentDetails.builder()
                            .paymentMethod(paymentDetails.getPaymentMethod())
                            .transactionId(paymentDetails.getTransactionId())
                            .status(paymentDetails.getStatus())
                            .paidAt(paymentDetails.getStatus() == Order.PaymentStatus.COMPLETED ?
                                    LocalDateTime.now() : null)
                            .build();

                    order.setPaymentDetails(entityPaymentDetails);

                    // If payment completed, update order status
                    if (paymentDetails.getStatus() == Order.PaymentStatus.COMPLETED &&
                            order.getStatus() == Order.OrderStatus.PENDING) {
                        order.setStatus(Order.OrderStatus.PAID);
                    }

                    Order updatedOrder = orderRepository.save(order);
                    return convertToDto(updatedOrder);
                });
    }

    /**
     * Update shipping details
     * @param id The order ID
     * @param shippingDetails The shipping details
     * @return The updated order
     */
    @Transactional
    public Optional<OrderDTO> updateShippingDetails(String id, OrderDTO.ShippingDetailsDTO shippingDetails) {
        log.info("Updating shipping details for order: {}", id);

        return orderRepository.findById(id)
                .map(order -> {
                    Order.ShippingDetails entityShippingDetails = Order.ShippingDetails.builder()
                            .recipientName(shippingDetails.getRecipientName())
                            .address(shippingDetails.getAddress())
                            .city(shippingDetails.getCity())
                            .state(shippingDetails.getState())
                            .zipCode(shippingDetails.getZipCode())
                            .country(shippingDetails.getCountry())
                            .trackingNumber(shippingDetails.getTrackingNumber())
                            .carrier(shippingDetails.getCarrier())
                            .estimatedDelivery(shippingDetails.getEstimatedDelivery())
                            .build();

                    order.setShippingDetails(entityShippingDetails);

                    // If tracking is provided, update order status to shipped
                    if (shippingDetails.getTrackingNumber() != null &&
                            !shippingDetails.getTrackingNumber().isEmpty() &&
                            order.getStatus() == Order.OrderStatus.PAID) {
                        order.setStatus(Order.OrderStatus.SHIPPED);
                    }

                    Order updatedOrder = orderRepository.save(order);
                    return convertToDto(updatedOrder);
                });
    }

    /**
     * Cancel an order and restore listing quantities
     * @param id The order ID
     * @return The canceled order
     */
    @Transactional
    public Optional<OrderDTO> cancelOrder(String id) {
        log.info("Canceling order: {}", id);

        return orderRepository.findById(id)
                .map(order -> {
                    // Only allow cancellation of pending or paid orders
                    if (order.getStatus() != Order.OrderStatus.PENDING &&
                            order.getStatus() != Order.OrderStatus.PAID) {
                        throw new IllegalStateException("Cannot cancel order with status: " + order.getStatus());
                    }

                    // Restore quantities for all items
                    for (Order.OrderItem item : order.getItems()) {
                        listingRepository.findById(item.getListingId()).ifPresent(listing -> {
                            // Restore quantity
                            listing.setQuantity(listing.getQuantity() + item.getQuantity());

                            // If listing was marked as sold and is being restored, mark as active
                            if (listing.getStatus() == Listing.ListingStatus.SOLD) {
                                listing.setStatus(Listing.ListingStatus.ACTIVE);
                            }

                            listingRepository.save(listing);
                        });
                    }

                    // Update order status
                    order.setStatus(Order.OrderStatus.CANCELLED);
                    if (order.getPaymentDetails() != null &&
                            order.getPaymentDetails().getStatus() == Order.PaymentStatus.COMPLETED) {
                        order.getPaymentDetails().setStatus(Order.PaymentStatus.REFUNDED);
                    }

                    Order updatedOrder = orderRepository.save(order);
                    return convertToDto(updatedOrder);
                });
    }

    /**
     * Mark an order as delivered
     * @param id The order ID
     * @return The updated order
     */
    @Transactional
    public Optional<OrderDTO> markAsDelivered(String id) {
        log.info("Marking order as delivered: {}", id);

        return orderRepository.findById(id)
                .map(order -> {
                    // Only shipped orders can be marked as delivered
                    if (order.getStatus() != Order.OrderStatus.SHIPPED) {
                        throw new IllegalStateException("Cannot mark as delivered order with status: " + order.getStatus());
                    }

                    order.setStatus(Order.OrderStatus.DELIVERED);
                    Order updatedOrder = orderRepository.save(order);
                    return convertToDto(updatedOrder);
                });
    }

    /**
     * Convert DTO to entity
     */
    private Order convertToEntity(OrderDTO dto) {
        // Convert order items
        List<Order.OrderItem> orderItems = dto.getItems().stream()
                .map(itemDto -> Order.OrderItem.builder()
                        .listingId(itemDto.getListingId())
                        .title(itemDto.getTitle())
                        .quantity(itemDto.getQuantity())
                        .pricePerUnit(itemDto.getPricePerUnit())
                        .imageUrl(itemDto.getImageUrl())
                        .build())
                .collect(Collectors.toList());

        // Convert shipping details if present
        Order.ShippingDetails shippingDetails = null;
        if (dto.getShippingDetails() != null) {
            shippingDetails = Order.ShippingDetails.builder()
                    .recipientName(dto.getShippingDetails().getRecipientName())
                    .address(dto.getShippingDetails().getAddress())
                    .city(dto.getShippingDetails().getCity())
                    .state(dto.getShippingDetails().getState())
                    .zipCode(dto.getShippingDetails().getZipCode())
                    .country(dto.getShippingDetails().getCountry())
                    .trackingNumber(dto.getShippingDetails().getTrackingNumber())
                    .carrier(dto.getShippingDetails().getCarrier())
                    .estimatedDelivery(dto.getShippingDetails().getEstimatedDelivery())
                    .build();
        }

        // Convert payment details if present
        Order.PaymentDetails paymentDetails = null;
        if (dto.getPaymentDetails() != null) {
            paymentDetails = Order.PaymentDetails.builder()
                    .paymentMethod(dto.getPaymentDetails().getPaymentMethod())
                    .transactionId(dto.getPaymentDetails().getTransactionId())
                    .status(dto.getPaymentDetails().getStatus())
                    .paidAt(dto.getPaymentDetails().getPaidAt())
                    .build();
        }

        return Order.builder()
                .id(dto.getId())
                .buyerId(dto.getBuyerId())
                .sellerId(dto.getSellerId())
                .items(orderItems)
                .totalAmount(dto.getTotalAmount())
                .currency(dto.getCurrency())
                .shippingDetails(shippingDetails)
                .paymentDetails(paymentDetails)
                .status(dto.getStatus())
                .build();
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
