package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.Schedule;
import com.tuttime.restaurant.repository.record.ScheduleRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

	Optional<Schedule> findByIdAndDeletedFalse(UUID scheduleId);

	List<Schedule> findAllByEntityIdAndDeletedFalse(UUID restaurantId);

	@Modifying
	@Query("UPDATE Schedule r SET r.deleted = true, r.deletedAt = CURRENT_TIMESTAMP " +
			"WHERE r.entityId = :entityId AND r.deleted = false")
	void deleteAllByEntityId(@Param("entityId") UUID entityId);

	@Query("SELECT s FROM Schedule s WHERE s.entityId = :entityId " +
			"AND s.deleted = false")
	List<Schedule> findAllByEntityId(@Param("entityId") UUID entityId);

	@Query("SELECT new com.tuttime.restaurant.repository.record.ScheduleRecord(" +
			"s.dayOfWeek, s.openTime, s.closeTime, s.closed) " +
			"FROM Schedule s " +
			"WHERE s.entityId = :branchId " +
			"AND s.dayOfWeek = :dayOfWeek " +
			"AND s.deleted = false")
	Optional<ScheduleRecord> findActiveSchedule(@Param("branchId") UUID branchId,
	                                            @Param("dayOfWeek") int dayOfWeek);

	@Query("SELECT s FROM Schedule s WHERE s.entityId IN :branchIds AND s.deleted = false")
	List<Schedule> findAllByBranchIds(@Param("branchIds") List<UUID> branchIds);
}