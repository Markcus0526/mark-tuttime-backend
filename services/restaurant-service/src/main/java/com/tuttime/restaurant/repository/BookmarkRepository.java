package com.tuttime.restaurant.repository;

import com.tuttime.restaurant.entity.UserBookmark;
import com.tuttime.restaurant.entity.UserBookmarkId;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookmarkRepository extends JpaRepository<UserBookmark, UserBookmarkId> {

    // Efficiently returns only the branch IDs the user has bookmarked
    @Query("SELECT b.id.branchId FROM UserBookmark b WHERE b.id.userId = :userId")
    List<UUID> findBookmarkedBranchIdsByUserId(@Param("userId") UUID userId);
}