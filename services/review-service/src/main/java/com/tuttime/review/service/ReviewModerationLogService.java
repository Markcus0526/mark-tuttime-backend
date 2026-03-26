package com.tuttime.review.service;

import com.tuttime.review.dto.ReviewModerationLogDTO;

import java.util.List;
import java.util.UUID;

public interface ReviewModerationLogService {

	ReviewModerationLogDTO getById(UUID id);

	List<ReviewModerationLogDTO> getByReviewId(UUID reviewId);

	List<ReviewModerationLogDTO> getByModeratorId(UUID moderatorId);

	ReviewModerationLogDTO create(ReviewModerationLogDTO dto);

	void delete(UUID id);
}
