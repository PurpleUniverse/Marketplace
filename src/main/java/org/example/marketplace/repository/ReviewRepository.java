package org.example.marketplace.repository;

import org.example.marketplace.domain.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends MongoRepository<Review, String> {

    Page<Review> findBySellerId(String sellerId, Pageable pageable);

    Page<Review> findByBuyerId(String buyerId, Pageable pageable);

    Optional<Review> findByOrderId(String orderId);

    @Aggregation(pipeline = {
            "{ $match: { sellerId: ?0 } }",
            "{ $group: { _id: null, averageRating: { $avg: '$rating' }, count: { $sum: 1 } } }"
    })
    SellerRating getSellerRating(String sellerId);

    interface SellerRating {
        Double getAverageRating();
        Integer getCount();
    }
}
