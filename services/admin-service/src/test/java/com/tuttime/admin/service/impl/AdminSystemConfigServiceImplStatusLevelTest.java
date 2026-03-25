package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.StatusLevelRequest;
import com.tuttime.admin.dto.StatusLevelResponse;
import com.tuttime.admin.dto.SystemConfigResponse;
import com.tuttime.admin.entity.StatusLevel;
import com.tuttime.admin.repository.StakingTierRepository;
import com.tuttime.admin.repository.StatusLevelRepository;
import com.tuttime.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSystemConfigServiceImpl - Status Level Management Tests")
class AdminSystemConfigServiceImplStatusLevelTest {

	@Mock
	private StatusLevelRepository statusLevelRepository;

	@Mock
	private StakingTierRepository stakingTierRepository;

	@InjectMocks
	private AdminSystemConfigServiceImpl systemConfigService;

	private StatusLevel testStatusLevel;
	private StatusLevelRequest testStatusLevelRequest;

	@BeforeEach
	void setUp() {
		testStatusLevel = StatusLevel.builder()
				.levelId(1L)
				.levelName("SILVER")
				.minApartBalance(new BigDecimal("5000"))
				.apBoostPerHour(new BigDecimal("2"))
				.includesProAutoMining(false)
				.displayOrder(0)
				.isActive(true)
				.description("Silver / 5k -> +2 AP/hr")
				.createdAt(OffsetDateTime.now(ZoneOffset.UTC))
				.updatedAt(OffsetDateTime.now(ZoneOffset.UTC))
				.createdBy(1L)
				.build();

		testStatusLevelRequest = StatusLevelRequest.builder()
				.levelName("GOLD")
				.minApartBalance(new BigDecimal("20000"))
				.apBoostPerHour(new BigDecimal("3"))
				.includesProAutoMining(false)
				.displayOrder(1)
				.isActive(true)
				.description("Gold / 20k -> +3 AP/hr")
				.build();
	}

