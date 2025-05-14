package org.example.marketplace.service.command;

import org.example.marketplace.domain.User;
import org.example.marketplace.dto.UserDTO;
import org.example.marketplace.repository.UserRepository;
import org.example.marketplace.service.storage.CloudStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepository;
    private final CloudStorageService cloudStorageService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new user
     * @param userDTO The user data
     * @return The created user
     */
    @Transactional
    public UserDTO createUser(UserDTO userDTO) {
        log.info("Creating user with email: {}", userDTO.getEmail());

        // Check if email already exists
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists: " + userDTO.getEmail());
        }

        // Prepare user entity with encoded password
        User user = convertToEntity(userDTO);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setAverageRating(0.0);
        user.setTotalReviews(0);

        User savedUser = userRepository.save(user);
        return convertToDto(savedUser);
    }

    /**
     * Update a user
     * @param id The user ID
     * @param userDTO The updated user data
     * @return The updated user
     */
    @Transactional
    public Optional<UserDTO> updateUser(String id, UserDTO userDTO) {
        log.info("Updating user: {}", id);

        return userRepository.findById(id)
                .map(existingUser -> {
                    // Update basic fields
                    existingUser.setFirstName(userDTO.getFirstName());
                    existingUser.setLastName(userDTO.getLastName());
                    existingUser.setPhoneNumber(userDTO.getPhoneNumber());

                    // Update address if provided
                    if (userDTO.getAddress() != null) {
                        User.Address address = User.Address.builder()
                                .street(userDTO.getAddress().getStreet())
                                .city(userDTO.getAddress().getCity())
                                .state(userDTO.getAddress().getState())
                                .zipCode(userDTO.getAddress().getZipCode())
                                .country(userDTO.getAddress().getCountry())
                                .build();
                        existingUser.setAddress(address);
                    }

                    User updatedUser = userRepository.save(existingUser);
                    return convertToDto(updatedUser);
                });
    }

    /**
     * Upload a profile image for a user
     * @param id The user ID
     * @param image The profile image file
     * @return The updated user
     */
    @Transactional
    public Optional<UserDTO> uploadProfileImage(String id, MultipartFile image) {
        log.info("Uploading profile image for user: {}", id);

        return userRepository.findById(id)
                .map(user -> {
                    try {
                        // Delete old image if exists
                        if (user.getProfileImageUrl() != null) {
                            cloudStorageService.deleteFile(user.getProfileImageUrl());
                        }

                        // Upload new image
                        String imageUrl = cloudStorageService.uploadFile(image, "users");
                        user.setProfileImageUrl(imageUrl);

                        User updatedUser = userRepository.save(user);
                        return convertToDto(updatedUser);
                    } catch (IOException e) {
                        log.error("Error uploading profile image", e);
                        throw new RuntimeException("Error uploading profile image", e);
                    }
                });
    }

    /**
     * Delete a user
     * @param id The user ID
     * @return true if successful, false if not found
     */
    @Transactional
    public boolean deleteUser(String id) {
        log.info("Deleting user: {}", id);

        return userRepository.findById(id)
                .map(user -> {
                    // Delete profile image if exists
                    if (user.getProfileImageUrl() != null) {
                        try {
                            cloudStorageService.deleteFile(user.getProfileImageUrl());
                        } catch (Exception e) {
                            log.error("Error deleting profile image", e);
                        }
                    }

                    userRepository.deleteById(id);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Convert DTO to entity
     */
    private User convertToEntity(UserDTO dto) {
        // Convert address if present
        User.Address address = null;
        if (dto.getAddress() != null) {
            address = User.Address.builder()
                    .street(dto.getAddress().getStreet())
                    .city(dto.getAddress().getCity())
                    .state(dto.getAddress().getState())
                    .zipCode(dto.getAddress().getZipCode())
                    .country(dto.getAddress().getCountry())
                    .build();
        }

        return User.builder()
                .id(dto.getId())
                .email(dto.getEmail())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .phoneNumber(dto.getPhoneNumber())
                .address(address)
                .profileImageUrl(dto.getProfileImageUrl())
                .build();
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
