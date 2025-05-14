package org.example.marketplace.dto;

import org.example.marketplace.domain.Listing;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ListingDTO {

    private String id;

    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price must be positive")
    private BigDecimal price;

    private String currency;

    private String sellerId;

    private String sellerName;

    private Double sellerRating;

    @NotBlank(message = "Category is required")
    private String category;

    @NotNull(message = "Condition is required")
    private Listing.Condition condition;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    private Listing.ListingStatus status;

    private Integer viewCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
