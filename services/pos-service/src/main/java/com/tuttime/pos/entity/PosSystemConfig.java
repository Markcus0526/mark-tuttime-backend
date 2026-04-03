package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pos_system_configs", schema = "pos_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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