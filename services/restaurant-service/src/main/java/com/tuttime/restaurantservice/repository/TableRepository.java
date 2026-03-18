package com.tuttime.restaurantservice.repository;

import com.tuttime.restaurantservice.entity.Table;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TableRepository extends JpaRepository<Table, Long> {
  List<Table> findByZoneBranchId(Long branchId);

  @Query("SELECT t FROM Table t WHERE t.zone.branch.id = :branchId AND t.status = 'AVAILABLE'")
  List<Table> findAvailableByBranchId(Long branchId);

}
