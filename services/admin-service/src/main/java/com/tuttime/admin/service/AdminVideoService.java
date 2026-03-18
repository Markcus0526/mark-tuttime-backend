package com.tuttime.admin.service;

import com.tuttime.admin.dto.RejectRequest;
import com.tuttime.admin.dto.ScheduleVideoRequest;
import com.tuttime.admin.dto.VideoRequest;
import com.tuttime.admin.dto.VideoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Admin Video Service Interface
 */
public interface AdminVideoService {
    Page<VideoResponse> listVideos(Pageable pageable, String status);
    VideoResponse getVideo(Long id);
    VideoResponse createVideo(VideoRequest request, Long adminId);
    VideoResponse updateVideo(Long id, VideoRequest request);
    void deleteVideo(Long id);
    VideoResponse approveVideo(Long id, Long adminId);
    VideoResponse rejectVideo(Long id, Long adminId, RejectRequest request);
    VideoResponse publishVideo(Long id);
    VideoResponse scheduleVideo(Long id, ScheduleVideoRequest request);
}

