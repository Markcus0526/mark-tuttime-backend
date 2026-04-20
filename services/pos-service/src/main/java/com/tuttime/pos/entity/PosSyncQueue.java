package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseSerialEntity;
import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pos_sync_queues", schema = "pos_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PosSyncQueue extends BaseSerialEntity {

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pos_log_id", nullable = false)
	private PosLog log;

	@Column(name = "next_retry_at", nullable = false)
	private OffsetDateTime nextRetryAt;

	@Column(name = "retry_count")
	@Builder.Default
	private Integer retryCount = 0;

	@Column(nullable = false)
	private String status; // Uses values from sync_task_status enum
}
