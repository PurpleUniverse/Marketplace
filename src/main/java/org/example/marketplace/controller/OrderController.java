package org.example.marketplace.controller;

import org.example.marketplace.domain.Order;
import org.example.marketplace.dto.OrderDTO;
import org.example.marketplace.service.command.OrderCommandService;
import org.example.marketplace.service.query.OrderQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

        import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;

    /**
     * Create a new order
     */
    @PostMapping
    public ResponseEntity<OrderDTO> createOrder(@RequestBody @Valid OrderDTO orderDTO) {
        OrderDTO created = orderCommandService.createOrder(orderDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get an order by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getOrderById(@PathVariable String id) {
        return orderQueryService.getOrderById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update order status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderDTO> updateOrderStatus(
            @PathVariable String id,
            @RequestParam Order.OrderStatus status) {

        return orderCommandService.updateOrderStatus(id, status)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update payment details
     */
    @PatchMapping("/{id}/payment")
    public ResponseEntity<OrderDTO> updatePaymentDetails(
            @PathVariable String id,
            @RequestBody @Valid OrderDTO.PaymentDetailsDTO paymentDetails) {

        return orderCommandService.updatePaymentDetails(id, paymentDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update shipping details
     */
    @PatchMapping("/{id}/shipping")
    public ResponseEntity<OrderDTO> updateShippingDetails(
            @PathVariable String id,
            @RequestBody @Valid OrderDTO.ShippingDetailsDTO shippingDetails) {

        return orderCommandService.updateShippingDetails(id, shippingDetails)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cancel an order
     */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDTO> cancelOrder(@PathVariable String id) {
        return orderCommandService.cancelOrder(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Mark an order as delivered
     */
    @PostMapping("/{id}/deliver")
    public ResponseEntity<OrderDTO> markAsDelivered(@PathVariable String id) {
        return orderCommandService.markAsDelivered(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get orders by buyer
     */
    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<Page<OrderDTO>> getOrdersByBuyer(
            @PathVariable String buyerId,
            Pageable pageable) {

        return ResponseEntity.ok(orderQueryService.getOrdersByBuyer(buyerId, pageable));
    }

    /**
     * Get orders by seller
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<OrderDTO>> getOrdersBySeller(
            @PathVariable String sellerId,
            Pageable pageable) {

        return ResponseEntity.ok(orderQueryService.getOrdersBySeller(sellerId, pageable));
    }

    /**
     * Get orders by buyer and status
     */
    @GetMapping("/buyer/{buyerId}/status/{status}")
    public ResponseEntity<Page<OrderDTO>> getOrdersByBuyerAndStatus(
            @PathVariable String buyerId,
            @PathVariable Order.OrderStatus status,
            Pageable pageable) {

        return ResponseEntity.ok(orderQueryService.getOrdersByBuyerAndStatus(buyerId, status, pageable));
    }

    /**
     * Get orders by seller and status
     */
    @GetMapping("/seller/{sellerId}/status/{status}")
    public ResponseEntity<Page<OrderDTO>> getOrdersBySellerAndStatus(
            @PathVariable String sellerId,
            @PathVariable Order.OrderStatus status,
            Pageable pageable) {

        return ResponseEntity.ok(orderQueryService.getOrdersBySellerAndStatus(sellerId, status, pageable));
    }

    /**
     * Get orders for a listing
     */
    @GetMapping("/listing/{listingId}")
    public ResponseEntity<List<OrderDTO>> getOrdersForListing(@PathVariable String listingId) {
        return ResponseEntity.ok(orderQueryService.getOrdersForListing(listingId));
    }
}
