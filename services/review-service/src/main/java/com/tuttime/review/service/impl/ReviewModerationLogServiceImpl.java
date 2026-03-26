package com.tuttime.review.service.impl;

import com.tuttime.review.dto.ReviewModerationLogDto;
import com.tuttime.review.entity.ReviewModerationLog;
import com.tuttime.review.mapper.ReviewModerationLogMapper;
import com.tuttime.review.repository.ReviewModerationLogRepository;
import com.tuttime.review.service.ReviewModerationLogService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReviewModerationLogServiceImpl implements ReviewModerationLogService {

	private final ReviewModerationLogRepository moderationLogRepository;
	private final ReviewModerationLogMapper moderationLogMapper;

	public ReviewModerationLogServiceImpl(ReviewModerationLogRepository moderationLogRepository,
	                                      ReviewModerationLogMapper moderationLogMapper) {
		this.moderationLogRepository = moderationLogRepository;
		this.moderationLogMapper = moderationLogMapper;
	}

	@Override
	public ReviewModerationLogDto getById(UUID id) {
		ReviewModerationLog entity = moderationLogRepository.findById(id)
				.orElseThrow(() -> new EntityNotFoundException("Moderation log not found with id " + id));
		return moderationLogMapper.toDTO(entity);
	}

	@Override
	public List<ReviewModerationLogDto> getByReviewId(UUID reviewId) {
		List<ReviewModerationLog> logs = moderationLogRepository.findByReviewId(reviewId);
		return logs.stream()
				.map(moderationLogMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public List<ReviewModerationLogDto> getByModeratorId(UUID moderatorId) {
		List<ReviewModerationLog> logs = moderationLogRepository.findByModeratorId(moderatorId);
		return logs.stream()
				.map(moderationLogMapper::toDTO)
				.collect(Collectors.toList());
	}

	@Override
	public ReviewModerationLogDto create(ReviewModerationLogDto dto) {
		ReviewModerationLog entity = moderationLogMapper.toEntity(dto);
		ReviewModerationLog saved = moderationLogRepository.save(entity);
		return moderationLogMapper.toDTO(saved);
	}

	@Override
	public void delete(UUID id) {
		if (!moderationLogRepository.existsById(id)) {
			throw new EntityNotFoundException("Moderation log not found with id " + id);
		}
		moderationLogRepository.deleteById(id);
	}
}