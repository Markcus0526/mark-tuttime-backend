package com.tuttime.user.repository.readmodel;

import com.tuttime.user.entity.readmodel.UserReadModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserReadModelRepository extends JpaRepository<UserReadModel, Long> {
    @Query("SELECT u FROM UserReadModel u WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<UserReadModel> findByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM UserReadModel u WHERE u.userId = :userId AND u.deletedAt IS NULL")
    Optional<UserReadModel> findByIdAndNotDeleted(@Param("userId") Long userId);

    @Query("SELECT u FROM UserReadModel u WHERE u.statusLevel = :statusLevel AND u.isActive = :isActive")
    List<UserReadModel> findByStatusLevelAndActive(@Param("statusLevel") UserReadModel.UserStatusLevel statusLevel, @Param("isActive") boolean isActive);

    @Query("SELECT COUNT(u) FROM UserReadModel u WHERE u.statusLevel = :statusLevel AND u.isActive = :isActive")
    long countByStatusLevelAndActive(@Param("statusLevel") UserReadModel.UserStatusLevel statusLevel, @Param("isActive") boolean isActive);

    @Query("SELECT u FROM UserReadModel u WHERE u.kycCompleted = true AND u.isActive = true")
    List<UserReadModel> findKycVerifiedAndActive();
}

