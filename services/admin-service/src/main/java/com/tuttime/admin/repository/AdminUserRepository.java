package com.tuttime.admin.repository;

import com.tuttime.admin.entity.AdminUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Admin User Repository
 */
@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, Long> {

    /**
     * Find admin user by username
     */
    Optional<AdminUser> findByUsername(String username);

    /**
     * Find admin user by email
     */
    Optional<AdminUser> findByEmail(String email);

    /**
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);

    /**
     * Find active admin user by username
     */
    @Query("SELECT a FROM AdminUser a WHERE a.username = :username AND a.isActive = true")
    Optional<AdminUser> findActiveByUsername(@Param("username") String username);

    /**
     * Find admin users by role
     */
    Page<AdminUser> findByRole(AdminUser.AdminRole role, Pageable pageable);

    /**
     * Find admin users by active status
     */
    Page<AdminUser> findByIsActive(Boolean isActive, Pageable pageable);

    /**
     * Find admin users by role and active status
     */
    Page<AdminUser> findByRoleAndIsActive(AdminUser.AdminRole role, Boolean isActive, Pageable pageable);

    /**
     * Search admin users by username or email (case-insensitive)
     */
    @Query("SELECT a FROM AdminUser a WHERE " +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<AdminUser> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            @Param("search") String search, Pageable pageable);

    /**
     * Search admin users with role filter
     */
    @Query("SELECT a FROM AdminUser a WHERE " +
           "a.role = :role AND (" +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AdminUser> findByRoleAndUsernameOrEmailContaining(
            @Param("role") AdminUser.AdminRole role,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Search admin users with active status filter
     */
    @Query("SELECT a FROM AdminUser a WHERE " +
           "a.isActive = :isActive AND (" +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AdminUser> findByIsActiveAndUsernameOrEmailContaining(
            @Param("isActive") Boolean isActive,
            @Param("search") String search,
            Pageable pageable);

    /**
     * Search admin users with role and active status filters
     */
    @Query("SELECT a FROM AdminUser a WHERE " +
           "a.role = :role AND a.isActive = :isActive AND (" +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.email) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<AdminUser> findByRoleAndIsActiveAndUsernameOrEmailContaining(
            @Param("role") AdminUser.AdminRole role,
            @Param("isActive") Boolean isActive,
            @Param("search") String search,
            Pageable pageable);
}

