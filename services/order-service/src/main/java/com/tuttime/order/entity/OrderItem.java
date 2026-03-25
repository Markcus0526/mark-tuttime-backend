package com.tuttime.order.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "order_items")
@SQLDelete(sql = "UPDATE order_items SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class OrderItem {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
//	@Type(type = "uuid-char")
	@Column(columnDefinition = "uuid", updatable = false, nullable = false)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "order_id", nullable = false)
	private Order order;

	@Column(name = "menu_item_id", nullable = false, columnDefinition = "uuid")
	private UUID menuItemId;

	@Column(nullable = false)
	private int quantity;

	@Column(nullable = false, precision = 10, scale = 2)
	private BigDecimal price;

	@Builder.Default
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt = Instant.now();

	@Builder.Default
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt = Instant.now();

	@Column(name = "created_by", columnDefinition = "uuid")
	private UUID createdBy;

	@Column(name = "updated_by", columnDefinition = "uuid")
	private UUID updatedBy;

	@Column(name = "deleted_at")
	private Instant deletedAt;

	@OneToMany(mappedBy = "orderItem", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<OrderItemModifier> modifiers;

	// Getters and setters omitted for brevity

	@PreUpdate
	public void preUpdate() {
		this.updatedAt = Instant.now();
	}
}
