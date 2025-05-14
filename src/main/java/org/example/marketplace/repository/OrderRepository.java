package org.example.marketplace.repository;

import org.example.marketplace.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends MongoRepository<Order, String> {

    Page<Order> findByBuyerId(String buyerId, Pageable pageable);

    Page<Order> findBySellerId(String sellerId, Pageable pageable);

    Page<Order> findByBuyerIdAndStatus(String buyerId, Order.OrderStatus status, Pageable pageable);

    Page<Order> findBySellerIdAndStatus(String sellerId, Order.OrderStatus status, Pageable pageable);

    List<Order> findByItemsListingId(String listingId);
}
