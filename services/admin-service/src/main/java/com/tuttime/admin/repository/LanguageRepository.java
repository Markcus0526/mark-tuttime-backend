package com.tuttime.admin.repository;

import com.tuttime.admin.entity.Language;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Language Repository
 */
@Repository
public interface LanguageRepository extends JpaRepository<Language, Long> {

    Optional<Language> findByLanguageCode(String languageCode);

    List<Language> findByIsActiveTrueOrderByDisplayOrderAsc();

    Optional<Language> findByIsDefaultTrue();

    boolean existsByLanguageCode(String languageCode);
}

