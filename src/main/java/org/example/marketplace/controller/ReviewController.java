package org.example.marketplace.controller;

import org.example.marketplace.dto.ReviewDTO;
import org.example.marketplace.repository.ReviewRepository;
import org.example.marketplace.service.command.ReviewCommandService;
import org.example.marketplace.service.query.ReviewQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewCommandService reviewCommandService;
    private final ReviewQueryService reviewQueryService;

    /**
     * Create a new review
     */
    @PostMapping
    public ResponseEntity<ReviewDTO> createReview(@RequestBody @Valid ReviewDTO reviewDTO) {
        ReviewDTO created = reviewCommandService.createReview(reviewDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a review by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ReviewDTO> getReviewById(@PathVariable String id) {
        return reviewQueryService.getReviewById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get a review by order ID
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ReviewDTO> getReviewByOrderId(@PathVariable String orderId) {
        return reviewQueryService.getReviewByOrderId(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a review
     */
    @PutMapping("/{id}")
    public ResponseEntity<ReviewDTO> updateReview(
            @PathVariable String id,
            @RequestBody @Valid ReviewDTO reviewDTO) {

        return reviewCommandService.updateReview(id, reviewDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a review
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReview(@PathVariable String id) {
        boolean deleted = reviewCommandService.deleteReview(id);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Get reviews for a seller
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsBySeller(
            @PathVariable String sellerId,
            Pageable pageable) {

        return ResponseEntity.ok(reviewQueryService.getReviewsBySeller(sellerId, pageable));
    }

    /**
     * Get reviews by a buyer
     */
    @GetMapping("/buyer/{buyerId}")
    public ResponseEntity<Page<ReviewDTO>> getReviewsByBuyer(
            @PathVariable String buyerId,
            Pageable pageable) {

        return ResponseEntity.ok(reviewQueryService.getReviewsByBuyer(buyerId, pageable));
    }

    /**
     * Get seller rating summary
     */
    @GetMapping("/seller/{sellerId}/rating")
    public ResponseEntity<ReviewRepository.SellerRating> getSellerRating(
            @PathVariable String sellerId) {

        ReviewRepository.SellerRating rating = reviewQueryService.getSellerRating(sellerId);
        return rating != null ? ResponseEntity.ok(rating) : ResponseEntity.notFound().build();
    }
}
