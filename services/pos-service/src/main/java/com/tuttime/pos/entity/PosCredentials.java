package com.tuttime.pos.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "integration_credentials")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@EntityListeners(AuditingEntityListener.class)
public class PosCredentials extends BaseTimeStampEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long restaurantId;

	@Enumerated(EnumType.STRING)
	private PosSystem posSystem;

	private String apiUrl;

	private String apiKey;

	private String apiToken;

	private LocalDateTime lastSync;

	public enum PosSystem {
		IIKO, RKEEPER
	}
}
