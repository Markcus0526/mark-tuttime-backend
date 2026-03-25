package com.tuttime.admin.repository;

import com.tuttime.admin.entity.AppVersion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * App Version Repository
 */
@Repository
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {

	/**
	 * Get the active app version configuration
	 *
	 * @return Optional containing the active version if it exists
	 */
	Optional<AppVersion> findByIsActiveTrue();

	/**
	 * Get the active app version with pessimistic lock to prevent concurrent updates
	 *
	 * <p>Uses PESSIMISTIC_WRITE lock to ensure only one transaction can update the active version at a time.
	 * This prevents race conditions when multiple admins try to update simultaneously.
	 *
	 * @return Optional containing the active version if it exists
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT av FROM AppVersion av WHERE av.isActive = true")
	Optional<AppVersion> findByIsActiveTrueWithLock();

	/**
	 * Get the latest app version (by updated_at)
	 *
	 * @return Optional containing the latest version if it exists
	 */
	Optional<AppVersion> findFirstByOrderByUpdatedAtDesc();
}

