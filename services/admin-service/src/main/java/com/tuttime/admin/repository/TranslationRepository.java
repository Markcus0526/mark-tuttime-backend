package com.tuttime.admin.repository;

import com.tuttime.admin.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Translation Repository
 */
@Repository
public interface TranslationRepository extends JpaRepository<Translation, Long> {

	Optional<Translation> findByLanguageCodeAndTranslationKey(String languageCode, String translationKey);

	List<Translation> findByLanguageCode(String languageCode);

	List<Translation> findByLanguageCodeAndNamespace(String languageCode, String namespace);

	@Modifying
	@Query("DELETE FROM Translation t WHERE t.languageCode = :languageCode")
	void deleteByLanguageCode(@Param("languageCode") String languageCode);

	long countByLanguageCode(String languageCode);
}

