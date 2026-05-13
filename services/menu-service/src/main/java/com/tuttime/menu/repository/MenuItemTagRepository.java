package com.tuttime.menu.repository;

import com.tuttime.menu.entity.MenuItemTag;
import com.tuttime.menu.entity.MenuItemTagId;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MenuItemTagRepository extends JpaRepository<MenuItemTag, MenuItemTagId> {

	@Query("SELECT mt FROM MenuItemTag mt JOIN FETCH mt.menuTag WHERE mt.id.menuItemId = :menuItemId")
	List<MenuItemTag> findAllByMenuItemId(@Param("menuItemId") UUID menuItemId);

	@Modifying
	@Query("DELETE FROM MenuItemTag bt WHERE bt.id.menuItemId = :menuItemId")
	void deleteByMenuItemId(UUID menuItemId);

	boolean existsByMenuTagId(UUID tagId);

	// Fetches the bridge entity and the actual MenuTag data for a list of item IDs
	@Query("""
			    SELECT mit FROM MenuItemTag mit 
			    JOIN FETCH mit.menuTag 
			    WHERE mit.id.menuItemId IN :itemIds
			""")
	List<MenuItemTag> findAllByMenuItemIdIn(@Param("itemIds") List<UUID> itemIds);
}
