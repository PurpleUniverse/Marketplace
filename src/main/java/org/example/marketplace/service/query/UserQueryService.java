package org.example.marketplace.service.query;

import org.example.marketplace.domain.User;
import org.example.marketplace.dto.UserDTO;
import org.example.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserQueryService {

    private final UserRepository userRepository;

    /**
     * Get a user by ID
     * @param id The user ID
     * @return The user if found
     */
    @Cacheable(value = "users", key = "#id")
    public Optional<UserDTO> getUserById(String id) {
        return userRepository.findById(id)
                .map(this::convertToDto);
    }

    /**
     * Get a user by email
     * @param email The user email
     * @return The user if found
     */
    @Cacheable(value = "users", key = "'email:' + #email")
    public Optional<UserDTO> getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(this::convertToDto);
    }

    /**
     * Convert entity to DTO
     */
    private UserDTO convertToDto(User entity) {
        // Convert address if present
        UserDTO.AddressDTO addressDTO = null;
        if (entity.getAddress() != null) {
            addressDTO = UserDTO.AddressDTO.builder()
                    .street(entity.getAddress().getStreet())
                    .city(entity.getAddress().getCity())
                    .state(entity.getAddress().getState())
                    .zipCode(entity.getAddress().getZipCode())
                    .country(entity.getAddress().getCountry())
                    .build();
        }

        return UserDTO.builder()
                .id(entity.getId())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .phoneNumber(entity.getPhoneNumber())
                .address(addressDTO)
                .averageRating(entity.getAverageRating())
                .totalReviews(entity.getTotalReviews())
                .profileImageUrl(entity.getProfileImageUrl())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
