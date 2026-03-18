package com.tuttime.user.service.impl;

import com.tuttime.common.exception.BusinessException;
import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.user.dto.*;
import com.tuttime.user.entity.User;
import com.tuttime.user.entity.UserDevice;
import com.tuttime.user.entity.UserBlock;
import com.tuttime.user.mapper.UserMapper;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.repository.UserDeviceRepository;
import com.tuttime.user.repository.UserBlockRepository;
import com.tuttime.user.service.AdminUserService;
import com.tuttime.user.service.ImageUrlCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin User Service Implementation
 * 
 * <p>Provides admin-only operations for user management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserBlockRepository userBlockRepository;
    private final UserMapper userMapper;
    private final ImageUrlCacheService imageUrlCacheService;

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> listUsers(String search, String status, String kycStatus, boolean includeDeleted, Pageable pageable) {
        log.debug("Listing users: search={}, status={}, kycStatus={}, includeDeleted={}", search, status, kycStatus, includeDeleted);
        
        Page<User> users = userRepository.findUsersWithFilters(search, status, kycStatus, includeDeleted, pageable);
        return users.map(user -> {
            UserResponse response = userMapper.toResponse(user);
            // Enrich with image URL (for backward compatibility with legacy keys in database)
            userMapper.enrichImageUrl(response, imageUrlCacheService);
            return response;
        });
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long userId, boolean includeDeleted) {
        log.debug("Getting user by ID: {}, includeDeleted={}", userId, includeDeleted);
        
        User user;
        if (includeDeleted) {
            user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        } else {
            user = userRepository.findByIdAndNotDeleted(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        }
        
        UserResponse response = userMapper.toResponse(user);
        // Enrich with image URL (for backward compatibility with legacy keys in database)
        userMapper.enrichImageUrl(response, imageUrlCacheService);
        return response;
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long userId, AdminUpdateUserRequest request) {
        log.debug("Updating user: userId={}, request={}", userId, request);
        
        try {
            // Allow updating soft-deleted users (for admin to restore them)
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
            
            // Update fields if provided (null = not provided/skip, empty string = clear the field)
            if (request.getFirstName() != null) {
                user.setFirstName(request.getFirstName().trim().isEmpty() ? null : request.getFirstName().trim());
            }
            if (request.getLastName() != null) {
                user.setLastName(request.getLastName().trim().isEmpty() ? null : request.getLastName().trim());
            }
            // Username is optional; only update if provided and non-blank
            if (request.getUsername() != null && !request.getUsername().trim().isEmpty()) {
                String username = request.getUsername().trim();
                if (!username.equalsIgnoreCase(user.getUsername())) {
                    // Check if username is already taken by another user (case-insensitive comparison)
                    userRepository.findByUsername(username)
                            .ifPresent(existingUser -> {
                                if (!existingUser.getUserId().equals(userId)) {
                                    throw new BusinessException("Username already taken: " + username);
                                }
                            });
                }
                user.setUsername(username);
            }
            if (request.getEmail() != null) {
                if (request.getEmail().trim().isEmpty()) {
                    user.setEmail(null);
                } else {
                    String newEmail = request.getEmail().trim();
                    if (!newEmail.equals(user.getEmail())) {
                        // Check if email is already taken by another user
                        userRepository.findByEmail(newEmail)
                                .ifPresent(existingUser -> {
                                    if (!existingUser.getUserId().equals(userId)) {
                                        throw new BusinessException("Email already registered: " + newEmail);
                                    }
                                });
                        user.setEmail(newEmail);
                    }
                }
            }
            if (request.getPhone() != null) {
                user.setPhone(request.getPhone().trim().isEmpty() ? null : request.getPhone().trim());
            }
            if (request.getCity() != null) {
                user.setCity(request.getCity().trim().isEmpty() ? null : request.getCity().trim());
            }
            // Note: User entity doesn't have state field
            if (request.getCountry() != null) {
                user.setCountry(request.getCountry().trim().isEmpty() ? null : request.getCountry().trim());
            }
            if (request.getDateOfBirth() != null) {
                if (request.getDateOfBirth().trim().isEmpty()) {
                    user.setDateOfBirth(null);
                } else {
                    try {
                        // Parse date string (YYYY-MM-DD) and convert to OffsetDateTime at start of day UTC
                        String dateStr = request.getDateOfBirth().trim();
                        user.setDateOfBirth(OffsetDateTime.parse(dateStr + "T00:00:00Z"));
                    } catch (Exception e) {
                        log.warn("Invalid dateOfBirth format: {}, ignoring", request.getDateOfBirth());
                    }
                }
            }
            if (request.getTelegramId() != null) {
                // User entity has telegram field, not telegramId
                // Empty string = clear telegram info
                user.setTelegram(request.getTelegramId().trim().isEmpty() ? null : request.getTelegramId().trim());
            }
            if (request.getInstagram() != null) {
                user.setInstagram(request.getInstagram().trim().isEmpty() ? null : request.getInstagram().trim());
            }
            if (request.getGender() != null) {
                user.setGender(request.getGender());
            }
            
            User saved = userRepository.save(user);
            log.info("User updated successfully: userId={}", userId);
            
            UserResponse response = userMapper.toResponse(saved);
            // Enrich with image URL (for backward compatibility with legacy keys in database)
            userMapper.enrichImageUrl(response, imageUrlCacheService);
            return response;
        } catch (BusinessException | ResourceNotFoundException e) {
            log.error("Business error updating user: userId={}, error={}", userId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error updating user: userId={}", userId, e);
            throw new BusinessException("Failed to update user: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public UserResponse blockUser(Long userId, String reason) {
        log.debug("Blocking user: userId={}, reason={}", userId, reason);
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("Block reason is required");
        }
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (user.getIsBlocked()) {
            log.warn("User already blocked: userId={}", userId);
            UserResponse response = userMapper.toResponse(user);
            // Enrich with image URL (for backward compatibility with legacy keys in database)
            userMapper.enrichImageUrl(response, imageUrlCacheService);
            return response;
        }
        
        user.setIsBlocked(true);
        user.setBlockedReason(reason);
        user.setBlockedAt(OffsetDateTime.now(ZoneOffset.UTC));
        user.setIsActive(false); // Also deactivate when blocking
        
        User saved = userRepository.save(user);
        log.info("User blocked: userId={}, reason={}", userId, reason);
        
        UserResponse response = userMapper.toResponse(saved);
        // Enrich with image URL (for backward compatibility with legacy keys in database)
        userMapper.enrichImageUrl(response, imageUrlCacheService);
        return response;
    }

    @Override
    @Transactional
    public UserResponse unblockUser(Long userId) {
        log.debug("Unblocking user: userId={}", userId);
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (!user.getIsBlocked()) {
            log.warn("User not blocked: userId={}", userId);
            UserResponse response = userMapper.toResponse(user);
            // Enrich with image URL (for backward compatibility with legacy keys in database)
            userMapper.enrichImageUrl(response, imageUrlCacheService);
            return response;
        }
        
        user.setIsBlocked(false);
        user.setBlockedReason(null);
        user.setBlockedAt(null);
        user.setIsActive(true); // Reactivate when unblocking
        
        User saved = userRepository.save(user);
        log.info("User unblocked: userId={}", userId);
        
        UserResponse response = userMapper.toResponse(saved);
        // Enrich with image URL (for backward compatibility with legacy keys in database)
        userMapper.enrichImageUrl(response, imageUrlCacheService);
        return response;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "userDetails"}, key = "#userId")
    public void deleteUser(Long userId) {
        log.debug("Deleting user (soft delete): userId={}", userId);
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (user.getDeletedAt() != null) {
            log.warn("User already deleted: userId={}", userId);
            return;
        }
        
        user.setDeletedAt(OffsetDateTime.now(ZoneOffset.UTC));
        user.setIsActive(false);
        userRepository.save(user);
        
        log.info("User deleted (soft delete): userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public UserReferralsResponse getUserReferrals(Long userId) {
        log.debug("Getting user referrals: userId={}", userId);
        
        User user = userRepository.findByIdAndNotDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        // TODO: Integrate with rewards-service to get actual referral data
        // For now, return placeholder data
        return UserReferralsResponse.builder()
                .userId(userId)
                .username(user.getUsername())
                .referralCode("") // TODO: Get from rewards-service
                .totalReferrals(0) // TODO: Get from rewards-service
                .activeReferrals(0) // TODO: Get from rewards-service
                .referrals(List.of()) // TODO: Get from rewards-service
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BlockedUserResponse> getBlockedUsers(Pageable pageable) {
        log.debug("Getting blocked users");
        
        Page<User> blockedUsers = userRepository.findBlockedUsers(pageable);
        
        return blockedUsers.map(user -> BlockedUserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .blockedAt(user.getBlockedAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByPhoneNumber(String phoneNumber) {
        log.debug("Getting user by phone number: {}", phoneNumber);
        
        // Use findByPhoneIncludingDeleted for admin operations to allow finding soft-deleted users
        // This is needed for beta tester deletion which should work even if user is soft-deleted
        User user = userRepository.findByPhoneIncludingDeleted(phoneNumber)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with phone number: " + phoneNumber));
        
        UserResponse response = userMapper.toResponse(user);
        // Enrich with image URL (for backward compatibility with legacy keys in database)
        userMapper.enrichImageUrl(response, imageUrlCacheService);
        return response;
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "userDetails"}, key = "#userId")
    public void hardDeleteUser(Long userId) {
        log.info("Hard deleting user: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (user.getDeletedAt() != null) {
            log.info("Hard deleting soft-deleted user: userId={}, deletedAt={}", userId, user.getDeletedAt());
        }
        
        // Delete all user devices
        List<UserDevice> devices = userDeviceRepository.findByUserId(userId);
        if (!devices.isEmpty()) {
            log.info("Deleting {} devices for user: userId={}", devices.size(), userId);
            userDeviceRepository.deleteAll(devices);
        }
        
        // Delete all user blocks (where user is blocker or blocked)
        List<UserBlock> blocksAsBlocker = userBlockRepository.findByBlockerUserIdAndDeletedAtIsNull(userId);
        List<UserBlock> blocksAsBlocked = userBlockRepository.findByBlockedUserIdAndDeletedAtIsNull(userId);
        if (!blocksAsBlocker.isEmpty() || !blocksAsBlocked.isEmpty()) {
            log.info("Deleting {} blocks for user: userId={}", blocksAsBlocker.size() + blocksAsBlocked.size(), userId);
            userBlockRepository.deleteAll(blocksAsBlocker);
            userBlockRepository.deleteAll(blocksAsBlocked);
        }
        
        // Finally, delete the user itself
        userRepository.delete(user);
        
        log.info("User hard deleted successfully: userId={}", userId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"users", "userDetails"}, key = "#userId")
    public UserResponse restoreUser(Long userId) {
        log.debug("Restoring soft-deleted user: userId={}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        
        if (user.getDeletedAt() == null) {
            log.warn("User is not soft-deleted: userId={}", userId);
            UserResponse response = userMapper.toResponse(user);
            userMapper.enrichImageUrl(response, imageUrlCacheService);
            return response;
        }
        
        // Clear deletedAt and reactivate
        user.setDeletedAt(null);
        user.setIsActive(true);
        
        User saved = userRepository.save(user);
        log.info("User restored successfully: userId={}", userId);
        
        UserResponse response = userMapper.toResponse(saved);
        userMapper.enrichImageUrl(response, imageUrlCacheService);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> getUsersPreferredLanguages(List<Long> userIds) {
        log.debug("Getting preferred languages for {} users", userIds.size());
        
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        
        // Fetch users in batch
        List<User> users = userRepository.findAllById(userIds);
        
        // Build map of userId -> preferred language (default to "en" if not set)
        Map<Long, String> languageMap = users.stream()
                .collect(Collectors.toMap(
                        User::getUserId,
                        user -> user.getPreferredLanguage() != null && !user.getPreferredLanguage().isEmpty() 
                                ? user.getPreferredLanguage() 
                                : "en"
                ));
        
        // Add default "en" for any user IDs not found
        for (Long userId : userIds) {
            languageMap.putIfAbsent(userId, "en");
        }
        
        log.debug("Retrieved preferred languages for {}/{} users", languageMap.size(), userIds.size());
        return languageMap;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getAllActiveUserIds(String status, boolean includeDeleted) {
        log.debug("Getting all active user IDs: status={}, includeDeleted={}", status, includeDeleted);
        
        // Use a projection query to fetch only user IDs (much more efficient)
        List<Long> userIds;
        
        if (includeDeleted) {
            if ("active".equals(status)) {
                userIds = userRepository.findUserIdsByIsActiveTrue();
            } else {
                userIds = userRepository.findAllUserIds();
            }
        } else {
            if ("active".equals(status)) {
                userIds = userRepository.findActiveUserIdsNotDeleted();
            } else {
                userIds = userRepository.findUserIdsNotDeleted();
            }
        }
        
        log.info("Retrieved {} user IDs", userIds.size());
        return userIds;
    }
}

