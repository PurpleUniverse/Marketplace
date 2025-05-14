package org.example.marketplace.service.command;

import org.example.marketplace.domain.Order;
import org.example.marketplace.domain.Review;
import org.example.marketplace.domain.User;
import org.example.marketplace.dto.ReviewDTO;
import org.example.marketplace.repository.OrderRepository;
import org.example.marketplace.repository.ReviewRepository;
import org.example.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewCommandService {

    private final ReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    /**
     * Create a review for a seller with transaction support to update seller ratings
     * @param reviewDTO The review data
     * @return The created review
     */
    @Transactional
    public ReviewDTO createReview(ReviewDTO reviewDTO) {
        log.info("Creating review for order: {}", reviewDTO.getOrderId());

        // Check if order exists and is delivered
        Order order = orderRepository.findById(reviewDTO.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + reviewDTO.getOrderId()));

        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new IllegalStateException("Cannot review order with status: " + order.getStatus());
        }

        // Check if review already exists for this order
        if (reviewRepository.findByOrderId(reviewDTO.getOrderId()).isPresent()) {
            throw new IllegalStateException("Review already exists for order: " + reviewDTO.getOrderId());
        }

        // Set buyer and seller IDs from order
        reviewDTO.setBuyerId(order.getBuyerId());
        reviewDTO.setSellerId(order.getSellerId());

        // Validate the buyer is the one creating the review
        if (!reviewDTO.getBuyerId().equals(reviewDTO.getBuyerId())) {
            throw new IllegalArgumentException("Only the buyer can review this order");
        }

        // Mark as verified since we've confirmed it's from a real order
        reviewDTO.setVerified(true);

        // Convert to entity and save
        Review review = convertToEntity(reviewDTO);
        Review savedReview = reviewRepository.save(review);

        // Update seller's average rating
        updateSellerRating(order.getSellerId());

        return convertToDto(savedReview);
    }

    /**
     * Update a review
     * @param id The review ID
     * @param reviewDTO The updated review data
     * @return The updated review
     */
    @Transactional
    public Optional<ReviewDTO> updateReview(String id, ReviewDTO reviewDTO) {
        log.info("Updating review: {}", id);

        return reviewRepository.findById(id)
                .map(existingReview -> {
                    // Only allow updating rating and comment
                    existingReview.setRating(reviewDTO.getRating());
                    existingReview.setComment(reviewDTO.getComment());

                    Review updatedReview = reviewRepository.save(existingReview);

                    // Update seller's average rating
                    updateSellerRating(existingReview.getSellerId());

                    return convertToDto(updatedReview);
                });
    }

    /**
     * Delete a review
     * @param id The review ID
     * @return true if successful, false if not found
     */
    @Transactional
    public boolean deleteReview(String id) {
        log.info("Deleting review: {}", id);

        return reviewRepository.findById(id)
                .map(review -> {
                    String sellerId = review.getSellerId();
                    reviewRepository.deleteById(id);

                    // Update seller's average rating
                    updateSellerRating(sellerId);

                    return true;
                })
                .orElse(false);
    }

    /**
     * Update a seller's average rating based on all reviews
     * @param sellerId The seller ID
     */
    private void updateSellerRating(String sellerId) {
        ReviewRepository.SellerRating sellerRating = reviewRepository.getSellerRating(sellerId);

        if (sellerRating != null) {
            userRepository.findById(sellerId).ifPresent(seller -> {
                seller.setAverageRating(sellerRating.getAverageRating());
                seller.setTotalReviews(sellerRating.getCount());
                userRepository.save(seller);
            });
        } else {
            // No reviews left, reset rating
            userRepository.findById(sellerId).ifPresent(seller -> {
                seller.setAverageRating(null);
                seller.setTotalReviews(0);
                userRepository.save(seller);
            });
        }
    }

    /**
     * Convert DTO to entity
     */
    private Review convertToEntity(ReviewDTO dto) {
        return Review.builder()
                .id(dto.getId())
                .sellerId(dto.getSellerId())
                .buyerId(dto.getBuyerId())
                .orderId(dto.getOrderId())
                .rating(dto.getRating())
                .comment(dto.getComment())
                .verified(dto.getVerified())
                .build();
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
