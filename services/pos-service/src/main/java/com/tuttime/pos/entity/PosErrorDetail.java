package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "pos_error_details", schema = "pos_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PosErrorDetail extends BaseTimeStampEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "pos_log_id", nullable = false)
	private PosLog log;

	@Column(name = "error_code")
	private String errorCode;

	@Column(name = "error_stacktrace", columnDefinition = "TEXT")
	private String errorStacktrace;
}
