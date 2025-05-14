package org.example.marketplace.repository;

import org.example.marketplace.domain.Listing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ListingRepository extends MongoRepository<Listing, String> {

    Page<Listing> findByStatus(Listing.ListingStatus status, Pageable pageable);

    Page<Listing> findBySellerId(String sellerId, Pageable pageable);

    Page<Listing> findBySellerIdAndStatus(String sellerId, Listing.ListingStatus status, Pageable pageable);

    Page<Listing> findByCategory(String category, Pageable pageable);

    @Query("{'title': {$regex: ?0, $options: 'i'}}")
    Page<Listing> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    @Query("{'attributes.?0': ?1}")
    Page<Listing> findByAttributeValue(String attributeName, Object attributeValue, Pageable pageable);

    List<Listing> findTop10ByOrderByViewCountDesc();
}
