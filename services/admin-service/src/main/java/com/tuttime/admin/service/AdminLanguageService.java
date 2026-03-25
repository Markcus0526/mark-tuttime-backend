package com.tuttime.admin.service;

import com.tuttime.admin.dto.LanguageResponse;

import java.util.List;
import java.util.Map;

/**
 * Admin Language Service Interface
 */
public interface AdminLanguageService {

	/**
	 * List all languages
	 */
	List<LanguageResponse> listLanguages();

	/**
	 * Get language by code
	 */
	LanguageResponse getLanguageByCode(String languageCode);

	/**
	 * Get language translations directly (returns nested Map structure)
	 */
	Map<String, Object> getLanguageTranslations(String languageCode);

	/**
	 * Upload/update language with JSON translations
	 */
	void uploadLanguage(String languageCode, Map<String, Object> translations, Long createdBy);

	/**
	 * Delete a language
	 */
	void deleteLanguage(String languageCode);
}

