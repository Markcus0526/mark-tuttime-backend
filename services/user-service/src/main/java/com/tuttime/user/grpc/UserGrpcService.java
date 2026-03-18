package com.tuttime.user.grpc;

import com.tuttime.common.exception.ResourceNotFoundException;
import com.tuttime.common.proto.*;
import com.tuttime.user.entity.User;
import com.tuttime.user.grpc.client.MiningServiceGrpcClient;
import com.tuttime.user.grpc.client.StakingServiceGrpcClient;
import com.tuttime.user.grpc.client.TokenServiceGrpcClient;
import com.tuttime.user.repository.UserRepository;
import com.tuttime.user.repository.UserDeviceRepository;
import com.tuttime.user.repository.readmodel.UserReadModelRepository;
import com.tuttime.user.service.ImageUrlCacheService;
import com.tuttime.user.service.OnlineStatusService;
import com.tuttime.user.service.UserService;
import com.tuttime.user.service.UserBlockService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;
    private final UserReadModelRepository userReadModelRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final UserService userService;
    private final MiningServiceGrpcClient miningServiceGrpcClient;
    private final TokenServiceGrpcClient tokenServiceGrpcClient;
    private final StakingServiceGrpcClient stakingServiceGrpcClient;
    private final OnlineStatusService onlineStatusService;
    private final com.tuttime.user.service.ApartSharingStatusService apartSharingStatusService;
    private final ImageUrlCacheService imageUrlCacheService;
    private final UserBlockService userBlockService;

    @PostConstruct
    public void init() {
        log.info("gRPC: UserGrpcService initialized - SearchUsers method available");
    }

    @Override
    @Transactional(readOnly = true)
    public void getUserById(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        log.debug("gRPC: GetUserById request for userId: {}", request.getUserId());

        try {
            User user = userRepository.findByIdAndNotDeleted(request.getUserId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found: " + request.getUserId()));

            UserResponse.Builder responseBuilder = UserResponse.newBuilder()
                    .setUserId(user.getUserId())
                    .setEmail(user.getEmail())
                    .setUsername(user.getUsername())
                    .setIsVerified(user.getIsVerified())
                    .setKycCompleted(user.getKycCompleted())
                    .setIsActive(user.getIsActive())
                    .setIsBlocked(user.getIsBlocked());
            
            if (user.getFirstName() != null) {
                responseBuilder.setFirstName(user.getFirstName());
            }
            if (user.getLastName() != null) {
                responseBuilder.setLastName(user.getLastName());
            }
            if (user.getProfileImageUrl() != null) {
                // Convert key to URL if needed (for backward compatibility with keys in database)
                String imageUrl = imageUrlCacheService.getCachedImageUrl(user.getProfileImageUrl());
                responseBuilder.setProfileImageUrl(imageUrl);
            }
            if (user.getAbout() != null) {
                responseBuilder.setAbout(user.getAbout());
            }
            if (user.getWebsites() != null) {
                responseBuilder.setWebsites(user.getWebsites());
            }
            if (user.getRegistrationDate() != null) {
                long registrationTimestamp = user.getRegistrationDate().toInstant().toEpochMilli();
                responseBuilder.setRegistrationDate(registrationTimestamp);
            }
            
            // Add contact information
            if (user.getPhone() != null) {
                responseBuilder.setPhone(user.getPhone());
            }
            if (user.getTelegram() != null) {
                responseBuilder.setTelegram(user.getTelegram());
            }
            if (user.getInstagram() != null) {
                responseBuilder.setInstagram(user.getInstagram());
            }
            
            // Add location information
            if (user.getCountry() != null) {
                responseBuilder.setCountry(user.getCountry());
            }
            if (user.getCity() != null) {
                responseBuilder.setCity(user.getCity());
            }
            if (user.getLatitude() != null) {
                responseBuilder.setLatitude(user.getLatitude().doubleValue());
            }
            if (user.getLongitude() != null) {
                responseBuilder.setLongitude(user.getLongitude().doubleValue());
            }
            
            // Add personal information
            if (user.getDateOfBirth() != null) {
                responseBuilder.setDateOfBirth(user.getDateOfBirth().toLocalDate().toString());
            }
            if (user.getGender() != null) {
                responseBuilder.setGender(user.getGender().name());
            }
            
            // Add language preference
            if (user.getPreferredLanguage() != null) {
                responseBuilder.setPreferredLanguage(user.getPreferredLanguage());
            } else {
                responseBuilder.setPreferredLanguage("en"); // Default
            }
            
            UserResponse response = responseBuilder.build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ResourceNotFoundException e) {
            log.warn("gRPC: User not found: userId={}", request.getUserId());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("User not found: " + request.getUserId())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC: Error getting user by ID: userId={}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get user: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getUserByEmail(GetUserByEmailRequest request, StreamObserver<UserResponse> responseObserver) {
        log.debug("gRPC: GetUserByEmail request for email: {}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getEmail()));

        UserResponse.Builder responseBuilder = UserResponse.newBuilder()
                .setUserId(user.getUserId())
                .setEmail(user.getEmail())
                .setUsername(user.getUsername())
                .setIsVerified(user.getIsVerified())
                .setKycCompleted(user.getKycCompleted())
                .setIsActive(user.getIsActive())
                .setIsBlocked(user.getIsBlocked());
        
        if (user.getFirstName() != null) {
            responseBuilder.setFirstName(user.getFirstName());
        }
        if (user.getLastName() != null) {
            responseBuilder.setLastName(user.getLastName());
        }
        if (user.getProfileImageUrl() != null) {
            // Convert key to URL if needed (for backward compatibility with keys in database)
            String imageUrl = imageUrlCacheService.getCachedImageUrl(user.getProfileImageUrl());
            if (imageUrl != null) {
                responseBuilder.setProfileImageUrl(imageUrl);
            }
        }
        if (user.getAbout() != null) {
            responseBuilder.setAbout(user.getAbout());
        }
        if (user.getWebsites() != null) {
            responseBuilder.setWebsites(user.getWebsites());
        }
        if (user.getRegistrationDate() != null) {
            long registrationTimestamp = user.getRegistrationDate().toInstant().toEpochMilli();
            responseBuilder.setRegistrationDate(registrationTimestamp);
        }
        
        // Add contact information
        if (user.getPhone() != null) {
            responseBuilder.setPhone(user.getPhone());
        }
        if (user.getTelegram() != null) {
            responseBuilder.setTelegram(user.getTelegram());
        }
        if (user.getInstagram() != null) {
            responseBuilder.setInstagram(user.getInstagram());
        }
        
        // Add location information
        if (user.getCountry() != null) {
            responseBuilder.setCountry(user.getCountry());
        }
        if (user.getCity() != null) {
            responseBuilder.setCity(user.getCity());
        }
        if (user.getLatitude() != null) {
            responseBuilder.setLatitude(user.getLatitude().doubleValue());
        }
        if (user.getLongitude() != null) {
            responseBuilder.setLongitude(user.getLongitude().doubleValue());
        }
        
        // Add personal information
        if (user.getDateOfBirth() != null) {
            responseBuilder.setDateOfBirth(user.getDateOfBirth().toLocalDate().toString());
        }
        if (user.getGender() != null) {
            responseBuilder.setGender(user.getGender().name());
        }
        
        // Add language preference
        if (user.getPreferredLanguage() != null) {
            responseBuilder.setPreferredLanguage(user.getPreferredLanguage());
        } else {
            responseBuilder.setPreferredLanguage("en"); // Default
        }
        
        UserResponse response = responseBuilder.build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void getUserByPhone(GetUserByPhoneRequest request, StreamObserver<UserResponse> responseObserver) {
        log.debug("gRPC: GetUserByPhone request for phone: {}", request.getPhone());

        try {
            // Normalize phone number before lookup for consistent comparison
            String normalizedPhone = com.tuttime.user.util.PhoneNumberUtil.normalizePhoneNumber(request.getPhone());
            User user = userRepository.findByPhone(normalizedPhone)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found with phone: " + request.getPhone()));

            UserResponse.Builder responseBuilder = UserResponse.newBuilder()
                    .setUserId(user.getUserId())
                    .setEmail(user.getEmail())
                    .setUsername(user.getUsername())
                    .setIsVerified(user.getIsVerified())
                    .setKycCompleted(user.getKycCompleted())
                    .setIsActive(user.getIsActive())
                    .setIsBlocked(user.getIsBlocked());
            
            if (user.getFirstName() != null) {
                responseBuilder.setFirstName(user.getFirstName());
            }
            if (user.getLastName() != null) {
                responseBuilder.setLastName(user.getLastName());
            }
            if (user.getProfileImageUrl() != null) {
                // Convert key to URL if needed (for backward compatibility with keys in database)
                String imageUrl = imageUrlCacheService.getCachedImageUrl(user.getProfileImageUrl());
                responseBuilder.setProfileImageUrl(imageUrl);
            }
            if (user.getAbout() != null) {
                responseBuilder.setAbout(user.getAbout());
            }
            if (user.getWebsites() != null) {
                responseBuilder.setWebsites(user.getWebsites());
            }
            if (user.getRegistrationDate() != null) {
                long registrationTimestamp = user.getRegistrationDate().toInstant().toEpochMilli();
                responseBuilder.setRegistrationDate(registrationTimestamp);
            }
            
            // Add personal information
            if (user.getDateOfBirth() != null) {
                responseBuilder.setDateOfBirth(user.getDateOfBirth().toLocalDate().toString());
            }
            if (user.getGender() != null) {
                responseBuilder.setGender(user.getGender().name());
            }
            
            // Add language preference
            if (user.getPreferredLanguage() != null) {
                responseBuilder.setPreferredLanguage(user.getPreferredLanguage());
            } else {
                responseBuilder.setPreferredLanguage("en"); // Default
            }
            
            UserResponse response = responseBuilder.build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (ResourceNotFoundException e) {
            log.warn("gRPC: User not found with phone: {}", request.getPhone());
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription("User not found with phone: " + request.getPhone())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("gRPC: Error getting user by phone: phone={}", request.getPhone(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get user: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void searchUsers(SearchUsersRequest request, StreamObserver<SearchUsersResponse> responseObserver) {
        log.debug("gRPC: SearchUsers request for query: '{}', page: {}, size: {}", 
                request.getQuery(), request.getPage(), request.getSize());

        try {
            String query = request.getQuery() != null ? request.getQuery().trim() : "";
            if (query.isEmpty()) {
                SearchUsersResponse emptyResponse = SearchUsersResponse.newBuilder()
                        .setTotalPages(0)
                        .setTotalElements(0)
                        .setCurrentPage(0)
                        .setPageSize(request.getSize() > 0 ? request.getSize() : 20)
                        .build();
                responseObserver.onNext(emptyResponse);
                responseObserver.onCompleted();
                return;
            }

            int page = request.getPage() >= 0 ? request.getPage() : 0;
            int size = request.getSize() > 0 ? request.getSize() : 20;
            
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            org.springframework.data.domain.Page<User> usersPage = userRepository.searchUsers(query, pageable);
            
            log.debug("gRPC: SearchUsers found {} users for query: '{}'", usersPage.getTotalElements(), query);
            
            SearchUsersResponse.Builder responseBuilder = SearchUsersResponse.newBuilder()
                    .setTotalPages(usersPage.getTotalPages())
                    .setTotalElements(usersPage.getTotalElements())
                    .setCurrentPage(usersPage.getNumber())
                    .setPageSize(usersPage.getSize());
            
            for (User user : usersPage.getContent()) {
                UserResponse.Builder userResponseBuilder = UserResponse.newBuilder()
                        .setUserId(user.getUserId())
                        .setEmail(user.getEmail())
                        .setUsername(user.getUsername())
                        .setIsVerified(user.getIsVerified())
                        .setKycCompleted(user.getKycCompleted())
                        .setIsActive(user.getIsActive())
                        .setIsBlocked(user.getIsBlocked());
                
                if (user.getFirstName() != null) {
                    userResponseBuilder.setFirstName(user.getFirstName());
                }
                if (user.getLastName() != null) {
                    userResponseBuilder.setLastName(user.getLastName());
                }
                if (user.getProfileImageUrl() != null) {
                    // Convert key to URL if needed (for backward compatibility with keys in database)
                    String imageUrl = imageUrlCacheService.getCachedImageUrl(user.getProfileImageUrl());
                    userResponseBuilder.setProfileImageUrl(imageUrl);
                }
                if (user.getAbout() != null) {
                    userResponseBuilder.setAbout(user.getAbout());
                }
                if (user.getWebsites() != null) {
                    userResponseBuilder.setWebsites(user.getWebsites());
                }
                if (user.getRegistrationDate() != null) {
                    long registrationTimestamp = user.getRegistrationDate().toInstant().toEpochMilli();
                    userResponseBuilder.setRegistrationDate(registrationTimestamp);
                }
                
                // Add contact information
                if (user.getPhone() != null) {
                    userResponseBuilder.setPhone(user.getPhone());
                }
                if (user.getTelegram() != null) {
                    userResponseBuilder.setTelegram(user.getTelegram());
                }
                if (user.getInstagram() != null) {
                    userResponseBuilder.setInstagram(user.getInstagram());
                }
                
                // Add location information
                if (user.getCountry() != null) {
                    userResponseBuilder.setCountry(user.getCountry());
                }
                if (user.getCity() != null) {
                    userResponseBuilder.setCity(user.getCity());
                }
                if (user.getLatitude() != null) {
                    userResponseBuilder.setLatitude(user.getLatitude().doubleValue());
                }
                if (user.getLongitude() != null) {
                    userResponseBuilder.setLongitude(user.getLongitude().doubleValue());
                }
                
                // Add personal information
                if (user.getDateOfBirth() != null) {
                    userResponseBuilder.setDateOfBirth(user.getDateOfBirth().toLocalDate().toString());
                }
                if (user.getGender() != null) {
                    userResponseBuilder.setGender(user.getGender().name());
                }
                
                // Add language preference
                if (user.getPreferredLanguage() != null) {
                    userResponseBuilder.setPreferredLanguage(user.getPreferredLanguage());
                } else {
                    userResponseBuilder.setPreferredLanguage("en"); // Default
                }
                
                responseBuilder.addUsers(userResponseBuilder.build());
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Error searching users: query={}", request.getQuery(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to search users: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validateUser(ValidateUserRequest request, StreamObserver<ValidateUserResponse> responseObserver) {
        log.debug("gRPC: ValidateUser request for userId: {}", request.getUserId());

        User user = userRepository.findByIdAndNotDeleted(request.getUserId())
                .orElse(null);

        ValidateUserResponse response = ValidateUserResponse.newBuilder()
                .setValid(user != null)
                .setActive(user != null && user.getIsActive())
                .setBlocked(user != null && user.getIsBlocked())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void getUserStatus(GetUserRequest request, StreamObserver<UserStatusResponse> responseObserver) {
        log.debug("gRPC: GetUserStatus request for userId: {}", request.getUserId());

        User user = userRepository.findByIdAndNotDeleted(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));

        // Get status level from token-service (source of truth)
        String statusLevel = "BRONZE"; // Default fallback
        try {
            GetUserStatusResponse statusResponse = tokenServiceGrpcClient.getUserStatus(user.getUserId());
            statusLevel = statusResponse.getStatusLevel();
        } catch (Exception e) {
            log.warn("Failed to get user status from token-service for user {}: {}, using default BRONZE", 
                    user.getUserId(), e.getMessage());
        }

        UserStatusResponse response = UserStatusResponse.newBuilder()
                .setUserId(user.getUserId())
                .setIsActive(user.getIsActive())
                .setIsVerified(user.getIsVerified())
                .setKycCompleted(user.getKycCompleted())
                .setIsBlocked(user.getIsBlocked())
                .setStatusLevel(statusLevel)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void isKycVerified(GetUserRequest request, StreamObserver<KycStatusResponse> responseObserver) {
        log.debug("gRPC: IsKycVerified request for userId: {}", request.getUserId());

        try {
            User user = userRepository.findByIdAndNotDeleted(request.getUserId())
                    .orElse(null);

            // If user not found, return unverified status
            if (user == null) {
                log.warn("User not found for KYC check: {}", request.getUserId());
                KycStatusResponse response = KycStatusResponse.newBuilder()
                        .setUserId(request.getUserId())
                        .setKycCompleted(false)
                        .setKycVerified(false)
                        .setKycStatus("PENDING")
                        .build();
                responseObserver.onNext(response);
                responseObserver.onCompleted();
                return;
            }

            String kycStatus = user.getKycCompleted() ? "COMPLETED" : "PENDING";
            if (user.getKycVerifiedAt() == null && user.getKycCompleted()) {
                kycStatus = "PENDING";
            }

            KycStatusResponse response = KycStatusResponse.newBuilder()
                    .setUserId(user.getUserId())
                    .setKycCompleted(user.getKycCompleted())
                    .setKycVerified(user.getKycVerifiedAt() != null)
                    .setKycStatus(kycStatus)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error checking KYC status for user: {}", request.getUserId(), e);
            responseObserver.onError(io.grpc.Status.INTERNAL
                    .withDescription("Error checking KYC status: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getUserEmail(GetUserRequest request, StreamObserver<UserEmailResponse> responseObserver) {
        log.debug("gRPC: GetUserEmail request for userId: {}", request.getUserId());

        User user = userRepository.findByIdAndNotDeleted(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found: " + request.getUserId()));

        UserEmailResponse response = UserEmailResponse.newBuilder()
                .setUserId(user.getUserId())
                .setEmail(user.getEmail())
                .setEmailVerified(user.getIsVerified())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    @Transactional(readOnly = true)
    public void getUserStatistics(GetUserStatisticsRequest request, StreamObserver<GetUserStatisticsResponse> responseObserver) {
        log.debug("gRPC: GetUserStatistics request");

        try {
            // Count total users (not deleted)
            long totalUsers = userRepository.countTotalUsers();
            
            // Count active users with active mining sessions (miningActive = true)
            // Note: This counts users with active mining sessions, not user account status
            long activeUsers = 0L;
            try {
                GetActiveAutoMiningsCountResponse miningResponse = miningServiceGrpcClient.getActiveAutoMiningsCount();
                activeUsers = miningResponse.getCount();
                log.debug("Active users with mining sessions: {}", activeUsers);
            } catch (Exception e) {
                log.warn("Failed to get active mining users count from mining service, using fallback: {}", e.getMessage());
                // Fallback: if mining service is unavailable, return 0 for active users
                // This ensures the endpoint doesn't fail completely
                activeUsers = 0L;
            }
            
            // Count blocked users (not deleted and isBlocked = true)
            long blockedUsers = userRepository.countBlockedUsers();

            GetUserStatisticsResponse response = GetUserStatisticsResponse.newBuilder()
                    .setTotalUsers(totalUsers)
                    .setActiveUsers(activeUsers)
                    .setBlockedUsers(blockedUsers)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting user statistics", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting user statistics: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getUsersByLevel(GetUsersByLevelRequest request, StreamObserver<GetUsersByLevelResponse> responseObserver) {
        log.debug("gRPC: GetUsersByLevel request");

        try {
            // Initialize all levels to 0
            Map<String, Long> usersByLevel = new HashMap<>();
            usersByLevel.put("BRONZE", 0L);
            usersByLevel.put("SILVER", 0L);
            usersByLevel.put("GOLD", 0L);
            usersByLevel.put("PLATINUM", 0L);
            usersByLevel.put("DIAMOND", 0L);
            usersByLevel.put("ELITE", 0L);

            // Count users by status level from UserReadModel (CQRS read model)
            // UserReadModel is updated via events from token-service
            List<String> statusLevels = List.of("BRONZE", "SILVER", "GOLD", "PLATINUM", "DIAMOND", "ELITE");
            for (String levelName : statusLevels) {
                try {
                    // Map to UserReadModel.UserStatusLevel enum
                    com.tuttime.user.entity.readmodel.UserReadModel.UserStatusLevel readModelLevel;
                    try {
                        readModelLevel = com.tuttime.user.entity.readmodel.UserReadModel.UserStatusLevel.valueOf(levelName);
                    } catch (IllegalArgumentException e) {
                        // Skip invalid levels
                        continue;
                    }
                    
                    long count = userReadModelRepository.countByStatusLevelAndActive(readModelLevel, true);
                    usersByLevel.put(levelName, count);
                } catch (Exception e) {
                    log.warn("Failed to count users for level {}: {}", levelName, e.getMessage());
                    // Continue with other levels
                }
            }

            // Build response
            GetUsersByLevelResponse.Builder responseBuilder = GetUsersByLevelResponse.newBuilder();
            usersByLevel.forEach(responseBuilder::putUsersByLevel);

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting users by level", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting users by level: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getTopUsersByApCredits(GetTopUsersByApCreditsRequest request, StreamObserver<GetTopUsersByApCreditsResponse> responseObserver) {
        log.debug("gRPC: GetTopUsersByApCredits request - limit: {}, period: {}", request.getLimit(), request.getPeriod());

        try {
            int limit = request.getLimit() > 0 ? request.getLimit() : 100;
            String period = request.getPeriod() != null && !request.getPeriod().isEmpty() ? request.getPeriod() : "TOTAL";
            
            // Query active users (not deleted, not blocked, isActive = true)
            List<User> activeUsers = userRepository.findAll().stream()
                    .filter(u -> u.getDeletedAt() == null && !u.getIsBlocked() && u.getIsActive())
                    .collect(Collectors.toList());
            
            // Create list to store users with their AP balances
            List<UserBalancePair> usersWithBalances = new ArrayList<>();
            
            // Get AP balance for each user from mining-service
            for (User user : activeUsers) {
                try {
                    ApBalanceResponse apBalance = miningServiceGrpcClient.getApBalance(user.getUserId());
                    BigDecimal balance = new BigDecimal(apBalance.getCurrentBalance());
                    
                    // Apply period filter if needed
                    if (shouldIncludeUserForPeriod(period, user)) {
                        usersWithBalances.add(new UserBalancePair(user, balance));
                    }
                } catch (Exception e) {
                    log.warn("Failed to get AP balance for user {}: {}", user.getUserId(), e.getMessage());
                    // Continue with other users even if one fails
                }
            }
            
            // Sort by balance descending and rank
            usersWithBalances.sort((a, b) -> b.balance.compareTo(a.balance));
            
            // Limit results
            int actualLimit = Math.min(limit, usersWithBalances.size());
            List<UserBalancePair> topUsers = usersWithBalances.subList(0, actualLimit);
            
            // Build response
            GetTopUsersByApCreditsResponse.Builder responseBuilder = GetTopUsersByApCreditsResponse.newBuilder();
            
            for (int i = 0; i < topUsers.size(); i++) {
                UserBalancePair pair = topUsers.get(i);
                User user = pair.user;
                
                // Get user status level from token-service (source of truth)
                String statusLevel = "BRONZE"; // Default fallback
                try {
                    GetUserStatusResponse statusResponse = tokenServiceGrpcClient.getUserStatus(user.getUserId());
                    statusLevel = statusResponse.getStatusLevel();
                } catch (Exception e) {
                    log.warn("Failed to get user status from token-service for user {}: {}, using default BRONZE", 
                            user.getUserId(), e.getMessage());
                }
                
                // Build username
                String username = buildUsername(user);
                
                TopUser topUser = TopUser.newBuilder()
                        .setUserId(user.getUserId())
                        .setUsername(username)
                        .setEmail(user.getEmail() != null ? user.getEmail() : "")
                        .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                        .setLastName(user.getLastName() != null ? user.getLastName() : "")
                        .setBalance(pair.balance.toString())
                        .setRank(i + 1)
                        .setCountry(user.getCountry() != null ? user.getCountry() : "")
                        .setCity(user.getCity() != null ? user.getCity() : "")
                        .setIsActive(user.getIsActive())
                        .setStatusLevel(statusLevel)
                        .build();
                
                responseBuilder.addUsers(topUser);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting top users by AP Credits", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting top users by AP Credits: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
    
    /**
     * Helper class to pair users with their balances for sorting
     */
    private static class UserBalancePair {
        final User user;
        final BigDecimal balance;
        
        UserBalancePair(User user, BigDecimal balance) {
            this.user = user;
            this.balance = balance;
        }
    }
    
    /**
     * Get username from user entity (username is required)
     */
    private String buildUsername(User user) {
        return user.getUsername();
    }
    
    /**
     * Check if user should be included based on period filter
     * For now, we include all users. Period filtering can be enhanced later.
     */
    private boolean shouldIncludeUserForPeriod(String period, User user) {
        // TODO: Implement period filtering based on registration date or activity
        // For now, include all active users
        return true;
    }

    // TODO: Uncomment when gRPC proto classes GetBlockedUsersRequest, GetBlockedUsersResponse, BlockedUserInfo are available
    /*
    @Override
    @Transactional(readOnly = true)
    public void getBlockedUsers(GetBlockedUsersRequest request, StreamObserver<GetBlockedUsersResponse> responseObserver) {
        log.debug("gRPC: GetBlockedUsers request - page: {}, size: {}", request.getPage(), request.getSize());

        try {
            int page = request.getPage() >= 0 ? request.getPage() : 0;
            int size = request.getSize() > 0 ? request.getSize() : 50;
            
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size);
            org.springframework.data.domain.Page<User> blockedUsersPage = userRepository.findBlockedUsers(pageable);
            
            GetBlockedUsersResponse.Builder responseBuilder = GetBlockedUsersResponse.newBuilder()
                    .setTotalPages(blockedUsersPage.getTotalPages())
                    .setTotalElements(blockedUsersPage.getTotalElements())
                    .setCurrentPage(blockedUsersPage.getNumber())
                    .setPageSize(blockedUsersPage.getSize());
            
            for (User user : blockedUsersPage.getContent()) {
                BlockedUserInfo.Builder userInfoBuilder = BlockedUserInfo.newBuilder()
                        .setUserId(user.getUserId())
                        .setEmail(user.getEmail() != null ? user.getEmail() : "");
                
                if (user.getPhone() != null) {
                    userInfoBuilder.setPhone(user.getPhone());
                }
                
                if (user.getBlockedReason() != null) {
                    userInfoBuilder.setBlockedReason(user.getBlockedReason());
                }
                
                if (user.getBlockedAt() != null) {
                    // Convert OffsetDateTime to Unix timestamp in milliseconds
                    long timestamp = user.getBlockedAt().toInstant().toEpochMilli();
                    userInfoBuilder.setBlockedAt(timestamp);
                }
                
                responseBuilder.addUsers(userInfoBuilder.build());
            }
            
            GetBlockedUsersResponse response = responseBuilder.build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            log.error("Error getting blocked users", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get blocked users: " + e.getMessage())
                    .asRuntimeException());
        }
    }
    */

    @Override
    @Transactional(readOnly = true)
    public void getTopUsersByApartBalance(GetTopUsersByApartBalanceRequest request, StreamObserver<GetTopUsersByApartBalanceResponse> responseObserver) {
        log.debug("gRPC: GetTopUsersByApartBalance request - limit: {}, period: {}", request.getLimit(), request.getPeriod());

        try {
            int limit = request.getLimit() > 0 ? request.getLimit() : 100;
            String period = request.getPeriod() != null && !request.getPeriod().isEmpty() ? request.getPeriod() : "TOTAL";
            
            // Query active users (not deleted, not blocked, isActive = true)
            List<User> activeUsers = userRepository.findAll().stream()
                    .filter(u -> u.getDeletedAt() == null && !u.getIsBlocked() && u.getIsActive())
                    .collect(Collectors.toList());
            
            // Create list to store users with their APART balances
            List<UserBalancePair> usersWithBalances = new ArrayList<>();
            
            // Get APART balance for each user from token-service
            for (User user : activeUsers) {
                try {
                    BalanceResponse balanceResponse = tokenServiceGrpcClient.getBalance(user.getUserId());
                    BigDecimal balance = new BigDecimal(balanceResponse.getBalance());
                    
                    // Apply period filter if needed
                    if (shouldIncludeUserForPeriod(period, user)) {
                        usersWithBalances.add(new UserBalancePair(user, balance));
                    }
                } catch (Exception e) {
                    log.warn("Failed to get APART balance for user {}: {}", user.getUserId(), e.getMessage());
                    // Continue with other users even if one fails
                }
            }
            
            // Sort by balance descending and rank
            usersWithBalances.sort((a, b) -> b.balance.compareTo(a.balance));
            
            // Limit results
            int actualLimit = Math.min(limit, usersWithBalances.size());
            List<UserBalancePair> topUsers = usersWithBalances.subList(0, actualLimit);
            
            // Build response
            GetTopUsersByApartBalanceResponse.Builder responseBuilder = GetTopUsersByApartBalanceResponse.newBuilder();
            
            for (int i = 0; i < topUsers.size(); i++) {
                UserBalancePair pair = topUsers.get(i);
                User user = pair.user;
                
                // Get user status level from token-service (source of truth)
                String statusLevel = "BRONZE"; // Default fallback
                try {
                    GetUserStatusResponse statusResponse = tokenServiceGrpcClient.getUserStatus(user.getUserId());
                    statusLevel = statusResponse.getStatusLevel();
                } catch (Exception e) {
                    log.warn("Failed to get user status from token-service for user {}: {}, using default BRONZE", 
                            user.getUserId(), e.getMessage());
                }
                
                // Build username
                String username = buildUsername(user);
                
                TopUser topUser = TopUser.newBuilder()
                        .setUserId(user.getUserId())
                        .setUsername(username)
                        .setEmail(user.getEmail() != null ? user.getEmail() : "")
                        .setFirstName(user.getFirstName() != null ? user.getFirstName() : "")
                        .setLastName(user.getLastName() != null ? user.getLastName() : "")
                        .setBalance(pair.balance.toString())
                        .setRank(i + 1)
                        .setCountry(user.getCountry() != null ? user.getCountry() : "")
                        .setCity(user.getCity() != null ? user.getCity() : "")
                        .setIsActive(user.getIsActive())
                        .setStatusLevel(statusLevel)
                        .build();
                
                responseBuilder.addUsers(topUser);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting top users by APART balance", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting top users by APART balance: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getGeolocationStats(GetGeolocationStatsRequest request, StreamObserver<GetGeolocationStatsResponse> responseObserver) {
        log.debug("gRPC: GetGeolocationStats request - country: {}, city: {}", request.getCountry(), request.getCity());

        try {
            String country = request.getCountry() != null && !request.getCountry().isEmpty() 
                    ? request.getCountry().trim() : null;
            String city = request.getCity() != null && !request.getCity().isEmpty() 
                    ? request.getCity().trim() : null;
            
            log.debug("gRPC: GetGeolocationStats - parsed country: '{}', city: '{}'", country, city);
            
            // Query users by location (case-insensitive matching)
            List<User> users;
            if (country != null && city != null) {
                // Query by both country and city (case-insensitive)
                log.debug("gRPC: Filtering by both country and city");
                users = userRepository.findAll().stream()
                        .filter(u -> {
                            boolean matches = u.getDeletedAt() == null && u.getIsActive() && 
                                    u.getCountry() != null && u.getCity() != null &&
                                    country.equalsIgnoreCase(u.getCountry().trim()) && 
                                    city.equalsIgnoreCase(u.getCity().trim());
                            if (matches) {
                                log.debug("gRPC: Found matching user: userId={}, country={}, city={}", 
                                        u.getUserId(), u.getCountry(), u.getCity());
                            }
                            return matches;
                        })
                        .collect(Collectors.toList());
                log.debug("gRPC: Found {} users matching country='{}' and city='{}'", users.size(), country, city);
            } else if (country != null) {
                // Query by country only (case-insensitive)
                log.debug("gRPC: Filtering by country only");
                users = userRepository.findAll().stream()
                        .filter(u -> {
                            boolean matches = u.getDeletedAt() == null && u.getIsActive() && 
                                    u.getCountry() != null &&
                                    country.equalsIgnoreCase(u.getCountry().trim());
                            if (matches) {
                                log.debug("gRPC: Found matching user: userId={}, country={}, city={}", 
                                        u.getUserId(), u.getCountry(), u.getCity());
                            }
                            return matches;
                        })
                        .collect(Collectors.toList());
                log.debug("gRPC: Found {} users matching country='{}'", users.size(), country);
            } else if (city != null) {
                // Query by city only (case-insensitive)
                log.debug("gRPC: Filtering by city only");
                users = userRepository.findAll().stream()
                        .filter(u -> {
                            boolean matches = u.getDeletedAt() == null && u.getIsActive() && 
                                    u.getCity() != null &&
                                    city.equalsIgnoreCase(u.getCity().trim());
                            if (matches) {
                                log.debug("gRPC: Found matching user: userId={}, country={}, city={}", 
                                        u.getUserId(), u.getCountry(), u.getCity());
                            }
                            return matches;
                        })
                        .collect(Collectors.toList());
                log.debug("gRPC: Found {} users matching city='{}'", users.size(), city);
            } else {
                // Get all active users and group by location
                log.debug("gRPC: Getting all active users with country");
                users = userRepository.findAll().stream()
                        .filter(u -> u.getDeletedAt() == null && u.getIsActive() && 
                                u.getCountry() != null && !u.getCountry().trim().isEmpty())
                        .collect(Collectors.toList());
                log.debug("gRPC: Found {} active users with country", users.size());
            }
            
            if (users.isEmpty()) {
                log.warn("gRPC: No users found matching criteria - country: '{}', city: '{}'", country, city);
            }
            
            // Group users by location (country, city)
            Map<String, List<User>> usersByLocation = users.stream()
                    .collect(Collectors.groupingBy(u -> {
                        String locCountry = (u.getCountry() != null && !u.getCountry().trim().isEmpty()) 
                                ? u.getCountry().trim() : "Unknown";
                        String locCity = (u.getCity() != null && !u.getCity().trim().isEmpty()) 
                                ? u.getCity().trim() : "Unknown";
                        return locCountry + "|" + locCity;
                    }));
            
            log.debug("gRPC: Grouped users into {} locations", usersByLocation.size());
            
            // Build response with aggregated stats per location
            GetGeolocationStatsResponse.Builder responseBuilder = GetGeolocationStatsResponse.newBuilder();
            
            for (Map.Entry<String, List<User>> entry : usersByLocation.entrySet()) {
                String[] locationParts = entry.getKey().split("\\|");
                String locCountry = locationParts[0];
                String locCity = locationParts.length > 1 ? locationParts[1] : "Unknown";
                List<User> locationUsers = entry.getValue();
                
                // Aggregate statistics for this location
                long usersCount = locationUsers.size();
                long activeMinersCount = 0;
                BigDecimal totalApMiningSpeed = BigDecimal.ZERO;
                BigDecimal totalApartReserve = BigDecimal.ZERO;
                BigDecimal totalStakedAmount = BigDecimal.ZERO;
                
                // Get last conversion summary for distributed amount
                BigDecimal lastConversionDistributedAmount = BigDecimal.ZERO;
                try {
                    // Use conversion service to get last conversion summary
                    // For now, we'll calculate from user conversions
                    // In production, this could be optimized with a dedicated endpoint
                } catch (Exception e) {
                    log.warn("Failed to get conversion data for location: {}", entry.getKey(), e);
                }
                
                // Aggregate data for each user in this location
                int usersWithData = 0;
                for (User user : locationUsers) {
                    try {
                        // Get AP balance and mining status
                        ApBalanceResponse apBalance = miningServiceGrpcClient.getApBalance(user.getUserId());
                        if (apBalance.getMiningActive()) {
                            activeMinersCount++;
                        }
                        
                        // Get mining speed (approximate from balance or use a default)
                        // Note: Actual mining speed calculation would require calling getMiningSpeed
                        // For performance, we'll use a simplified approach
                        // TODO: Call getMiningSpeed for accurate speed calculation
                        // For now, use a default speed estimate
                        totalApMiningSpeed = totalApMiningSpeed.add(new BigDecimal("1.0")); // Placeholder
                        
                        // Get APART balance
                        BalanceResponse balanceResponse = tokenServiceGrpcClient.getBalance(user.getUserId());
                        totalApartReserve = totalApartReserve.add(new BigDecimal(balanceResponse.getBalance()));
                        
                        // Get staking positions
                        StakingPositionsResponse stakingPositions = stakingServiceGrpcClient.getStakingPositions(user.getUserId());
                        for (StakingPosition position : stakingPositions.getPositionsList()) {
                            if ("ACTIVE".equals(position.getStatus())) {
                                totalStakedAmount = totalStakedAmount.add(new BigDecimal(position.getAmount()));
                            }
                        }
                        
                        usersWithData++;
                    } catch (Exception e) {
                        log.warn("Failed to get data for user {} in location {}: {}", 
                                user.getUserId(), entry.getKey(), e.getMessage());
                        // Continue with other users
                    }
                }
                
                // Calculate averages
                BigDecimal averageApMiningSpeed = usersWithData > 0 
                        ? totalApMiningSpeed.divide(BigDecimal.valueOf(usersWithData), 2, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                BigDecimal averageApartReserve = usersWithData > 0
                        ? totalApartReserve.divide(BigDecimal.valueOf(usersWithData), 18, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO;
                
                // Build geolocation stat
                GeolocationStat stat = GeolocationStat.newBuilder()
                        .setCountry(locCountry)
                        .setCity(locCity)
                        .setUsersCount(usersCount)
                        .setActiveMinersCount(activeMinersCount)
                        .setAverageApMiningSpeed(averageApMiningSpeed.toString())
                        .setAverageApartReserve(averageApartReserve.toString())
                        .setLastConversionDistributedAmount(lastConversionDistributedAmount.toString())
                        .setTotalStakedAmount(totalStakedAmount.toString())
                        .build();
                
                responseBuilder.addStats(stat);
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting geolocation stats", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting geolocation stats: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    public void getUsersOnlineStatus(GetUsersOnlineStatusRequest request, StreamObserver<GetUsersOnlineStatusResponse> responseObserver) {
        log.debug("gRPC: GetUsersOnlineStatus request for {} users", request.getUserIdsCount());

        try {
            // Convert proto list to Java list
            List<Long> userIds = request.getUserIdsList().stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            // Get online status from Redis
            Map<Long, Boolean> onlineStatusMap = onlineStatusService.getUsersOnlineStatus(userIds);

            // Build response
            GetUsersOnlineStatusResponse.Builder responseBuilder = GetUsersOnlineStatusResponse.newBuilder();
            
            // Add all user IDs to response (even if not online)
            for (Long userId : userIds) {
                boolean isOnline = onlineStatusMap.getOrDefault(userId, false);
                responseBuilder.putOnlineStatus(userId, isOnline);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Error getting users online status", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get users online status: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getOnlineUsersCount(GetOnlineUsersCountRequest request, StreamObserver<GetOnlineUsersCountResponse> responseObserver) {
        log.debug("gRPC: GetOnlineUsersCount request");

        try {
            // Count total users (not deleted)
            long totalUsers = userRepository.countTotalUsers();
            
            // Count online users
            long onlineUsers = onlineStatusService.getOnlineUsersCount();

            GetOnlineUsersCountResponse response = GetOnlineUsersCountResponse.newBuilder()
                    .setTotalUsers(totalUsers)
                    .setOnlineUsers(onlineUsers)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting online users count", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting online users count: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getApartSharingStatus(GetUserRequest request, StreamObserver<ApartSharingStatusResponse> responseObserver) {
        log.debug("gRPC: GetApartSharingStatus request for userId: {}", request.getUserId());

        try {
            java.util.Map<String, Object> status = apartSharingStatusService.getApartSharingStatus(request.getUserId());
            
            ApartSharingStatusResponse.Builder responseBuilder = ApartSharingStatusResponse.newBuilder()
                    .setUserId(request.getUserId());
            
            if (status.containsKey("error")) {
                responseBuilder.setError((String) status.get("error"));
            } else {
                responseBuilder.setIsRegistered((Boolean) status.getOrDefault("isRegistered", false));
                responseBuilder.setIsVerified((Boolean) status.getOrDefault("isVerified", false));
                responseBuilder.setIsBlocked((Boolean) status.getOrDefault("isBlocked", false));
                responseBuilder.setIsPhoneConfirmed((Boolean) status.getOrDefault("isPhoneConfirmed", false));
                
                if (status.containsKey("status")) {
                    responseBuilder.setStatus((String) status.get("status"));
                }
                if (status.containsKey("statusMessage") && status.get("statusMessage") != null) {
                    responseBuilder.setStatusMessage((String) status.get("statusMessage"));
                }
                if (status.containsKey("verifiedStatusTitle")) {
                    responseBuilder.setVerifiedStatusTitle((String) status.get("verifiedStatusTitle"));
                }
                if (status.containsKey("verifiedStatusColor")) {
                    responseBuilder.setVerifiedStatusColor((String) status.get("verifiedStatusColor"));
                }
                if (status.containsKey("statusCheckedAt")) {
                    Object checkedAt = status.get("statusCheckedAt");
                    if (checkedAt instanceof java.time.OffsetDateTime) {
                        long timestamp = ((java.time.OffsetDateTime) checkedAt).toInstant().toEpochMilli();
                        responseBuilder.setStatusCheckedAt(timestamp);
                    }
                }
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Error getting Apart-sharing status for user: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get Apart-sharing status: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void refreshApartSharingStatus(GetUserRequest request, StreamObserver<ApartSharingStatusResponse> responseObserver) {
        log.debug("gRPC: RefreshApartSharingStatus request for userId: {}", request.getUserId());

        try {
            java.util.Map<String, Object> status = apartSharingStatusService.refreshApartSharingStatus(request.getUserId());
            
            ApartSharingStatusResponse.Builder responseBuilder = ApartSharingStatusResponse.newBuilder()
                    .setUserId(request.getUserId());
            
            if (status.containsKey("error")) {
                responseBuilder.setError((String) status.get("error"));
            } else {
                responseBuilder.setIsRegistered((Boolean) status.getOrDefault("isRegistered", false));
                responseBuilder.setIsVerified((Boolean) status.getOrDefault("isVerified", false));
                responseBuilder.setIsBlocked((Boolean) status.getOrDefault("isBlocked", false));
                responseBuilder.setIsPhoneConfirmed((Boolean) status.getOrDefault("isPhoneConfirmed", false));
                
                if (status.containsKey("status")) {
                    responseBuilder.setStatus((String) status.get("status"));
                }
                if (status.containsKey("statusMessage") && status.get("statusMessage") != null) {
                    responseBuilder.setStatusMessage((String) status.get("statusMessage"));
                }
                if (status.containsKey("verifiedStatusTitle")) {
                    responseBuilder.setVerifiedStatusTitle((String) status.get("verifiedStatusTitle"));
                }
                if (status.containsKey("verifiedStatusColor")) {
                    responseBuilder.setVerifiedStatusColor((String) status.get("verifiedStatusColor"));
                }
                if (status.containsKey("statusCheckedAt")) {
                    Object checkedAt = status.get("statusCheckedAt");
                    if (checkedAt instanceof java.time.OffsetDateTime) {
                        long timestamp = ((java.time.OffsetDateTime) checkedAt).toInstant().toEpochMilli();
                        responseBuilder.setStatusCheckedAt(timestamp);
                    }
                }
            }
            
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Error refreshing Apart-sharing status for user: {}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to refresh Apart-sharing status: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getUserFcmTokens(GetUserRequest request, StreamObserver<GetUserFcmTokensResponse> responseObserver) {
        log.debug("gRPC: GetUserFcmTokens request for userId: {}", request.getUserId());

        try {
            List<String> fcmTokens = userService.getUserFcmTokens(request.getUserId());
            
            GetUserFcmTokensResponse response = GetUserFcmTokensResponse.newBuilder()
                    .addAllFcmTokens(fcmTokens)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Error getting FCM tokens for user: userId={}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get FCM tokens: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional
    public void removeInvalidFcmTokens(RemoveInvalidFcmTokensRequest request, StreamObserver<RemoveInvalidFcmTokensResponse> responseObserver) {
        log.debug("gRPC: RemoveInvalidFcmTokens request for userId: {}, count: {}", 
                request.getUserId(), request.getInvalidTokensCount());

        try {
            List<String> invalidTokens = request.getInvalidTokensList();
            int removedCount = userService.removeInvalidFcmTokens(request.getUserId(), invalidTokens);
            
            RemoveInvalidFcmTokensResponse response = RemoveInvalidFcmTokensResponse.newBuilder()
                    .setUserId(request.getUserId())
                    .setRemovedCount(removedCount)
                    .setMessage(String.format("Removed %d invalid FCM token(s)", removedCount))
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Error removing invalid FCM tokens for user: userId={}", request.getUserId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to remove invalid FCM tokens: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void checkUserBlocks(CheckUserBlocksRequest request, StreamObserver<CheckUserBlocksResponse> responseObserver) {
        log.debug("gRPC: CheckUserBlocks request for requesterUserId: {}, targetUserIds count: {}", 
                request.getRequesterUserId(), request.getTargetUserIdsCount());

        try {
            Long requesterUserId = request.getRequesterUserId();
            List<Long> targetUserIds = request.getTargetUserIdsList().stream()
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            // Build response map
            CheckUserBlocksResponse.Builder responseBuilder = CheckUserBlocksResponse.newBuilder();
            
            // Check block relationship for each target user
            for (Long targetUserId : targetUserIds) {
                // Check bidirectional block: if either user has blocked the other
                boolean isBlocked = userBlockService.isBlocked(requesterUserId, targetUserId);
                responseBuilder.putBlockedStatus(targetUserId, isBlocked);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Error checking user blocks for requesterUserId: {}", request.getRequesterUserId(), e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to check user blocks: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    public void getUserIdsByUsernames(GetUserIdsByUsernamesRequest request, StreamObserver<GetUserIdsByUsernamesResponse> responseObserver) {
        log.debug("gRPC: GetUserIdsByUsernames request for {} usernames", request.getUsernamesCount());

        try {
            if (request.getUsernamesCount() == 0) {
                GetUserIdsByUsernamesResponse emptyResponse = GetUserIdsByUsernamesResponse.newBuilder().build();
                responseObserver.onNext(emptyResponse);
                responseObserver.onCompleted();
                return;
            }

            // Limit to 100 usernames to prevent abuse; lowercase for case-insensitive JPQL IN match
            List<String> usernames = request.getUsernamesList().stream()
                    .limit(100)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            if (usernames.isEmpty()) {
                GetUserIdsByUsernamesResponse emptyResponse = GetUserIdsByUsernamesResponse.newBuilder().build();
                responseObserver.onNext(emptyResponse);
                responseObserver.onCompleted();
                return;
            }

            // Find users by usernames
            List<User> users = userRepository.findByUsernameInAndNotDeleted(usernames);

            // Build response map
            GetUserIdsByUsernamesResponse.Builder responseBuilder = GetUserIdsByUsernamesResponse.newBuilder();
            for (User user : users) {
                responseBuilder.putUsernameToUserId(user.getUsername(), user.getUserId());
            }

            log.debug("gRPC: GetUserIdsByUsernames found {} users out of {} requested usernames", 
                    users.size(), usernames.size());

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Error getting user IDs by usernames", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Failed to get user IDs by usernames: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getRegistrationsPerDay(GetRegistrationsPerDayRequest request, StreamObserver<GetRegistrationsPerDayResponse> responseObserver) {
        log.debug("gRPC: GetRegistrationsPerDay request - startDate: {}, endDate: {}", request.getStartDate(), request.getEndDate());

        try {
            // Parse date parameters
            java.time.LocalDate startDate;
            java.time.LocalDate endDate;
            
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                startDate = java.time.LocalDate.parse(request.getStartDate());
            } else {
                // Default to 30 days ago
                startDate = java.time.LocalDate.now().minusDays(30);
            }
            
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                endDate = java.time.LocalDate.parse(request.getEndDate());
            } else {
                // Default to today
                endDate = java.time.LocalDate.now();
            }

            // Query registrations per day
            List<Object[]> results = userRepository.findRegistrationsPerDay(startDate, endDate);

            // Build response
            GetRegistrationsPerDayResponse.Builder responseBuilder = GetRegistrationsPerDayResponse.newBuilder();
            
            for (Object[] result : results) {
                // result[0] is the date (java.sql.Date or LocalDate)
                // result[1] is the count (Long or BigInteger)
                java.time.LocalDate date;
                if (result[0] instanceof java.sql.Date) {
                    date = ((java.sql.Date) result[0]).toLocalDate();
                } else if (result[0] instanceof java.time.LocalDate) {
                    date = (java.time.LocalDate) result[0];
                } else {
                    // Try to parse as string
                    date = java.time.LocalDate.parse(result[0].toString());
                }
                
                long count;
                if (result[1] instanceof Number) {
                    count = ((Number) result[1]).longValue();
                } else {
                    count = Long.parseLong(result[1].toString());
                }
                
                RegistrationDayStat stat = RegistrationDayStat.newBuilder()
                        .setDate(date.toString())
                        .setCount(count)
                        .build();
                
                responseBuilder.addStats(stat);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Error getting registrations per day", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting registrations per day: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void getRegistrationsPerMonth(GetRegistrationsPerMonthRequest request, StreamObserver<GetRegistrationsPerMonthResponse> responseObserver) {
        log.debug("gRPC: GetRegistrationsPerMonth request - startDate: {}, endDate: {}", request.getStartDate(), request.getEndDate());

        try {
            // Parse date parameters
            java.time.LocalDate startDate;
            java.time.LocalDate endDate;
            
            if (request.getStartDate() != null && !request.getStartDate().isEmpty()) {
                startDate = java.time.LocalDate.parse(request.getStartDate());
            } else {
                // Default to 12 months ago (first day of month)
                startDate = java.time.LocalDate.now().minusMonths(12).withDayOfMonth(1);
            }
            
            if (request.getEndDate() != null && !request.getEndDate().isEmpty()) {
                endDate = java.time.LocalDate.parse(request.getEndDate());
            } else {
                // Default to today
                endDate = java.time.LocalDate.now();
            }

            // Query registrations per month
            List<Object[]> results = userRepository.findRegistrationsPerMonth(startDate, endDate);

            // Build response
            GetRegistrationsPerMonthResponse.Builder responseBuilder = GetRegistrationsPerMonthResponse.newBuilder();
            
            for (Object[] result : results) {
                // result[0] is the month string (YYYY-MM format)
                // result[1] is the count (Long or BigInteger)
                String month = result[0].toString();
                
                long count;
                if (result[1] instanceof Number) {
                    count = ((Number) result[1]).longValue();
                } else {
                    count = Long.parseLong(result[1].toString());
                }
                
                RegistrationMonthStat stat = RegistrationMonthStat.newBuilder()
                        .setMonth(month)
                        .setCount(count)
                        .build();
                
                responseBuilder.addStats(stat);
            }

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("gRPC: Error getting registrations per month", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("Error getting registrations per month: " + e.getMessage())
                    .withCause(e)
                    .asRuntimeException());
        }
    }
}

