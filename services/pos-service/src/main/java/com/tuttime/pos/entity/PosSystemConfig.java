package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "pos_system_configs", schema = "pos_service")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PosSystemConfig extends BaseTimeStampEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;

	@Column(name = "pos_system", unique = true, nullable = false)
	private String posSystem;

	@Column(name = "api_endpoint", nullable = false)
	private String apiEndpoint;

	@Column(name = "webhook_secret")
	private String webhookSecret;

	@Column(nullable = false)
	@Builder.Default
	private Boolean enabled = true;
}