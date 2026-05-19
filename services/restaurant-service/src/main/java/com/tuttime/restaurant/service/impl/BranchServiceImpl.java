package com.tuttime.restaurant.service.impl;

import com.tuttime.common.exception.ConflictException;
import com.tuttime.common.exception.InvalidRequestException;
import com.tuttime.restaurant.dto.BranchDto;
import com.tuttime.restaurant.dto.ScheduleDto;
import com.tuttime.restaurant.entity.*;
import com.tuttime.restaurant.entity.enums.MediaType;
import com.tuttime.restaurant.mapper.BranchMapper;
import com.tuttime.restaurant.mapper.ScheduleMapper;
import com.tuttime.restaurant.mapper.TagMapper;
import com.tuttime.restaurant.provider.StorageProvider;
import com.tuttime.restaurant.repository.*;
import com.tuttime.restaurant.repository.projection.BranchProjection;
import com.tuttime.restaurant.service.*;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static com.tuttime.restaurant.util.GeometryUtil.createPoint;

@Service
@AllArgsConstructor
@Slf4j
public class BranchServiceImpl implements BranchService {

    private final StaffService staffService;
    private final BranchRepository branchRepository;
    private final RestaurantRepository restaurantRepository;
    private final BranchTagRepository branchTagRepository;
    private final ScheduleRepository scheduleRepository;
    private final TagRepository tagRepository;
    private final BookmarkRepository bookmarkRepository;
    private final MediaRepository mediaRepository;
    private final StorageProvider storageProvider;
    private final BranchMapper branchMapper;
    private final TagMapper tagMapper;
    private final ScheduleMapper scheduleMapper;
    private final ZoneService zoneService;
    private final MediaService mediaService;
    private final ScheduleService scheduleService;

    @Override
    @Transactional
    public BranchDto.BranchResponse createBranch(BranchDto.BranchRequest request,
                                                 List<MultipartFile> files,
                                                 UUID currentUserId,
                                                 String userRoles) {
        Restaurant restaurant = restaurantRepository.findByIdAndDeletedFalse(request.restaurantId())
                .orElseThrow(() -> new InvalidRequestException("Restaurant not found: " + request.restaurantId()));

        staffService.validatePermissions(restaurant.getId(), currentUserId, userRoles);

        // Check unique branch name within the same restaurant
        if (branchRepository.existsByRestaurantIdAndNameIgnoreCase(restaurant.getId(), request.name())) {
            throw new ConflictException("Branch name '" + request.name() + "' already exists for this restaurant.");
        }

        // Check unique phone within the same restaurant (if phone provided)
        if (request.phone() != null && branchRepository.existsByRestaurantIdAndPhone(restaurant.getId(), request.phone())) {
            throw new ConflictException("Phone number '" + request.phone() + "' already used by another branch of this restaurant.");
        }

        Branch branch = branchMapper.toEntity(request);
        branch.setRestaurant(restaurant);
        Branch savedBranch = branchRepository.save(branch);

        // Save images if provided
        if (files != null && !files.isEmpty()) {
            saveBranchImages(savedBranch.getId(), files, savedBranch.getName());
        }

        return branchMapper.toResponse(savedBranch);
    }

