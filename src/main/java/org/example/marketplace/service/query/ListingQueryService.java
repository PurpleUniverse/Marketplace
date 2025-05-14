package org.example.marketplace.service.query;

import org.example.marketplace.domain.Listing;
import org.example.marketplace.dto.ListingDTO;
import org.example.marketplace.repository.ListingRepository;
import org.example.marketplace.repository.ReviewRepository;
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
public class ListingQueryService {

    private final ListingRepository listingRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    /**
     * Get a listing by ID with seller information
     * @param id The listing ID
     * @return The listing, if found
     */
    @Cacheable(value = "listings", key = "#id")
    public Optional<ListingDTO> getListingById(String id) {
        return listingRepository.findById(id)
                .map(this::enrichWithSellerInfo);
    }

    /**
     * Get active listings with pagination
     * @param pageable Pagination information
     * @return Page of active listings
     */
    @Cacheable(value = "listings", key = "'active:' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ListingDTO> getActiveListings(Pageable pageable) {
        return listingRepository.findByStatus(Listing.ListingStatus.ACTIVE, pageable)
                .map(this::enrichWithSellerInfo);
    }

    /**
     * Get listings by seller ID with pagination
     * @param sellerId The seller ID
     * @param pageable Pagination information
     * @return Page of listings by seller
     */
    @Cacheable(value = "listings", key = "'seller:' + #sellerId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ListingDTO> getListingsBySeller(String sellerId, Pageable pageable) {
        return listingRepository.findBySellerId(sellerId, pageable)
                .map(this::enrichWithSellerInfo);
    }

    /**
     * Get listings by category with pagination
     * @param category The category
     * @param pageable Pagination information
     * @return Page of listings in category
     */
    @Cacheable(value = "listings", key = "'category:' + #category + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ListingDTO> getListingsByCategory(String category, Pageable pageable) {
        return listingRepository.findByCategory(category, pageable)
                .map(this::enrichWithSellerInfo);
    }

    /**
     * Search listings by title with pagination
     * @param query The search query
     * @param pageable Pagination information
     * @return Page of matching listings
     */
    @Cacheable(value = "searchResults", key = "'title:' + #query + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ListingDTO> searchListingsByTitle(String query, Pageable pageable) {
        return listingRepository.findByTitleContainingIgnoreCase(query, pageable)
                .map(this::enrichWithSellerInfo);
    }

    /**
     * Search listings by attribute with pagination
     * @param attributeName The attribute name
     * @param attributeValue The attribute value
     * @param pageable Pagination information
     * @return Page of matching listings
     */
    @Cacheable(value = "searchResults", key = "'attr:' + #attributeName + ':' + #attributeValue + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    public Page<ListingDTO> searchListingsByAttribute(String attributeName, Object attributeValue, Pageable pageable) {
        return listingRepository.findByAttributeValue(attributeName, attributeValue, pageable)
                .map(this::enrichWithSellerInfo);
    }

    /**
     * Get top viewed listings
     * @return List of top viewed listings
     */
    @Cacheable(value = "listings", key = "'topViewed'")
    public List<ListingDTO> getTopViewedListings() {
        return listingRepository.findTop10ByOrderByViewCountDesc().stream()
                .map(this::enrichWithSellerInfo)
                .collect(Collectors.toList());
    }

    /**
     * Enrich a listing with seller information
     * @param listing The listing to enrich
     * @return The enriched listing
     */
    private ListingDTO enrichWithSellerInfo(Listing listing) {
        ListingDTO dto = convertToDto(listing);

        // Fetch seller information
        userRepository.findById(listing.getSellerId()).ifPresent(seller -> {
            dto.setSellerName(seller.getFirstName() + " " + seller.getLastName());
            dto.setSellerRating(seller.getAverageRating());
        });

        return dto;
    }

    /**
     * Convert entity to DTO
     */
    private ListingDTO convertToDto(Listing entity) {
        return ListingDTO.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .currency(entity.getCurrency())
                .sellerId(entity.getSellerId())
                .category(entity.getCategory())
                .condition(entity.getCondition())
                .imageUrls(entity.getImageUrls())
                .attributes(entity.getAttributes())
                .quantity(entity.getQuantity())
                .status(entity.getStatus())
                .viewCount(entity.getViewCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
