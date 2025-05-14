package org.example.marketplace.controller;

import org.example.marketplace.dto.ListingDTO;
import org.example.marketplace.service.command.ListingCommandService;
import org.example.marketplace.service.query.ListingQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
@RequiredArgsConstructor
public class ListingController {

    private final ListingCommandService listingCommandService;
    private final ListingQueryService listingQueryService;

    /**
     * Create a new listing
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListingDTO> createListing(
            @RequestPart("listing") @Valid ListingDTO listingDTO,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        ListingDTO created = listingCommandService.createListing(listingDTO, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get a listing by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ListingDTO> getListingById(@PathVariable String id) {
        // Increment view count when listing is viewed
        listingCommandService.incrementViewCount(id);

        return listingQueryService.getListingById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update a listing
     */
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ListingDTO> updateListing(
            @PathVariable String id,
            @RequestPart("listing") @Valid ListingDTO listingDTO,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages) {

        return listingCommandService.updateListing(id, listingDTO, newImages)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a listing
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteListing(
            @PathVariable String id,
            @RequestParam(value = "permanent", defaultValue = "false") boolean permanent) {

        boolean deleted = listingCommandService.deleteListing(id, permanent);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Mark a listing as sold
     */
    @PatchMapping("/{id}/sold")
    public ResponseEntity<Void> markAsSold(@PathVariable String id) {
        boolean updated = listingCommandService.markAsSold(id);
        return updated ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Get active listings with pagination
     */
    @GetMapping
    public ResponseEntity<Page<ListingDTO>> getActiveListings(Pageable pageable) {
        return ResponseEntity.ok(listingQueryService.getActiveListings(pageable));
    }

    /**
     * Get listings by seller
     */
    @GetMapping("/seller/{sellerId}")
    public ResponseEntity<Page<ListingDTO>> getListingsBySeller(
            @PathVariable String sellerId,
            Pageable pageable) {

        return ResponseEntity.ok(listingQueryService.getListingsBySeller(sellerId, pageable));
    }

    /**
     * Get listings by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<Page<ListingDTO>> getListingsByCategory(
            @PathVariable String category,
            Pageable pageable) {

        return ResponseEntity.ok(listingQueryService.getListingsByCategory(category, pageable));
    }

    /**
     * Search listings by title
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ListingDTO>> searchListings(
            @RequestParam String query,
            Pageable pageable) {

        return ResponseEntity.ok(listingQueryService.searchListingsByTitle(query, pageable));
    }

    /**
     * Search listings by attribute
     */
    @GetMapping("/search/attribute")
    public ResponseEntity<Page<ListingDTO>> searchListingsByAttribute(
            @RequestParam String name,
            @RequestParam String value,
            Pageable pageable) {

        return ResponseEntity.ok(listingQueryService.searchListingsByAttribute(name, value, pageable));
    }

    /**
     * Get top viewed listings
     */
    @GetMapping("/top-viewed")
    public ResponseEntity<List<ListingDTO>> getTopViewedListings() {
        return ResponseEntity.ok(listingQueryService.getTopViewedListings());
    }
}
