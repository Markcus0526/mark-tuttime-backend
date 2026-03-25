package com.tuttime.admin.service.impl;

import com.tuttime.admin.dto.RejectRequest;
import com.tuttime.admin.dto.ScheduleVideoRequest;
import com.tuttime.admin.dto.VideoRequest;
import com.tuttime.admin.dto.VideoResponse;
import com.tuttime.admin.rest.client.ContentServiceRestClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminVideoServiceImpl Tests")
class AdminVideoServiceImplTest {

	@Mock
	private ContentServiceRestClient contentServiceRestClient;

	@InjectMocks
	private AdminVideoServiceImpl adminVideoService;

	@Test
	@DisplayName("Should list videos successfully")
	void testListVideos_Success_ReturnsPage() {
		// Given
		PageRequest pageable = PageRequest.of(0, 10);
		String status = "ACTIVE";
		VideoResponse response = VideoResponse.builder()
				.videoId(1L)
				.title("Test Video")
				.status("ACTIVE")
				.build();

		Page<VideoResponse> page = new PageImpl<>(Arrays.asList(response), pageable, 1);
		when(contentServiceRestClient.listVideos(pageable, status)).thenReturn(page);

		// When
		Page<VideoResponse> result = adminVideoService.listVideos(pageable, status);

		// Then
		assertThat(result.getTotalElements()).isEqualTo(1);
		assertThat(result.getContent()).hasSize(1);
	}

	@Test
	@DisplayName("Should get video by ID successfully")
	void testGetVideo_Success_ReturnsVideo() {
		// Given
		Long id = 1L;
		VideoResponse response = VideoResponse.builder()
				.videoId(id)
				.title("Test Video")
				.build();

		when(contentServiceRestClient.getVideo(id)).thenReturn(response);

		// When
		VideoResponse result = adminVideoService.getVideo(id);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getVideoId()).isEqualTo(id);
	}

	@Test
	@DisplayName("Should create video successfully")
	void testCreateVideo_Success() {
		// Given
		VideoRequest request = VideoRequest.builder()
				.title("New Video")
				.videoUrl("https://example.com/video.mp4")
				.durationSeconds(180)
				.build();

		VideoResponse response = VideoResponse.builder()
				.videoId(1L)
				.title("New Video")
				.build();

		when(contentServiceRestClient.createVideo(request, 100L)).thenReturn(response);

		// When
		VideoResponse result = adminVideoService.createVideo(request, 100L);

		// Then
		assertThat(result).isNotNull();
		verify(contentServiceRestClient).createVideo(request, 100L);
	}

	@Test
	@DisplayName("Should update video successfully")
	void testUpdateVideo_Success() {
		// Given
		Long id = 1L;
		VideoRequest request = VideoRequest.builder()
				.title("Updated Title")
				.build();

		VideoResponse response = VideoResponse.builder()
				.videoId(id)
				.title("Updated Title")
				.build();

		when(contentServiceRestClient.updateVideo(id, request)).thenReturn(response);

		// When
		VideoResponse result = adminVideoService.updateVideo(id, request);

		// Then
		assertThat(result).isNotNull();
		verify(contentServiceRestClient).updateVideo(id, request);
	}

	@Test
	@DisplayName("Should delete video successfully")
	void testDeleteVideo_Success() {
		// Given
		Long id = 1L;
		doNothing().when(contentServiceRestClient).deleteVideo(id);

		// When
		adminVideoService.deleteVideo(id);

		// Then
		verify(contentServiceRestClient).deleteVideo(id);
	}

	@Test
	@DisplayName("Should approve video successfully")
	void testApproveVideo_Success() {
		// Given
		Long id = 1L;
		Long adminId = 100L;
		VideoResponse response = VideoResponse.builder()
				.videoId(id)
				.status("APPROVED")
				.build();

		when(contentServiceRestClient.approveVideo(id, adminId)).thenReturn(response);

		// When
		VideoResponse result = adminVideoService.approveVideo(id, adminId);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo("APPROVED");
	}

	@Test
	@DisplayName("Should reject video successfully")
	void testRejectVideo_Success() {
		// Given
		Long id = 1L;
		Long adminId = 100L;
		RejectRequest rejectRequest = RejectRequest.builder()
				.reason("Inappropriate content")
				.build();
		VideoResponse response = VideoResponse.builder()
				.videoId(id)
				.status("REJECTED")
				.build();

		when(contentServiceRestClient.rejectVideo(id, adminId, rejectRequest)).thenReturn(response);

		// When
		VideoResponse result = adminVideoService.rejectVideo(id, adminId, rejectRequest);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo("REJECTED");
	}

	@Test
	@DisplayName("Should publish video successfully")
	void testPublishVideo_Success() {
		// Given
		Long id = 1L;
		VideoResponse response = VideoResponse.builder()
				.videoId(id)
				.status("ACTIVE")
				.build();

		when(contentServiceRestClient.publishVideo(id)).thenReturn(response);

		// When
		VideoResponse result = adminVideoService.publishVideo(id);

		// Then
		assertThat(result).isNotNull();
		assertThat(result.getStatus()).isEqualTo("ACTIVE");
	}

	@Test
	@DisplayName("Should schedule video successfully")
	void testScheduleVideo_Success() {
		// Given
		Long id = 1L;
		OffsetDateTime scheduledDate = OffsetDateTime.now(ZoneOffset.UTC).plusDays(7);
		ScheduleVideoRequest scheduleRequest = ScheduleVideoRequest.builder()
				.scheduledAt(scheduledDate)
				.build();
		VideoResponse response = VideoResponse.builder()
				.videoId(id)
				.status("APPROVED")
				.build();

		when(contentServiceRestClient.scheduleVideo(id, scheduleRequest)).thenReturn(response);

		// When
		VideoResponse result = adminVideoService.scheduleVideo(id, scheduleRequest);

		// Then
		assertThat(result).isNotNull();
		verify(contentServiceRestClient).scheduleVideo(id, scheduleRequest);
	}
}

