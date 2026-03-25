package com.tuttime.admin.repository;

import com.tuttime.admin.dto.WarningCategory;
import com.tuttime.admin.dto.WarningSeverity;
import com.tuttime.admin.entity.FraudFlag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Fraud Flags
 */
@Repository
public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long>, JpaSpecificationExecutor<FraudFlag> {

	/**
	 * Count total fraud flags
	 */
	@Query("SELECT COUNT(f) FROM FraudFlag f")
	long countTotalFlags();

	/**
	 * Count unresolved fraud flags
	 */
	@Query("SELECT COUNT(f) FROM FraudFlag f WHERE f.resolved = false")
	long countUnresolvedFlags();

	/**
	 * Count fraud flags by category
	 */
	@Query("SELECT COUNT(f) FROM FraudFlag f WHERE f.category = :category")
	long countByCategory(@Param("category") WarningCategory category);

	/**
	 * Count fraud flags by severity
	 */
	@Query("SELECT COUNT(f) FROM FraudFlag f WHERE f.severity = :severity")
	long countBySeverity(@Param("severity") WarningSeverity severity);
}

