package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.LanguageResponse;
import com.tuttime.admin.rest.client.UserServiceRestClient;
import com.tuttime.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Admin Language Service Implementation
 *
 * <p>Uses REST client to interact with user-service for language management.
 * Languages are stored as JSON files in user-service, not in database.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLanguageServiceImpl implements com.tuttime.admin.service.AdminLanguageService {

	private final UserServiceRestClient userServiceRestClient;

	@Override
	public List<LanguageResponse> listLanguages() {
		log.info("Listing all languages from user-service");
		try {
			Set<String> languageCodes = userServiceRestClient.getAllLanguages();
			log.info("Received {} language codes from user-service: {}", languageCodes.size(), languageCodes);

			if (languageCodes.isEmpty()) {
				log.warn("No languages found in user-service. This might indicate:");
				log.warn("1. User-service is not running or not accessible");
				log.warn("2. No language JSON files exist in user-service i18n directory");
				log.warn("3. User-service endpoint is returning empty set");
			}

			// Convert Set<String> to List<LanguageResponse>
			// Since we're using JSON files, we create simple responses with just the code
			List<LanguageResponse> languages = languageCodes.stream()
					.map(code -> LanguageResponse.builder()
							.languageCode(code)
							.languageName(code.toUpperCase()) // Default name
							.nativeName(code.toUpperCase()) // Default native name
							.isActive(true)
							.isDefault(false) // No default concept in file-based system
							.displayOrder(0)
							.build())
					.collect(Collectors.toList());

			log.info("Converted {} language codes to {} LanguageResponse objects", languageCodes.size(), languages.size());
			return languages;
		} catch (Exception e) {
			log.error("Error listing languages from user-service", e);
			throw new BusinessException("Failed to list languages: " + e.getMessage());
		}
	}

	@Override
	public LanguageResponse getLanguageByCode(String languageCode) {
		log.debug("Getting language by code: {} from user-service", languageCode);
		try {
			// Get translations to verify language exists
			userServiceRestClient.getLanguageByCode(languageCode);

			return LanguageResponse.builder()
					.languageCode(languageCode)
					.languageName(languageCode.toUpperCase())
					.nativeName(languageCode.toUpperCase())
					.isActive(true)
					.isDefault(false)
					.displayOrder(0)
					.build();
		} catch (Exception e) {
			log.error("Error getting language from user-service: {}", languageCode, e);
			throw new BusinessException("Language not found: " + languageCode);
		}
	}

	@Override
	public Map<String, Object> getLanguageTranslations(String languageCode) {
		log.debug("Getting language translations directly for: {} from user-service", languageCode);
		try {
			// Get translations directly from user-service (preserves nested structure)
			Map<String, Object> translations = userServiceRestClient.getLanguageByCode(languageCode);
			log.debug("Retrieved translations for language: {}, keys: {}", languageCode, translations.keySet());
			return translations;
		} catch (Exception e) {
			log.error("Error getting language translations from user-service: {}", languageCode, e);
			throw new BusinessException("Failed to get language translations: " + e.getMessage());
		}
	}

	@Override
	public void uploadLanguage(String languageCode, Map<String, Object> translations, Long createdBy) {
		log.info("Uploading/updating language: {} with {} translation keys to user-service", languageCode, translations.size());
		try {
			userServiceRestClient.uploadTranslations(languageCode, translations);
			log.info("Language {} uploaded successfully", languageCode);
		} catch (Exception e) {
			log.error("Error uploading language to user-service: {}", languageCode, e);
			throw new BusinessException("Failed to upload language: " + e.getMessage());
		}
	}

	@Override
	public void deleteLanguage(String languageCode) {
		log.info("Deleting language: {} from user-service", languageCode);
		try {
			userServiceRestClient.deleteLanguage(languageCode);
		} catch (Exception e) {
			log.error("Error deleting language from user-service: {}", languageCode, e);
			throw new BusinessException("Failed to delete language: " + e.getMessage());
		}
	}
}

