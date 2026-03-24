package com.tuttime.menu.repository;

import com.tuttime.menu.entity.StopList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StopListRepository extends JpaRepository<StopList, Long> {
    List<StopList> findByMenuItemId(Long menuItemId);
}
