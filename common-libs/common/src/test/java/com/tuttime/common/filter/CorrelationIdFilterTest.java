package com.tuttime.common.filter;

import com.tuttime.common.filter.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CorrelationIdFilter Tests")
class CorrelationIdFilterTest {

	@Mock
	private HttpServletRequest request;

	@Mock
	private HttpServletResponse response;

	@Mock
	private FilterChain filterChain;

	@InjectMocks
	private CorrelationIdFilter correlationIdFilter;

	@BeforeEach
	void setUp() {
		MDC.clear();
	}

	@Test
	@DisplayName("Should extract correlation ID from request header")
	void testDoFilterInternal_WithCorrelationIdHeader_ExtractsFromHeader() throws ServletException, IOException {
		// Given
		String correlationId = "existing-correlation-id";
		when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);

		// When
		correlationIdFilter.doFilterInternal(request, response, filterChain);

		// Then
		verify(response).setHeader("X-Correlation-Id", correlationId);
		verify(filterChain).doFilter(request, response);
		assertThat(MDC.get("correlationId")).isNull(); // MDC is cleared after filter
	}

	@Test
	@DisplayName("Should generate correlation ID when header is not present")
	void testDoFilterInternal_WithoutCorrelationIdHeader_GeneratesNewId() throws ServletException, IOException {
		// Given
		when(request.getHeader("X-Correlation-Id")).thenReturn(null);

		ArgumentCaptor<String> correlationIdCaptor = ArgumentCaptor.forClass(String.class);

		// When
		correlationIdFilter.doFilterInternal(request, response, filterChain);

		// Then
		verify(response).setHeader(eq("X-Correlation-Id"), correlationIdCaptor.capture());
		String generatedId = correlationIdCaptor.getValue();
		assertThat(generatedId).isNotNull();
		assertThat(generatedId).isNotEmpty();
		// Verify it's a valid UUID format
		UUID.fromString(generatedId); // Should not throw exception
		verify(filterChain).doFilter(request, response);
	}

	@Test
	@DisplayName("Should generate correlation ID when header is empty")
	void testDoFilterInternal_WithEmptyCorrelationIdHeader_GeneratesNewId() throws ServletException, IOException {
		// Given
		when(request.getHeader("X-Correlation-Id")).thenReturn("");

		ArgumentCaptor<String> correlationIdCaptor = ArgumentCaptor.forClass(String.class);

		// When
		correlationIdFilter.doFilterInternal(request, response, filterChain);

		// Then
		verify(response).setHeader(eq("X-Correlation-Id"), correlationIdCaptor.capture());
		String generatedId = correlationIdCaptor.getValue();
		assertThat(generatedId).isNotNull();
		assertThat(generatedId).isNotEmpty();
		verify(filterChain).doFilter(request, response);
	}

	@Test
	@DisplayName("Should add correlation ID to MDC during request processing")
	void testDoFilterInternal_AddsCorrelationIdToMDC() throws ServletException, IOException {
		// Given
		String correlationId = "test-correlation-id";
		when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);

		// Use a filter chain that checks MDC
		doAnswer(invocation -> {
			assertThat(MDC.get("correlationId")).isEqualTo(correlationId);
			return null;
		}).when(filterChain).doFilter(any(), any());

		// When
		correlationIdFilter.doFilterInternal(request, response, filterChain);

		// Then
		verify(filterChain).doFilter(request, response);
		// MDC should be cleared after filter
		assertThat(MDC.get("correlationId")).isNull();
	}

	@Test
	@DisplayName("Should clear MDC after request processing")
	void testDoFilterInternal_ClearsMDCAfterProcessing() throws ServletException, IOException {
		// Given
		String correlationId = "test-correlation-id";
		when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);

		// When
		correlationIdFilter.doFilterInternal(request, response, filterChain);

		// Then
		// MDC should be cleared after filter completes
		assertThat(MDC.get("correlationId")).isNull();
		verify(filterChain).doFilter(request, response);
	}

	@Test
	@DisplayName("Should clear MDC even when filter chain throws exception")
	void testDoFilterInternal_ClearsMDCOnException() throws ServletException, IOException {
		// Given
		String correlationId = "test-correlation-id";
		when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);
		doThrow(new RuntimeException("Filter chain error")).when(filterChain).doFilter(any(), any());

		// When/Then
		try {
			correlationIdFilter.doFilterInternal(request, response, filterChain);
		} catch (RuntimeException e) {
			// Expected
		}

		// MDC should still be cleared in finally block
		assertThat(MDC.get("correlationId")).isNull();
	}

	@Test
	@DisplayName("Should always set correlation ID in response header")
	void testDoFilterInternal_AlwaysSetsResponseHeader() throws ServletException, IOException {
		// Given
		String correlationId = "response-correlation-id";
		when(request.getHeader("X-Correlation-Id")).thenReturn(correlationId);

		// When
		correlationIdFilter.doFilterInternal(request, response, filterChain);

		// Then
		verify(response, times(1)).setHeader("X-Correlation-Id", correlationId);
	}
}

