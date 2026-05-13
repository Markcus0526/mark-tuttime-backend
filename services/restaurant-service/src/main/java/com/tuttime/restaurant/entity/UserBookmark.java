package com.tuttime.restaurant.entity;

import com.tuttime.common.entity.BaseTimeStampEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Entity
@Table(name = "user_bookmarks", schema = "restaurant_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class UserBookmark extends BaseTimeStampEntity {

	@EmbeddedId
	private UserBookmarkId id;

	@ManyToOne(fetch = FetchType.LAZY)
	@MapsId("branchId")
	@JoinColumn(name = "branch_id")
	private Branch branch;

	// Custom constructor for easier use in Service
	public UserBookmark(UUID userId, Branch branch) {
		this.id = new UserBookmarkId(userId, branch.getId());
		this.branch = branch;
	}
}