    @Override
    @Transactional(readOnly = true)
    public BranchDto.BranchDetailResponse getBranchById(UUID branchId) {
        // 1. Fetch Branch
        Branch branch = branchRepository.findByIdAndDeletedFalse(branchId)
                .orElseThrow(() -> new InvalidRequestException("Branch not found"));

        // 2. Fetch Schedules
        List<Schedule> schedules = scheduleRepository.findAllByEntityIdAndDeletedFalse(branchId);

        // 3. Map to DTOs
        List<ScheduleDto.ScheduleResponse> scheduleDtos = schedules.stream()
                .map(scheduleMapper::toResponse)
                .sorted(Comparator.comparingInt(ScheduleDto.ScheduleResponse::dayOfWeek))
                .toList();

        // 5. Return detailed response
        return branchMapper.toDetailResponse(branch).toBuilder()
                .schedules(scheduleDtos)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchDto.BranchResponse> findAllBranches(UUID restaurantId, List<UUID> tagIds, String query,
                                                          BigDecimal minPrice, BigDecimal maxPrice,
                                                          Double minRating, Double maxRating,
                                                          Pageable pageable) {
        Specification<Branch> spec = createSpecification(restaurantId, tagIds, query,
                minPrice, maxPrice, minRating, maxRating);

        return branchRepository.findAll(spec, pageable)
                .map(branchMapper::toResponse);
    }

    @Override
    @Transactional
    public BranchDto.BranchResponse updateBranch(UUID branchId,
                                                 BranchDto.BranchRequest request,
                                                 List<MultipartFile> files,
                                                 UUID currentUserId,
                                                 String userRoles) {
        Branch branch = branchRepository.findByIdAndDeletedFalse(branchId)
                .orElseThrow(() -> new InvalidRequestException("Branch not found: " + branchId));

        staffService.validatePermissions(branch.getId(), currentUserId, userRoles);

        // --- Update fields only if present in request ---
        if (request.name() != null && !request.name().equals(branch.getName())) {
            if (branchRepository.existsByRestaurantIdAndNameIgnoreCaseAndIdNot(branch.getRestaurant().getId(), request.name(), branchId)) {
                throw new ConflictException("Branch name '" + request.name() + "' already exists for this restaurant.");
            }
            branch.setName(request.name());
        }

        if (request.description() != null) {
            branch.setDescription(request.description());
        }
        if (request.address() != null) {
            branch.setAddress(request.address());
        }
        if (request.phone() != null) {
            if (branchRepository.existsByRestaurantIdAndPhoneAndIdNot(branch.getRestaurant().getId(), request.phone(), branchId)) {
                throw new ConflictException("Phone number '" + request.phone() + "' already used by another branch.");
            }
            branch.setPhone(request.phone());
        }
        if (request.timezone() != null) {
            branch.setTimezone(request.timezone());
        }
        if (request.minPrice() != null) {
            branch.setMinPrice(request.minPrice());
        }
        if (request.maxPrice() != null) {
            branch.setMaxPrice(request.maxPrice());
        }
        if (request.closed() != null) {
            branch.setClosed(request.closed());
        }

        // Validate price range after potential updates
        if (branch.getMinPrice() != null && branch.getMaxPrice() != null
                && branch.getMinPrice().compareTo(branch.getMaxPrice()) > 0) {
            throw new InvalidRequestException("minPrice must be <= maxPrice");
        }

        // Update location if latitude and longitude are both provided
        if (request.latitude() != null && request.longitude() != null) {
            branch.setLocation(createPoint(request.longitude(), request.latitude()));
        }

        // --- Handle images ---
        if (files != null) {
            // Delete existing physical images and DB records
            List<Media> existingImages = mediaRepository.findByEntityIdAndMediaType(branchId, MediaType.IMAGE);
            for (Media media : existingImages) {
                try {
                    storageProvider.delete(media.getUrl());
                } catch (Exception e) {
                    log.warn("Failed to delete old image: {}", media.getUrl(), e);
                }
            }
            mediaRepository.deleteByEntityIdAndMediaType(branchId, MediaType.IMAGE);

            if (!files.isEmpty()) {
                saveBranchImages(branchId, files, branch.getName());
            }
        }

        // No explicit save needed – dirty checking will persist changes
        return branchMapper.toResponse(branch);
    }

    @Override
    @Transactional
    public void deleteBranch(UUID branchId, UUID currentUserId, String userRoles) {
        Branch branch = branchRepository.findByIdAndDeletedFalse(branchId)
                .orElseThrow(() -> new InvalidRequestException("Branch not found: " + branchId));

        staffService.validatePermissions(branch.getId(), currentUserId, userRoles);

        // Delete associated images from storage and DB
        List<Media> images = mediaRepository.findByEntityIdAndMediaType(branchId, MediaType.IMAGE);
        for (Media media : images) {
            try {
                storageProvider.delete(media.getUrl());
            } catch (Exception e) {
                log.warn("Failed to delete image: {}", media.getUrl(), e);
            }
        }
        mediaRepository.deleteByEntityIdAndMediaType(branchId, MediaType.IMAGE);

        // Soft delete branch
        branch.setDeleted(true);
        branchRepository.save(branch);
    }

    @Override
    @Transactional
    public void deleteAllByRestaurantId(UUID restaurantId) {
        List<Branch> branches = branchRepository.findAllByRestaurantIdAndDeletedFalse(restaurantId);
        if (branches.isEmpty()) return;

        for (Branch branch : branches) {
            // Delete branch images
            List<Media> images = mediaRepository.findByEntityIdAndMediaType(branch.getId(), MediaType.IMAGE);
            for (Media media : images) {
                storageProvider.delete(media.getUrl());
            }
            mediaRepository.deleteByEntityIdAndMediaType(branch.getId(), MediaType.IMAGE);

            if (!zoneService.deleteAllByBranchId(branch.getId())) return;
            mediaService.deleteAllByEntityId(branch.getId());
            scheduleService.deleteAllByEntityId(branch.getId());
        }
        branchRepository.deleteAllByRestaurantId(restaurantId);
    }

    @Transactional(readOnly = true)
    public List<BranchDto.TagResponse> getAllTags() {
        // Fetch all tags and map them to DTOs for the UI
        return tagRepository.findAll().stream()
                .map(tagMapper::toTagResponse) // Uses the iconUrl and name mapping
                .toList();
    }

    @Transactional
    public void syncBranchTags(UUID branchId, List<BranchDto.TagRequest> requests, UUID userId, String roles) {
        // 1. Security: Verify user has permission for THIS specific branch if not Admin
        List<UUID> oldTagIds = branchTagRepository.findAllByBranchId(branchId)
                .stream().map(bt -> bt.getTag().getId()).toList();

        // Logic #2: Only remove the links for this specific branch
        // Efficiently clears the bridge table entries
        branchTagRepository.deleteByBranchId(branchId);

        for (BranchDto.TagRequest req : requests) {
            Tag tag = tagRepository.findByNameIgnoreCase(req.name().trim())
                    .orElseGet(() -> tagRepository.save(Tag.builder()
                            .name(req.name().trim())
                            .iconUrl(req.iconUrl())
                            .category("GENERAL")
                            .build()));

            branchTagRepository.save(BranchTag.builder()
                    .id(new BranchTagId(branchId, tag.getId()))
                    .branch(branchRepository.getReferenceById(branchId))
                    .tag(tag)
                    .build());
        }

        cleanupOrphanTags(oldTagIds);
    }

    @Transactional
    public boolean toggleBookmark(UUID branchId, UUID userId) {
        UserBookmarkId id = new UserBookmarkId(userId, branchId);

        if (bookmarkRepository.existsById(id)) {
            bookmarkRepository.deleteById(id);
            return false; // Removed
        } else {
            // Use getReferenceById to satisfy the Branch relationship in the entity
            Branch branch = branchRepository.getReferenceById(branchId);
            UserBookmark bookmark = UserBookmark.builder()
                    .id(id)
                    .branch(branch)
                    .build();

            bookmarkRepository.save(bookmark);
            return true; // Added
        }
    }

    @Transactional(readOnly = true)
    public Page<BranchDto.BranchMapResponse> searchMap(
            double latitude,
            double longitude,
            double radiusMeters,
            List<UUID> tagIds,
            String query,
            Pageable pageable,
            UUID currentUserId) {

        String searchTerm = (query != null && !query.isBlank()) ? "%" + query.trim() + "%" : null;
        boolean ignoreTags = (tagIds == null || tagIds.isEmpty());
        UUID[] tagIdsArray = ignoreTags ? new UUID[0] : tagIds.toArray(new UUID[0]);

        // 1. High-Speed Spatial Query (Returns record with calculated distance)
        Page<BranchProjection> projections = branchRepository.findNearbyWithDistance(
                latitude, longitude, radiusMeters, tagIdsArray, ignoreTags, searchTerm, pageable);

        if (projections.isEmpty()) {
            return Page.empty();
        }

        List<UUID> favoriteIds;
        if (currentUserId != null)
            favoriteIds = bookmarkRepository.findBookmarkedBranchIdsByUserId(currentUserId);
        else {
            favoriteIds = Collections.emptyList();
        }

        // 2. Batch Fetch Entities to populate Tags and Images (Fixes N+1)
        List<UUID> branchIds = projections.getContent().stream()
                .map(BranchProjection::getBranchId).toList();

        List<Branch> branches = branchRepository.findAllByIdWithTags(branchIds);
        List<Schedule> schedules = scheduleRepository.findAllByBranchIds(branchIds);

        // 3. Merge Data into Final Response
        return projections.map(record -> {
            // Find the full entity from the pre-fetched list
            Branch entity = branches.stream()
                    .filter(b -> b.getId().equals(record.getBranchId()))
                    .findFirst()
                    .orElseThrow();

            // Use Mapper for base fields and builder to inject dynamic data
            return branchMapper.toMapResponse(entity).toBuilder()
                    .distance(record.getDistanceInMeters())
                    .availableTables(record.getAvailableTables().intValue()) // Cast Long to Int
                    .minPrice(record.getMinPrice())
                    .maxPrice(record.getMaxPrice())
                    .priceRangeLabel(branchMapper.formatPriceRange(record.getMinPrice(), record.getMaxPrice()))
                    .closed(isCurrentlyClosed(entity, schedules))
                    .favorite(favoriteIds.contains(record.getBranchId()))
                    .build();
        });
    }

    private Specification<Branch> createSpecification(UUID restaurantId, List<UUID> tagIds, String query,
                                                      BigDecimal minPrice, BigDecimal maxPrice,
                                                      Double minRating, Double maxRating) {
        return (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (restaurantId != null) {
                predicates.add(cb.equal(root.get("restaurant").get("id"), restaurantId));
            }

            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("minPrice"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("maxPrice"), maxPrice));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            // 2. Keyword search (Name or Address)
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("address")), pattern)
                ));
            }

            if (tagIds != null && !tagIds.isEmpty()) {
                // Join Branch -> BranchTag -> Tag
                Join<Branch, BranchTag> branchTagJoin = root.join("branchTags");
                predicates.add(branchTagJoin.get("tag").get("id").in(tagIds));

                // Ensure unique results if a branch matches multiple tags
                criteriaQuery.distinct(true);
            }

            // 4. Soft-delete filter (Always exclude deleted by default)
            predicates.add(cb.equal(root.get("deleted"), false));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private boolean isCurrentlyClosed(Branch branch, List<Schedule> schedules) {
        // 1. If branch is manually force-closed for all time
        if (Boolean.TRUE.equals(branch.getClosed())) return true;

        // 2. Get current time in the branch's local timezone
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of(branch.getTimezone()));
        short currentDay = (short) now.getDayOfWeek().getValue(); // 1 = Monday, 7 = Sunday
        LocalTime currentTime = now.toLocalTime();

        // 3. Find today's schedule for this specific branch
        return schedules.stream()
                .filter(s -> s.getEntityId().equals(branch.getId()) && s.getDayOfWeek().equals(currentDay))
                .findFirst()
                .map(s -> {
                    // If the schedule record says the branch is closed today
                    if (Boolean.TRUE.equals(s.getClosed())) return true;

                    // Otherwise, check if current time is outside the open/close window
                    return currentTime.isBefore(s.getOpenTime()) || currentTime.isAfter(s.getCloseTime());
                })
                .orElse(true); // Default to closed if no schedule exists for today
    }

    private void cleanupOrphanTags(List<UUID> tagIdsToCheck) {
        for (UUID tagId : tagIdsToCheck) {
            // If no branch is using this tag anymore, delete it from master table
            if (!branchTagRepository.existsByTagId(tagId)) {
                tagRepository.deleteById(tagId);
            }
        }
    }

    private void saveBranchImages(UUID branchId, List<MultipartFile> files, String altText) {
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String storedKey = storageProvider.store(file);
            Media media = Media.builder()
                    .entityId(branchId)
                    .url(storedKey)
                    .altText(altText)
                    .primaryEntity(i == 0)
                    .mediaType(MediaType.IMAGE)
                    .build();
            mediaRepository.save(media);
        }
    }
}