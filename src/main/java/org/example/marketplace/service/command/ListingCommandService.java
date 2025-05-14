package org.example.marketplace.service.command;

import org.example.marketplace.domain.Listing;
import org.example.marketplace.dto.ListingDTO;
import org.example.marketplace.repository.ListingRepository;
import org.example.marketplace.service.storage.CloudStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListingCommandService {

    private final ListingRepository listingRepository;
    private final CloudStorageService cloudStorageService;

    /**
     * Create a new listing
     * @param listingDTO The listing data
     * @param images Optional images to upload
     * @return The created listing
     */
    @Transactional
    public ListingDTO createListing(ListingDTO listingDTO, List<MultipartFile> images) {
        log.info("Creating listing: {}", listingDTO.getTitle());

        // Set initial values
        listingDTO.setStatus(Listing.ListingStatus.ACTIVE);
        listingDTO.setViewCount(0);

        // Handle image uploads if provided
        List<String> imageUrls = new ArrayList<>();
        if (images != null && !images.isEmpty()) {
            imageUrls = uploadListingImages(images);
        }
        listingDTO.setImageUrls(imageUrls);

        // Convert DTO to entity and save
        Listing listing = convertToEntity(listingDTO);
        Listing savedListing = listingRepository.save(listing);

        return convertToDto(savedListing);
    }

    /**
     * Update an existing listing
     * @param id The listing ID
     * @param listingDTO The updated listing data
     * @param newImages Optional new images to upload
     * @return The updated listing
     */
    @Transactional
    public Optional<ListingDTO> updateListing(String id, ListingDTO listingDTO, List<MultipartFile> newImages) {
        log.info("Updating listing: {}", id);

        return listingRepository.findById(id)
                .map(existingListing -> {
                    // Update basic fields
                    existingListing.setTitle(listingDTO.getTitle());
                    existingListing.setDescription(listingDTO.getDescription());
                    existingListing.setPrice(listingDTO.getPrice());
                    existingListing.setCurrency(listingDTO.getCurrency());
                    existingListing.setCategory(listingDTO.getCategory());
                    existingListing.setCondition(listingDTO.getCondition());
                    existingListing.setAttributes(listingDTO.getAttributes());
                    existingListing.setQuantity(listingDTO.getQuantity());

                    // Only update status if provided
                    if (listingDTO.getStatus() != null) {
                        existingListing.setStatus(listingDTO.getStatus());
                    }

                    // Handle new images if provided
                    if (newImages != null && !newImages.isEmpty()) {
                        List<String> newImageUrls = uploadListingImages(newImages);
                        // Add new images to existing ones
                        List<String> allImageUrls = new ArrayList<>(existingListing.getImageUrls());
                        allImageUrls.addAll(newImageUrls);
                        existingListing.setImageUrls(allImageUrls);
                    }

                    Listing updatedListing = listingRepository.save(existingListing);
                    return convertToDto(updatedListing);
                });
    }

    /**
     * Delete a listing by ID
     * @param id The listing ID
     * @param permanent Whether to permanently delete or just mark as deleted
     * @return true if successful, false if not found
     */
    @Transactional
    public boolean deleteListing(String id, boolean permanent) {
        log.info("Deleting listing: {}, permanent: {}", id, permanent);

        if (permanent) {
            // Find the listing to get image URLs before deleting
            return listingRepository.findById(id)
                    .map(listing -> {
                        // Delete images from cloud storage
                        listing.getImageUrls().forEach(url -> {
                            try {
                                cloudStorageService.deleteFile(url);
                            } catch (Exception e) {
                                log.error("Error deleting image: {}", url, e);
                            }
                        });
                        // Delete the listing
                        listingRepository.deleteById(id);
                        return true;
                    })
                    .orElse(false);
        } else {
            // Soft delete by updating status
            return listingRepository.findById(id)
                    .map(listing -> {
                        listing.setStatus(Listing.ListingStatus.DELETED);
                        listingRepository.save(listing);
                        return true;
                    })
                    .orElse(false);
        }
    }

    /**
     * Mark a listing as sold
     * @param id The listing ID
     * @return true if successful, false if not found
     */
    @Transactional
    public boolean markAsSold(String id) {
        log.info("Marking listing as sold: {}", id);

        return listingRepository.findById(id)
                .map(listing -> {
                    listing.setStatus(Listing.ListingStatus.SOLD);
                    listingRepository.save(listing);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Increment the view count for a listing
     * @param id The listing ID
     */
    @Transactional
    public void incrementViewCount(String id) {
        listingRepository.findById(id).ifPresent(listing -> {
            listing.setViewCount(listing.getViewCount() + 1);
            listingRepository.save(listing);
        });
    }

    /**
     * Upload images for a listing
     * @param images The images to upload
     * @return List of image URLs
     */
    private List<String> uploadListingImages(List<MultipartFile> images) {
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile image : images) {
            try {
                String imageUrl = cloudStorageService.uploadFile(image, "listings");
                imageUrls.add(imageUrl);
            } catch (IOException e) {
                log.error("Error uploading image", e);
            }
        }
        return imageUrls;
    }

    /**
     * Convert DTO to entity
     */
    private Listing convertToEntity(ListingDTO dto) {
        return Listing.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .description(dto.getDescription())
                .price(dto.getPrice())
                .currency(dto.getCurrency())
                .sellerId(dto.getSellerId())
                .category(dto.getCategory())
                .condition(dto.getCondition())
                .imageUrls(dto.getImageUrls())
                .attributes(dto.getAttributes())
                .quantity(dto.getQuantity())
                .status(dto.getStatus())
                .viewCount(dto.getViewCount())
                .build();
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
