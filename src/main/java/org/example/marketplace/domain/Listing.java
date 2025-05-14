package org.example.marketplace.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
@Document(collection = "listings")
public class Listing {

    @Id
    private String id;

    private String title;

    private String description;

    private BigDecimal price;

    private String currency;

    private String sellerId;

    @Indexed
    private String category;

    private Condition condition;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();

    private Integer quantity;

    @Builder.Default
    private ListingStatus status = ListingStatus.ACTIVE;

    private Integer viewCount;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public enum Condition {
        NEW, LIKE_NEW, VERY_GOOD, GOOD, FAIR, POOR
    }

    public enum ListingStatus {
        DRAFT, ACTIVE, SOLD, INACTIVE, DELETED
    }
}
