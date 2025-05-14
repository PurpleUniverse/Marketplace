package org.example.marketplace.service.query;

import org.example.marketplace.domain.Review;
import org.example.marketplace.dto.ReviewDTO;
import org.example.marketplace.repository.ReviewRepository;
import org.example.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReviewQueryService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    /**
     * Get a review by ID
     * @param id The review ID
     * @return The review if found
     */
    @Cacheable(value = "reviews", key = "#id")
    public Optional<ReviewDTO> getReviewById(String id) {
        return reviewRepository.findById(id)
                .map(this::enrichWithUserInfo);
    }

    /**
     * Get a review by order ID
     * @param orderId The order ID
     * @return The review if found
     */
    @Cacheable(value = "reviews", key = "'order:' + #orderId")
    public Optional<ReviewDTO> getReviewByOrderId(String orderId) {
        return reviewRepository.findByOrderId(orderId)
                .map(this::enrichWithUserInfo);
    }

    /**
     * Get reviews for a seller with pagination
     * @param sellerId The seller ID
     * @param pageable Pagination information
     * @return Page of reviews for the seller
     */
    @Cacheable(value = "reviews", key = "'seller:' + #sellerId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ReviewDTO> getReviewsBySeller(String sellerId, Pageable pageable) {
        return reviewRepository.findBySellerId(sellerId, pageable)
                .map(this::enrichWithUserInfo);
    }

    /**
     * Get reviews by buyer with pagination
     * @param buyerId The buyer ID
     * @param pageable Pagination information
     * @return Page of reviews by the buyer
     */
    @Cacheable(value = "reviews", key = "'buyer:' + #buyerId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ReviewDTO> getReviewsByBuyer(String buyerId, Pageable pageable) {
        return reviewRepository.findByBuyerId(buyerId, pageable)
                .map(this::enrichWithUserInfo);
    }

    /**
     * Get seller rating summary
     * @param sellerId The seller ID
     * @return The seller rating or null if no reviews
     */
    @Cacheable(value = "sellerRatings", key = "#sellerId")
    public ReviewRepository.SellerRating getSellerRating(String sellerId) {
        return reviewRepository.getSellerRating(sellerId);
    }

    /**
     * Enrich a review with user information
     * @param review The review to enrich
     * @return The enriched review DTO
     */
    private ReviewDTO enrichWithUserInfo(Review review) {
        ReviewDTO dto = convertToDto(review);

        // Add buyer name
        userRepository.findById(review.getBuyerId()).ifPresent(buyer -> {
            dto.setBuyerName(buyer.getFirstName() + " " + buyer.getLastName());
        });

        // Add seller name
        userRepository.findById(review.getSellerId()).ifPresent(seller -> {
            dto.setSellerName(seller.getFirstName() + " " + seller.getLastName());
        });

        return dto;
    }

    /**
     * Convert entity to DTO
     */
    private ReviewDTO convertToDto(Review entity) {
        return ReviewDTO.builder()
                .id(entity.getId())
                .sellerId(entity.getSellerId())
                .buyerId(entity.getBuyerId())
                .orderId(entity.getOrderId())
                .rating(entity.getRating())
                .comment(entity.getComment())
                .verified(entity.getVerified())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
