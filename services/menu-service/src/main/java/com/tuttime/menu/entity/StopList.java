package com.tuttime.menu.entity;

import com.tuttime.common.entity.BaseUuidEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.UUIDJdbcType;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "stop_lists")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StopList extends BaseUuidEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "menu_item_id", nullable = false)
	private MenuItem menuItem;

	@Column(name = "branch_id", nullable = false)
	@JdbcType(UUIDJdbcType.class)
	private UUID branchId;

	@Column(name = "start_date", nullable = false)
	private LocalDate startDate;

	@Column(name = "end_date")
	private LocalDate endDate;

	@Column(name = "reason")
	private String reason;
}
