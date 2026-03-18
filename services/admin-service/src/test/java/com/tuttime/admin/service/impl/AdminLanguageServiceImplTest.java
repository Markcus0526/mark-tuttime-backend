package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.LanguageResponse;
import com.tuttime.admin.rest.client.UserServiceRestClient;
import com.tuttime.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminLanguageServiceImpl Tests")
class AdminLanguageServiceImplTest {

    @Mock
    private UserServiceRestClient userServiceRestClient;

    @InjectMocks
    private AdminLanguageServiceImpl languageService;

    @BeforeEach
    void setUp() {
        // Setup is minimal since we're mocking the REST client
    }

    @Test
    @DisplayName("Should list all languages successfully")
    void testListLanguages_Success_ReturnsAllLanguages() {
        // Given
        Set<String> languageCodes = Set.of("en", "ko", "es");
        when(userServiceRestClient.getAllLanguages()).thenReturn(languageCodes);

        // When
        List<LanguageResponse> result = languageService.listLanguages();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).extracting(LanguageResponse::getLanguageCode)
                .containsExactlyInAnyOrder("en", "ko", "es");
        verify(userServiceRestClient).getAllLanguages();
    }

    @Test
    @DisplayName("Should get language by code successfully")
    void testGetLanguageByCode_Success_ReturnsLanguage() {
        // Given
        Map<String, Object> translations = Map.of("common.welcome", "Welcome");
        when(userServiceRestClient.getLanguageByCode("en")).thenReturn(translations);

        // When
        LanguageResponse result = languageService.getLanguageByCode("en");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLanguageCode()).isEqualTo("en");
        assertThat(result.getLanguageName()).isEqualTo("EN");
        verify(userServiceRestClient).getLanguageByCode("en");
    }

    @Test
    @DisplayName("Should throw exception when language not found")
    void testGetLanguageByCode_NotFound_ThrowsException() {
        // Given
        when(userServiceRestClient.getLanguageByCode("xx"))
                .thenThrow(new RuntimeException("Language not found: xx"));

        // When/Then
        assertThatThrownBy(() -> languageService.getLanguageByCode("xx"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Language not found");
    }


    @Test
    @DisplayName("Should delete language successfully")
    void testDeleteLanguage_Success_DeletesLanguage() {
        // Given
        doNothing().when(userServiceRestClient).deleteLanguage("ko");

        // When
        languageService.deleteLanguage("ko");

        // Then
        verify(userServiceRestClient).deleteLanguage("ko");
    }

    @Test
    @DisplayName("Should throw exception when deleting language fails")
    void testDeleteLanguage_Failure_ThrowsException() {
        // Given
        doThrow(new RuntimeException("Failed to delete language: ko"))
                .when(userServiceRestClient).deleteLanguage("ko");

        // When/Then
        assertThatThrownBy(() -> languageService.deleteLanguage("ko"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Failed to delete language");
    }

    @Test
    @DisplayName("Should upload language translations successfully")
    void testUploadLanguage_Success_UploadsTranslations() {
        // Given
        Map<String, Object> translations = new HashMap<>();
        translations.put("common", Map.of(
                "welcome", "Welcome",
                "goodbye", "Goodbye"
        ));
        translations.put("error", Map.of("not_found", "Not found"));

        doNothing().when(userServiceRestClient).uploadTranslations("en", translations);

        // When
        languageService.uploadLanguage("en", translations, 1L);

        // Then
        verify(userServiceRestClient).uploadTranslations("en", translations);
    }

    @Test
    @DisplayName("Should throw exception when uploading language fails")
    void testUploadLanguage_Failure_ThrowsException() {
        // Given
        Map<String, Object> translations = new HashMap<>();
        translations.put("common", Map.of("welcome", "Welcome"));

        doThrow(new RuntimeException("Failed to upload translations"))
                .when(userServiceRestClient).uploadTranslations("en", translations);

        // When/Then
        assertThatThrownBy(() -> languageService.uploadLanguage("en", translations, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Failed to upload language");
    }

    @Test
    @DisplayName("Should get language translations successfully")
    void testGetLanguageTranslations_Success_ReturnsTranslations() {
        // Given
        Map<String, Object> translations = Map.of(
                "common", Map.of(
                        "welcome", "Welcome",
                        "goodbye", "Goodbye"
                ),
                "error", Map.of("not_found", "Not found")
        );

        when(userServiceRestClient.getLanguageByCode("en")).thenReturn(translations);

        // When
        Map<String, Object> result = languageService.getLanguageTranslations("en");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).containsKey("common");
        assertThat(result).containsKey("error");
        verify(userServiceRestClient).getLanguageByCode("en");
    }

    @Test
    @DisplayName("Should throw exception when getting translations for non-existent language")
    void testGetLanguageTranslations_LanguageNotFound_ThrowsException() {
        // Given
        when(userServiceRestClient.getLanguageByCode("xx"))
                .thenThrow(new RuntimeException("Language not found: xx"));

        // When/Then
        assertThatThrownBy(() -> languageService.getLanguageTranslations("xx"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Failed to get language translations");
    }

    @Test
    @DisplayName("Should handle empty translations map")
    void testGetLanguageTranslations_EmptyMap_ReturnsEmptyMap() {
        // Given
        Map<String, Object> emptyTranslations = Collections.emptyMap();
        when(userServiceRestClient.getLanguageByCode("en")).thenReturn(emptyTranslations);

        // When
        Map<String, Object> result = languageService.getLanguageTranslations("en");

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
}

