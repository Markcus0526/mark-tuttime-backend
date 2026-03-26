package com.tuttime.review.service;

import com.tuttime.review.dto.ReviewModerationLogDto;

import java.util.List;
import java.util.UUID;

public interface ReviewModerationLogService {

	ReviewModerationLogDto getById(UUID id);

	List<ReviewModerationLogDto> getByReviewId(UUID reviewId);

	List<ReviewModerationLogDto> getByModeratorId(UUID moderatorId);

	ReviewModerationLogDto create(ReviewModerationLogDto dto);

	void delete(UUID id);
}
