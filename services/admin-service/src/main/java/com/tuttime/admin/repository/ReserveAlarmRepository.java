package com.tuttime.admin.repository;

import com.tuttime.admin.entity.ReserveAlarm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Reserve Alarm entities
 */
@Repository
public interface ReserveAlarmRepository extends JpaRepository<ReserveAlarm, Long> {

	/**
	 * Find all active and enabled alarms
	 */
	List<ReserveAlarm> findByStatusAndIsEnabled(ReserveAlarm.AlarmStatus status, Boolean isEnabled);

	/**
	 * Find alarms by network
	 */
	List<ReserveAlarm> findByNetwork(String network);

	/**
	 * Find alarms by network and status
	 */
	List<ReserveAlarm> findByNetworkAndStatus(String network, ReserveAlarm.AlarmStatus status);

	/**
	 * Find all alarms for a specific network or global alarms (network is null)
	 */
	List<ReserveAlarm> findByNetworkOrNetworkIsNull(String network);

	/**
	 * Find active alarms for a specific network or global alarms (network is null)
	 */
	@org.springframework.data.jpa.repository.Query(
			"SELECT a FROM ReserveAlarm a WHERE " +
					"a.status = :status AND a.isEnabled = :isEnabled AND " +
					"(a.network = :network OR a.network IS NULL)"
	)
	List<ReserveAlarm> findActiveAlarmsForNetwork(
			ReserveAlarm.AlarmStatus status,
			Boolean isEnabled,
			String network);
}

