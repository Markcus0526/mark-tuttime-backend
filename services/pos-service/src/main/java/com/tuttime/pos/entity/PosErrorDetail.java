package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pos_error_details", schema = "pos_service")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PosErrorDetail extends BaseEntity {
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
