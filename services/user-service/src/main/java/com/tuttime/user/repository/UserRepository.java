package com.tuttime.user.repository;

import com.tuttime.user.entity.LocationPrivacyLevel;
import com.tuttime.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmail(@Param("email") String email);

    @Query("SELECT u FROM User u WHERE u.phone = :phone AND u.deletedAt IS NULL")
    Optional<User> findByPhone(@Param("phone") String phone);

    /**
     * Find user by phone number (including soft-deleted users)
     * Used for admin operations like beta tester deletion
     */
    @Query("SELECT u FROM User u WHERE u.phone = :phone")
    Optional<User> findByPhoneIncludingDeleted(@Param("phone") String phone);

    @Query("SELECT u FROM User u WHERE LOWER(u.username) = LOWER(:username) AND u.deletedAt IS NULL")
    Optional<User> findByUsername(@Param("username") String username);

    @Query("SELECT u FROM User u WHERE u.userId = :userId AND u.deletedAt IS NULL")
    Optional<User> findByIdAndNotDeleted(@Param("userId") Long userId);

    /**
     * Find users within bounding box (for map display)
     * 
     * @param minLat Minimum latitude
     * @param minLng Minimum longitude
     * @param maxLat Maximum latitude
     * @param maxLng Maximum longitude
     * @param pageable Pagination parameters
     * @return Page of users within bounds
     */
    @Query(value = """
        SELECT u.* FROM user_service.users u 
        WHERE u.location_sharing_enabled = true 
        AND LOWER(u.location_privacy_level) != LOWER(CAST(:privateLevel AS VARCHAR))
        AND u.location_point IS NOT NULL
        AND ST_Within(
            u.location_point::geometry,
            ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
        )
        AND u.deleted_at IS NULL
        AND u.is_active = true
        ORDER BY u.location_updated_at DESC
        """, nativeQuery = true, countQuery = """
        SELECT COUNT(*) FROM user_service.users u 
        WHERE u.location_sharing_enabled = true 
        AND LOWER(u.location_privacy_level) != LOWER(CAST(:privateLevel AS VARCHAR))
        AND u.location_point IS NOT NULL
        AND ST_Within(
            u.location_point::geometry,
            ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
        )
        AND u.deleted_at IS NULL
        AND u.is_active = true
        """)
    Page<User> findUsersInBounds(
        @Param("minLat") double minLat,
        @Param("minLng") double minLng,
        @Param("maxLat") double maxLat,
        @Param("maxLng") double maxLng,
        @Param("privateLevel") LocationPrivacyLevel privateLevel,
        Pageable pageable
    );

    /**
     * Find users within radius (nearby search)
     * 
     * @param lat Center latitude
     * @param lng Center longitude
     * @param radiusMeters Radius in meters
     * @param pageable Pagination parameters
     * @return Page of users within radius, ordered by distance
     */
    @Query(value = """
        SELECT u.* FROM user_service.users u 
        WHERE u.location_sharing_enabled = true 
        AND LOWER(u.location_privacy_level) != LOWER(CAST(:privateLevel AS VARCHAR))
        AND u.location_point IS NOT NULL
        AND ST_DWithin(
            u.location_point::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
            :radiusMeters
        )
        AND u.deleted_at IS NULL
        AND u.is_active = true
        ORDER BY ST_Distance(
            u.location_point::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
        )
        """, nativeQuery = true, countQuery = """
        SELECT COUNT(*) FROM user_service.users u 
        WHERE u.location_sharing_enabled = true 
        AND LOWER(u.location_privacy_level) != LOWER(CAST(:privateLevel AS VARCHAR))
        AND u.location_point IS NOT NULL
        AND ST_DWithin(
            u.location_point::geography,
            ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
            :radiusMeters
        )
        AND u.deleted_at IS NULL
        AND u.is_active = true
        """)
    Page<User> findUsersNearby(
        @Param("lat") double lat,
        @Param("lng") double lng,
        @Param("radiusMeters") int radiusMeters,
        @Param("privateLevel") LocationPrivacyLevel privateLevel,
        Pageable pageable
    );

    /**
     * Find users by country and city
     * 
     * @param country Country name
     * @param city City name (optional, can be null)
     * @param pageable Pagination parameters
     * @return Page of users in country/city
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.locationSharingEnabled = true 
        AND u.locationPrivacyLevel != :privateLevel
        AND u.country = :country
        AND COALESCE(:city, u.city) = u.city
        AND u.deletedAt IS NULL
        AND u.isActive = true
        ORDER BY u.locationUpdatedAt DESC
        """)
    Page<User> findByCountryAndCityAndLocationSharingEnabled(
        @Param("country") String country,
        @Param("city") String city,
        @Param("privateLevel") LocationPrivacyLevel privateLevel,
        Pageable pageable
    );

    /**
     * Find users by country only
     * 
     * @param country Country name
     * @param pageable Pagination parameters
     * @return Page of users in country
     */
    @Query("""
        SELECT u FROM User u 
        WHERE u.locationSharingEnabled = true 
        AND u.locationPrivacyLevel != :privateLevel
        AND u.country = :country
        AND u.deletedAt IS NULL
        AND u.isActive = true
        ORDER BY u.locationUpdatedAt DESC
        """)
    Page<User> findByCountryAndLocationSharingEnabled(
        @Param("country") String country,
        @Param("privateLevel") LocationPrivacyLevel privateLevel,
        Pageable pageable
    );

    /**
     * Count total users (not deleted)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL")
    long countTotalUsers();

    /**
     * Count active users (not deleted and isActive = true)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.isActive = true")
    long countActiveUsers();

    /**
     * Count blocked users (not deleted and isBlocked = true)
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.deletedAt IS NULL AND u.isBlocked = true")
    long countBlockedUsers();

    /**
     * Get registrations per day for a date range.
     * Groups by DATE(registration_date) and counts registrations per day.
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (exclusive, will be converted to start of next day)
     * @return List of Object arrays: [date (LocalDate), count (Long)]
     */
    @Query(value = """
        SELECT 
            DATE(registration_date) as date,
            COUNT(*) as count
        FROM user_service.users
        WHERE deleted_at IS NULL
            AND registration_date >= CAST(:startDate AS TIMESTAMP WITH TIME ZONE)
            AND registration_date < CAST(:endDate AS TIMESTAMP WITH TIME ZONE) + INTERVAL '1 day'
        GROUP BY DATE(registration_date)
        ORDER BY date ASC
        """, nativeQuery = true)
    List<Object[]> findRegistrationsPerDay(
        @Param("startDate") java.time.LocalDate startDate,
        @Param("endDate") java.time.LocalDate endDate
    );

    /**
     * Get registrations per month for a date range.
     * Groups by YEAR(registration_date), MONTH(registration_date) and counts registrations per month.
     * 
     * @param startDate Start date (inclusive)
     * @param endDate End date (exclusive, will be converted to start of next day)
     * @return List of Object arrays: [month (String in YYYY-MM format), count (Long)]
     */
    @Query(value = """
        SELECT 
            TO_CHAR(registration_date, 'YYYY-MM') as month,
            COUNT(*) as count
        FROM user_service.users
        WHERE deleted_at IS NULL
            AND registration_date >= CAST(:startDate AS TIMESTAMP WITH TIME ZONE)
            AND registration_date < CAST(:endDate AS TIMESTAMP WITH TIME ZONE) + INTERVAL '1 day'
        GROUP BY TO_CHAR(registration_date, 'YYYY-MM')
        ORDER BY month ASC
        """, nativeQuery = true)
    List<Object[]> findRegistrationsPerMonth(
        @Param("startDate") java.time.LocalDate startDate,
        @Param("endDate") java.time.LocalDate endDate
    );
    
    /**
     * Find all blocked users (not deleted and isBlocked = true) with pagination
     */
    @Query("SELECT u FROM User u WHERE u.deletedAt IS NULL AND u.isBlocked = true ORDER BY u.blockedAt DESC")
    Page<User> findBlockedUsers(Pageable pageable);
    
    /**
     * Find users with search and filters for admin
     * Search by email, phone, or username (firstName + lastName)
     * Filter by status (isActive) and KYC status (kycCompleted)
     * Optionally include soft-deleted users (includeDeleted = true)
     * 
     * Note: Using native query to avoid PostgreSQL parameter type inference issues
     */
    @Query(value = """
        SELECT u.* FROM user_service.users u 
        WHERE (:includeDeleted = true OR u.deleted_at IS NULL)
        AND (COALESCE(:search, '') = '' OR 
             LOWER(u.email) LIKE LOWER('%' || COALESCE(:search, '') || '%') OR
             LOWER(u.phone) LIKE LOWER('%' || COALESCE(:search, '') || '%') OR
             LOWER(u.username) LIKE LOWER('%' || COALESCE(:search, '') || '%') OR
             LOWER(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')) LIKE LOWER('%' || COALESCE(:search, '') || '%'))
        AND (COALESCE(:status, '') = '' OR 
             (:status = 'ACTIVE' AND u.is_active = true AND u.is_blocked = false) OR
             (:status = 'INACTIVE' AND u.is_active = false) OR
             (:status = 'BLOCKED' AND u.is_blocked = true) OR
             (:status = 'DELETED' AND u.deleted_at IS NOT NULL))
        AND (COALESCE(:kycStatus, '') = '' OR 
             (:kycStatus = 'COMPLETED' AND u.kyc_completed = true) OR
             (:kycStatus = 'PENDING' AND u.kyc_completed = false))
        ORDER BY u.created_at DESC
        """, 
        countQuery = """
        SELECT COUNT(*) FROM user_service.users u 
        WHERE (:includeDeleted = true OR u.deleted_at IS NULL)
        AND (COALESCE(:search, '') = '' OR 
             LOWER(u.email) LIKE LOWER('%' || COALESCE(:search, '') || '%') OR
             LOWER(u.phone) LIKE LOWER('%' || COALESCE(:search, '') || '%') OR
             LOWER(u.username) LIKE LOWER('%' || COALESCE(:search, '') || '%') OR
             LOWER(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')) LIKE LOWER('%' || COALESCE(:search, '') || '%'))
        AND (COALESCE(:status, '') = '' OR 
             (:status = 'ACTIVE' AND u.is_active = true AND u.is_blocked = false) OR
             (:status = 'INACTIVE' AND u.is_active = false) OR
             (:status = 'BLOCKED' AND u.is_blocked = true) OR
             (:status = 'DELETED' AND u.deleted_at IS NOT NULL))
        AND (COALESCE(:kycStatus, '') = '' OR 
             (:kycStatus = 'COMPLETED' AND u.kyc_completed = true) OR
             (:kycStatus = 'PENDING' AND u.kyc_completed = false))
        """,
        nativeQuery = true)
    Page<User> findUsersWithFilters(
        @Param("search") String search,
        @Param("status") String status,
        @Param("kycStatus") String kycStatus,
        @Param("includeDeleted") boolean includeDeleted,
        Pageable pageable
    );

    /**
     * Check uniqueness constraints for user registration in a single query.
     * Returns a projection containing which fields already exist.
     * 
     * <p>This method performs all uniqueness checks (email, phone, username) in one database query
     * instead of multiple sequential queries, significantly reducing latency.
     * 
     * <p><b>Important:</b> Phone numbers are checked across ALL users (including deleted accounts)
     * to prevent re-registration with the same phone number. Email and username are only checked
     * for non-deleted accounts.
     * 
     * @param email Email to check (required)
     * @param phone Phone to check (optional, can be null)
     * @param username Username to check (required)
     * @return UserUniquenessCheckProjection containing existence flags for each field
     */
    @Query(value = """
        SELECT 
            COUNT(CASE WHEN u.email = :email AND u.deleted_at IS NULL THEN 1 END) > 0 as emailExists,
            CASE 
                WHEN COALESCE(CAST(:phone AS VARCHAR), '') = '' THEN false
                ELSE COUNT(CASE WHEN u.phone = CAST(:phone AS VARCHAR) THEN 1 END) > 0
            END as phoneExists,
            COUNT(CASE WHEN LOWER(u.username) = LOWER(:username) AND u.deleted_at IS NULL THEN 1 END) > 0 as usernameExists
        FROM user_service.users u
        WHERE (
            (u.deleted_at IS NULL AND u.email = :email) 
            OR (COALESCE(CAST(:phone AS VARCHAR), '') != '' AND u.phone = CAST(:phone AS VARCHAR))
            OR (u.deleted_at IS NULL AND LOWER(u.username) = LOWER(:username))
        )
        """, nativeQuery = true)
    UserUniquenessCheckProjection checkUserUniqueness(
        @Param("email") String email,
        @Param("phone") String phone,
        @Param("username") String username
    );

    /**
     * Search users by query string (username, first_name, last_name).
     * Only returns active, non-blocked, non-deleted users.
     * 
     * @param query Search query string
     * @param pageable Pagination parameters
     * @return Page of users matching the query
     */
    @Query(value = """
        SELECT u.* FROM user_service.users u 
        WHERE u.deleted_at IS NULL
        AND u.is_active = true
        AND u.is_blocked = false
        AND (LOWER(u.username) LIKE LOWER('%' || :query || '%') OR
             LOWER(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')) LIKE LOWER('%' || :query || '%'))
        ORDER BY 
            CASE 
                WHEN LOWER(u.username) = LOWER(:query) THEN 1
                WHEN LOWER(u.username) LIKE LOWER(:query || '%') THEN 2
                WHEN LOWER(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')) = LOWER(:query) THEN 3
                ELSE 4
            END,
            u.username ASC
        """, 
        countQuery = """
        SELECT COUNT(*) FROM user_service.users u 
        WHERE u.deleted_at IS NULL
        AND u.is_active = true
        AND u.is_blocked = false
        AND (LOWER(u.username) LIKE LOWER('%' || :query || '%') OR
             LOWER(COALESCE(u.first_name, '') || ' ' || COALESCE(u.last_name, '')) LIKE LOWER('%' || :query || '%'))
        """,
        nativeQuery = true)
    Page<User> searchUsers(@Param("query") String query, Pageable pageable);

    /**
     * Find users by list of usernames (for mention validation).
     * Only returns active, non-blocked, non-deleted users.
     * 
     * <p><b>Important:</b> Callers MUST pass <em>lowercased</em> usernames because
     * the query performs an exact match against {@code LOWER(u.username)}.
     * 
     * <p>Uses native query to avoid Hibernate 6 array parameter binding issue
     * with PostgreSQL ({@code unnest(CAST(? AS text[]))} fails with plain strings).
     * 
     * @param usernames List of <b>lowercased</b> usernames to find
     * @return List of users matching the usernames
     */
    @Query(value = "SELECT u.* FROM users u WHERE LOWER(u.username) IN (:usernames) " +
           "AND u.deleted_at IS NULL AND u.is_active = true AND u.is_blocked = false",
           nativeQuery = true)
    List<User> findByUsernameInAndNotDeleted(@Param("usernames") List<String> usernames);
    
    /**
     * Find all active user IDs (not deleted, active, not blocked)
     * Returns only user IDs for performance
     */
    @Query("SELECT u.userId FROM User u WHERE u.deletedAt IS NULL AND u.isActive = true AND u.isBlocked = false")
    List<Long> findActiveUserIdsNotDeleted();
    
    /**
     * Find all user IDs (not deleted)
     * Returns only user IDs for performance
     */
    @Query("SELECT u.userId FROM User u WHERE u.deletedAt IS NULL")
    List<Long> findUserIdsNotDeleted();
    
    /**
     * Find all active user IDs (including deleted if needed)
     * Returns only user IDs for performance
     */
    @Query("SELECT u.userId FROM User u WHERE u.isActive = true AND u.isBlocked = false")
    List<Long> findUserIdsByIsActiveTrue();
    
    /**
     * Find all user IDs (including deleted)
     * Returns only user IDs for performance
     */
    @Query("SELECT u.userId FROM User u")
    List<Long> findAllUserIds();
}

