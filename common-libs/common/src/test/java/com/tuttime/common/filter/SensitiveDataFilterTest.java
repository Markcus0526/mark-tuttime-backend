package com.tuttime.common.filter;

import com.tuttime.common.filter.SensitiveDataFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SensitiveDataFilter Tests")
class SensitiveDataFilterTest {

	@Test
	@DisplayName("Should mask sensitive header values")
	void testMaskHeaderValue_MasksSensitiveHeaders() {
		// Given
		String authorizationHeader = "Bearer token123";
		String apiKeyHeader = "api-key-value";

		// When
		String maskedAuth = SensitiveDataFilter.maskHeaderValue("Authorization", authorizationHeader);
		String maskedApiKey = SensitiveDataFilter.maskHeaderValue("X-Api-Key", apiKeyHeader);
		String normalHeader = SensitiveDataFilter.maskHeaderValue("Content-Type", "application/json");

		// Then
		assertThat(maskedAuth).isEqualTo("***MASKED***");
		assertThat(maskedApiKey).isEqualTo("***MASKED***");
		assertThat(normalHeader).isEqualTo("application/json");
	}

	@Test
	@DisplayName("Should mask sensitive parameter values")
	void testMaskParameterValue_MasksSensitiveParameters() {
		// Given
		String password = "secretPassword123";
		String token = "jwt-token-123";
		String creditCard = "4532-1234-5678-9010";
		String normalParam = "username";

		// When
		String maskedPassword = SensitiveDataFilter.maskParameterValue("password", password);
		String maskedToken = SensitiveDataFilter.maskParameterValue("token", token);
		String maskedCreditCard = SensitiveDataFilter.maskParameterValue("creditCard", creditCard);
		String normalParamValue = SensitiveDataFilter.maskParameterValue("username", normalParam);

		// Then
		assertThat(maskedPassword).isEqualTo("***MASKED***");
		assertThat(maskedToken).isEqualTo("***MASKED***");
		assertThat(maskedCreditCard).isEqualTo("***MASKED***");
		assertThat(normalParamValue).isEqualTo("username");
	}

	@Test
	@DisplayName("Should handle case-insensitive parameter names")
	void testMaskParameterValue_IsCaseInsensitive() {
		// Given
		String password = "secretPassword123";

		// When
		String maskedUpper = SensitiveDataFilter.maskParameterValue("PASSWORD", password);
		String maskedMixed = SensitiveDataFilter.maskParameterValue("Password", password);
		String maskedLower = SensitiveDataFilter.maskParameterValue("password", password);

		// Then
		assertThat(maskedUpper).isEqualTo("***MASKED***");
		assertThat(maskedMixed).isEqualTo("***MASKED***");
		assertThat(maskedLower).isEqualTo("***MASKED***");
	}

	@Test
	@DisplayName("Should mask credit card numbers in content")
	void testMaskContentBody_MasksCreditCardNumbers() {
		// Given
		String content = "Payment with card 4532-1234-5678-9010 was processed";
		String contentWithMultipleCards = "Card 1: 4532123456789010 and Card 2: 5555-1234-5678-9010";

		// When
		String masked = SensitiveDataFilter.maskContentBody(content);
		String maskedMultiple = SensitiveDataFilter.maskContentBody(contentWithMultipleCards);

		// Then
		assertThat(masked).contains("***MASKED***");
		assertThat(masked).doesNotContain("4532-1234-5678-9010");
		assertThat(maskedMultiple).contains("***MASKED***");
	}

	@Test
	@DisplayName("Should mask SSN in content")
	void testMaskContentBody_MasksSSN() {
		// Given
		String content = "User SSN: 123-45-6789";
		String contentWithSSNNoDashes = "User SSN: 123456789";

		// When
		String masked = SensitiveDataFilter.maskContentBody(content);

		// Then
		assertThat(masked).contains("***MASKED***");
		assertThat(masked).doesNotContain("123-45-6789");
	}

	@Test
	@DisplayName("Should mask sensitive JSON fields in content")
	void testMaskContentBody_MasksJsonFields() {
		// Given
		String jsonContent = "{\"password\":\"secret123\",\"token\":\"jwt-token\",\"username\":\"john\"}";

		// When
		String masked = SensitiveDataFilter.maskContentBody(jsonContent);

		// Then
		assertThat(masked).contains("***MASKED***");
		assertThat(masked).doesNotContain("secret123");
		assertThat(masked).doesNotContain("jwt-token");
		assertThat(masked).contains("john"); // Non-sensitive field should remain
	}

	@Test
	@DisplayName("Should handle null values gracefully")
	void testMaskMethods_HandleNullValues() {
		// When/Then
		assertThat(SensitiveDataFilter.maskHeaderValue("Authorization", null)).isNull();
		assertThat(SensitiveDataFilter.maskParameterValue("password", null)).isNull();
		assertThat(SensitiveDataFilter.maskContentBody(null)).isNull();
		assertThat(SensitiveDataFilter.maskPhoneNumber(null)).isEqualTo("***MASKED***");
		assertThat(SensitiveDataFilter.maskEmail(null)).isEqualTo("***MASKED***");
	}

	@Test
	@DisplayName("Should handle empty strings gracefully")
	void testMaskMethods_HandleEmptyStrings() {
		// When/Then
		assertThat(SensitiveDataFilter.maskHeaderValue("Authorization", "")).isEqualTo("");
		assertThat(SensitiveDataFilter.maskParameterValue("password", "")).isEqualTo("");
		assertThat(SensitiveDataFilter.maskContentBody("")).isEqualTo("");
	}

	@Test
	@DisplayName("Should partially mask phone numbers")
	void testMaskPhoneNumber_PartiallyMasks() {
		// Given
		String phone1 = "1234567890";
		String phone2 = "+1-234-567-8901";
		String shortPhone = "123";

		// When
		String masked1 = SensitiveDataFilter.maskPhoneNumber(phone1);
		String masked2 = SensitiveDataFilter.maskPhoneNumber(phone2);
		String maskedShort = SensitiveDataFilter.maskPhoneNumber(shortPhone);

		// Then
		assertThat(masked1).endsWith("7890");
		assertThat(masked1).startsWith("***-***-");
		assertThat(masked2).endsWith("8901");
		assertThat(maskedShort).isEqualTo("***MASKED***");
	}

	@Test
	@DisplayName("Should partially mask email addresses")
	void testMaskEmail_PartiallyMasks() {
		// Given
		String email1 = "john.doe@example.com";
		String email2 = "ab@example.com";

		// When
		String masked1 = SensitiveDataFilter.maskEmail(email1);
		String masked2 = SensitiveDataFilter.maskEmail(email2);
		String invalidEmail = SensitiveDataFilter.maskEmail("notanemail");

		// Then
		assertThat(masked1).startsWith("jo");
		assertThat(masked1).contains("***@");
		assertThat(masked1).endsWith("@example.com");
		assertThat(masked2).isEqualTo("***MASKED***"); // Too short
		assertThat(invalidEmail).isEqualTo("***MASKED***"); // No @ symbol
	}

	@Test
	@DisplayName("Should return original content when no sensitive data found")
	void testMaskContentBody_NoSensitiveData_ReturnsOriginal() {
		// Given
		String safeContent = "This is a safe message with no sensitive data.";

		// When
		String result = SensitiveDataFilter.maskContentBody(safeContent);

		// Then
		assertThat(result).isEqualTo(safeContent);
	}
}