	@Test
	@DisplayName("Should get all status levels successfully")
	void testGetAllStatusLevels_Success_ReturnsAllLevels() {
		// Given
		StatusLevel level2 = StatusLevel.builder()
				.levelId(2L)
				.levelName("GOLD")
				.minApartBalance(new BigDecimal("20000"))
				.apBoostPerHour(new BigDecimal("3"))
				.includesProAutoMining(false)
				.displayOrder(1)
				.isActive(true)
				.build();

		List<StatusLevel> levels = Arrays.asList(testStatusLevel, level2);
		when(statusLevelRepository.findAllByOrderByDisplayOrderAsc()).thenReturn(levels);

		// When
		List<StatusLevelResponse> result = systemConfigService.getAllStatusLevels();

		// Then
		assertThat(result).isNotNull();
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getLevelName()).isEqualTo("SILVER");
		assertThat(result.get(1).getLevelName()).isEqualTo("GOLD");
		verify(statusLevelRepository).findAllByOrderByDisplayOrderAsc();
	}

	@Test
	@DisplayName("Should get status level by name successfully")
	void testGetStatusLevelByLevelName_Success_ReturnsLevel() {
		// Given
		when(statusLevelRepository.findByLevelName("SILVER")).thenReturn(Optional.of(testStatusLevel));

		// When
		StatusLevelResponse result = systemConfigService.getStatusLevelByLevelName("SILVER");

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getLevelId()).isEqualTo(1L);
		assertThat(result.getLevelName()).isEqualTo("SILVER");
		assertThat(result.getMinApartBalance()).isEqualByComparingTo(new BigDecimal("5000"));
		assertThat(result.getApBoostPerHour()).isEqualByComparingTo(new BigDecimal("2"));
		assertThat(result.getIncludesProAutoMining()).isFalse();
		verify(statusLevelRepository).findByLevelName("SILVER");
	}

	@Test
	@DisplayName("Should throw exception when status level not found")
	void testGetStatusLevelByLevelName_NotFound_ThrowsException() {
		// Given
		when(statusLevelRepository.findByLevelName("UNKNOWN")).thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> systemConfigService.getStatusLevelByLevelName("UNKNOWN"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Status level not found");
	}

	@Test
	@DisplayName("Should create status level successfully")
	void testCreateStatusLevel_Success_CreatesLevel() {
		// Given
		when(statusLevelRepository.existsByLevelName("GOLD")).thenReturn(false);
		when(statusLevelRepository.existsByMinApartBalance(new BigDecimal("20000"))).thenReturn(false);
		when(statusLevelRepository.save(any(StatusLevel.class))).thenAnswer(invocation -> {
			StatusLevel level = invocation.getArgument(0);
			level.setLevelId(2L);
			level.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
			level.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
			return level;
		});

		// When
		StatusLevelResponse result = systemConfigService.createStatusLevel(testStatusLevelRequest, 1L);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getLevelName()).isEqualTo("GOLD");
		assertThat(result.getMinApartBalance()).isEqualByComparingTo(new BigDecimal("20000"));
		assertThat(result.getApBoostPerHour()).isEqualByComparingTo(new BigDecimal("3"));
		verify(statusLevelRepository).existsByLevelName("GOLD");
		verify(statusLevelRepository).existsByMinApartBalance(new BigDecimal("20000"));
		verify(statusLevelRepository).save(any(StatusLevel.class));
	}

	@Test
	@DisplayName("Should throw exception when level name already exists")
	void testCreateStatusLevel_DuplicateName_ThrowsException() {
		// Given
		when(statusLevelRepository.existsByLevelName("SILVER")).thenReturn(true);

		StatusLevelRequest request = StatusLevelRequest.builder()
				.levelName("SILVER")
				.minApartBalance(new BigDecimal("5000"))
				.apBoostPerHour(new BigDecimal("2"))
				.build();

		// When/Then
		assertThatThrownBy(() -> systemConfigService.createStatusLevel(request, 1L))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("already exists");
	}

	@Test
	@DisplayName("Should throw exception when min balance already exists")
	void testCreateStatusLevel_DuplicateMinBalance_ThrowsException() {
		// Given
		when(statusLevelRepository.existsByLevelName("GOLD")).thenReturn(false);
		when(statusLevelRepository.existsByMinApartBalance(new BigDecimal("5000"))).thenReturn(true);

		StatusLevelRequest request = StatusLevelRequest.builder()
				.levelName("GOLD")
				.minApartBalance(new BigDecimal("5000"))
				.apBoostPerHour(new BigDecimal("3"))
				.build();

		// When/Then
		assertThatThrownBy(() -> systemConfigService.createStatusLevel(request, 1L))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("minimum balance already exists");
	}

	@Test
	@DisplayName("Should create status level with pro auto-mining")
	void testCreateStatusLevel_WithProAutoMining_CreatesLevel() {
		// Given
		StatusLevelRequest request = StatusLevelRequest.builder()
				.levelName("PLATINUM")
				.minApartBalance(new BigDecimal("50000"))
				.apBoostPerHour(new BigDecimal("5"))
				.includesProAutoMining(true)
				.build();

		when(statusLevelRepository.existsByLevelName("PLATINUM")).thenReturn(false);
		when(statusLevelRepository.existsByMinApartBalance(new BigDecimal("50000"))).thenReturn(false);
		when(statusLevelRepository.save(any(StatusLevel.class))).thenAnswer(invocation -> {
			StatusLevel level = invocation.getArgument(0);
			level.setLevelId(3L);
			level.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
			level.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
			return level;
		});

		// When
		StatusLevelResponse result = systemConfigService.createStatusLevel(request, 1L);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getIncludesProAutoMining()).isTrue();
		verify(statusLevelRepository).save(argThat(level ->
				level.getIncludesProAutoMining() == true
		));
	}

	@Test
	@DisplayName("Should update status level successfully")
	void testUpdateStatusLevel_Success_UpdatesLevel() {
		// Given
		StatusLevelRequest updateRequest = StatusLevelRequest.builder()
				.apBoostPerHour(new BigDecimal("2.5"))
				.description("Updated description")
				.build();

		when(statusLevelRepository.findByLevelName("SILVER")).thenReturn(Optional.of(testStatusLevel));
		when(statusLevelRepository.save(any(StatusLevel.class))).thenReturn(testStatusLevel);

		// When
		StatusLevelResponse result = systemConfigService.updateStatusLevel("SILVER", updateRequest);

		// Then
		assertThat(result).isNotNull();
		verify(statusLevelRepository).findByLevelName("SILVER");
		verify(statusLevelRepository).save(testStatusLevel);
		assertThat(testStatusLevel.getApBoostPerHour()).isEqualByComparingTo(new BigDecimal("2.5"));
	}

	@Test
	@DisplayName("Should throw exception when updating to duplicate level name")
	void testUpdateStatusLevel_DuplicateName_ThrowsException() {
		// Given
		StatusLevelRequest updateRequest = StatusLevelRequest.builder()
				.levelName("GOLD")
				.build();

		when(statusLevelRepository.findByLevelName("SILVER")).thenReturn(Optional.of(testStatusLevel));
		when(statusLevelRepository.existsByLevelName("GOLD")).thenReturn(true);

		// When/Then
		assertThatThrownBy(() -> systemConfigService.updateStatusLevel("SILVER", updateRequest))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("already exists");
	}

	@Test
	@DisplayName("Should throw exception when updating to duplicate min balance")
	void testUpdateStatusLevel_DuplicateMinBalance_ThrowsException() {
		// Given
		StatusLevelRequest updateRequest = StatusLevelRequest.builder()
				.minApartBalance(new BigDecimal("20000"))
				.build();

		when(statusLevelRepository.findByLevelName("SILVER")).thenReturn(Optional.of(testStatusLevel));
		when(statusLevelRepository.existsByMinApartBalance(new BigDecimal("20000"))).thenReturn(true);

		// When/Then
		assertThatThrownBy(() -> systemConfigService.updateStatusLevel("SILVER", updateRequest))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("minimum balance already exists");
	}

	@Test
	@DisplayName("Should delete status level successfully")
	void testDeleteStatusLevel_Success_DeletesLevel() {
		// Given
		when(statusLevelRepository.findByLevelName("SILVER")).thenReturn(Optional.of(testStatusLevel));
		doNothing().when(statusLevelRepository).delete(testStatusLevel);

		// When
		systemConfigService.deleteStatusLevel("SILVER");

		// Then
		verify(statusLevelRepository).findByLevelName("SILVER");
		verify(statusLevelRepository).delete(testStatusLevel);
	}

	@Test
	@DisplayName("Should throw exception when deleting non-existent status level")
	void testDeleteStatusLevel_NotFound_ThrowsException() {
		// Given
		when(statusLevelRepository.findByLevelName("UNKNOWN")).thenReturn(Optional.empty());

		// When/Then
		assertThatThrownBy(() -> systemConfigService.deleteStatusLevel("UNKNOWN"))
				.isInstanceOf(BusinessException.class)
				.hasMessageContaining("Status level not found");
	}

	@Test
	@DisplayName("Should create status level with default values")
	void testCreateStatusLevel_WithDefaults_UsesDefaults() {
		// Given
		StatusLevelRequest request = StatusLevelRequest.builder()
				.levelName("ELITE")
				.minApartBalance(new BigDecimal("100000"))
				.apBoostPerHour(new BigDecimal("7.77"))
				.build(); // No includesProAutoMining, displayOrder, isActive specified

		when(statusLevelRepository.existsByLevelName("ELITE")).thenReturn(false);
		when(statusLevelRepository.existsByMinApartBalance(new BigDecimal("100000"))).thenReturn(false);
		when(statusLevelRepository.save(any(StatusLevel.class))).thenAnswer(invocation -> {
			StatusLevel level = invocation.getArgument(0);
			level.setLevelId(4L);
			level.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
			level.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
			return level;
		});

		// When
		StatusLevelResponse result = systemConfigService.createStatusLevel(request, 1L);

		// Then
		assertThat(result).isNotNull();
		verify(statusLevelRepository).save(argThat(level ->
				level.getIncludesProAutoMining() == false &&
						level.getDisplayOrder() == 0 &&
						level.getIsActive() == true
		));
	}

	@Test
	@DisplayName("Should include status levels in system config")
	void testGetSystemConfig_IncludesStatusLevels_ReturnsLevels() {
		// Given
		StatusLevel level1 = StatusLevel.builder()
				.levelId(1L)
				.levelName("SILVER")
				.minApartBalance(new BigDecimal("5000"))
				.apBoostPerHour(new BigDecimal("2"))
				.includesProAutoMining(false)
				.displayOrder(0)
				.isActive(true)
				.build();

		StatusLevel level2 = StatusLevel.builder()
				.levelId(2L)
				.levelName("PLATINUM")
				.minApartBalance(new BigDecimal("50000"))
				.apBoostPerHour(new BigDecimal("5"))
				.includesProAutoMining(true)
				.displayOrder(1)
				.isActive(true)
				.build();

		// Mock staking tier repository (required for getSystemConfig)
		when(stakingTierRepository.findByIsActiveTrueOrderByDisplayOrderAsc())
				.thenReturn(Collections.emptyList());

		when(statusLevelRepository.findByIsActiveTrueOrderByDisplayOrderAsc())
				.thenReturn(Arrays.asList(level1, level2));

		// When
		SystemConfigResponse result = systemConfigService.getSystemConfig();

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatusLevels()).isNotNull();
		assertThat(result.getStatusLevels()).hasSize(2);
		assertThat(result.getStatusLevels().get(0).getLevelName()).isEqualTo("SILVER");
		assertThat(result.getStatusLevels().get(1).getLevelName()).isEqualTo("PLATINUM");
		verify(statusLevelRepository).findByIsActiveTrueOrderByDisplayOrderAsc();
	}
}

