package com.tuttime.booking.controller;

import com.tuttime.booking.dto.BookingDto;
import com.tuttime.booking.dto.BookingHistoryDto;
import com.tuttime.booking.dto.BookingSetupDto;
import com.tuttime.booking.service.BookingService;
import com.tuttime.common.auth.security.UserContext;
import com.tuttime.common.dto.ApiRes;
import com.tuttime.common.enums.BookingStatus;
import com.tuttime.common.exception.ForbiddenException;
import com.tuttime.common.exception.UnauthorizedException;
import com.tuttime.common.util.UuidUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Tag(name = "Booking Management", description = "Core service for table reservations and status tracking")
public class BookingController {

    private final BookingService bookingService;

    // Booking CRUD

    @PostMapping
    @Operation(summary = "Create a booking", description = "Creates a PENDING booking and locks the table in Redis.")
    public ResponseEntity<ApiRes<BookingDto.BookingResponse>> createBooking(
            @Valid @RequestBody BookingDto.BookingRequest request) {

        validateUser();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        BookingDto.BookingResponse data = bookingService.createBooking(request, currentUserId, userRoles);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(data));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking detail", description = "Updates booking status to COMPLETED/ARRIVED when guest arrives.")
    public ResponseEntity<ApiRes<BookingDto.BookingDetailResponse>> getBooking(
            @PathVariable UUID bookingId) {

        validateUser();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        BookingDto.BookingDetailResponse data = bookingService.getBooking(bookingId, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(data));
    }

    @PutMapping("/{bookingId}")
    @Operation(summary = "Edit a booking", description = "Updates time, party size, or guest info. Handles re-locking tables in Redis.")
    public ResponseEntity<ApiRes<BookingDto.BookingResponse>> editBooking(
            @PathVariable UUID bookingId,
            @Valid @RequestBody BookingDto.BookingUpdateRequest request) {

        validateUser();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        // The service will handle releasing the old table lock and acquiring the new one
        BookingDto.BookingResponse data = bookingService.editBooking(bookingId, request, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(data));
    }

    @PostMapping("/{bookingId}/status")
    @Operation(summary = "Update booking state", description = "Updates booking status to COMPLETED/ARRIVED when guest arrives.")
    public ResponseEntity<ApiRes<BookingDto.BookingResponse>> updateBooking(
            @PathVariable UUID bookingId,
            @RequestParam BookingStatus newStatus,
            @RequestParam(required = false) String reason) {

        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        BookingDto.BookingResponse data = bookingService.updateStatus(bookingId, newStatus, reason, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(data));
    }

    @DeleteMapping("/{bookingId}")
    @Operation(summary = "Cancel a booking", description = "Sets status to CANCELED and records the reason.")
    public ResponseEntity<ApiRes<Void>> deleteBooking(
            @PathVariable UUID bookingId,
            @RequestParam UUID userId,
            @RequestParam(required = false) String reason) {

        validateUser();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        bookingService.deleteBooking(bookingId, userId, reason, currentUserId, userRoles);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success(null));
    }

    @GetMapping("/{bookingId}/history")
    @Operation(summary = "Get audit trail", description = "Returns timeline of status changes from booking_status_histories table.")
    public ResponseEntity<ApiRes<List<BookingHistoryDto.BookingHistoryResponse>>> getBookingHistory(
            @PathVariable UUID bookingId) {

        validateUser();

        List<BookingHistoryDto.BookingHistoryResponse> data = bookingService.getStatusHistory(bookingId);
        return ResponseEntity.ok(ApiRes.success(data));
    }

    @GetMapping("/branch/{branchId}")
    @Operation(summary = "Admin: Get branch bookings with date range, status, and pagination")
    public ResponseEntity<ApiRes<Page<BookingDto.BookingDetailResponse>>> getBranchBookings(
            @PathVariable UUID branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime toDate,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "bookingDatetime") String sort,
            @RequestParam(defaultValue = "desc") String direction) {

        validateAdminOrOwnerOrManager();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());
        String userRoles = UserContext.getRoles();

        Pageable pageable = PageRequest.of(page, size,
                direction.equalsIgnoreCase("asc") ? Sort.by(sort).ascending() : Sort.by(sort).descending());

        Page<BookingDto.BookingDetailResponse> data = bookingService.getBookingsByBranch(
                branchId, fromDate, toDate, status, pageable, currentUserId, userRoles);
        return ResponseEntity.ok(ApiRes.success(data));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get my bookings", description = "Returns the booking history and upcoming reservations for the current user.")
    public ResponseEntity<ApiRes<List<BookingDto.BookingDetailResponse>>> getMyBookings(
            @PathVariable UUID userId) {

        validateUser();

        List<BookingDto.BookingDetailResponse> data = bookingService.getBookingsByUser(userId);
        return ResponseEntity.ok(ApiRes.success(data));
    }

    @GetMapping("/branch/{branchId}/slots")
    public ResponseEntity<ApiRes<BookingSetupDto.BookingSetupResponse>> getAvailableSlots(
            @Parameter(description = "The unique ID of the branch")
            @PathVariable UUID branchId,

            @Parameter(description = "Date in ISO format (e.g., 2026-04-16T10:00:00+02:00)")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) OffsetDateTime date,

            @Parameter(description = "Number of people in the booking party")
            @RequestParam Integer partySize) {

        validateUser();
        UUID currentUserId = UuidUtil.toUuid(UserContext.getUserId());

        BookingSetupDto.BookingSetupResponse data = bookingService.getAvailableSlots(branchId, date, partySize, currentUserId);
        return ResponseEntity.ok(ApiRes.success(data));
    }

    /**
     * Private helper to keep the controller DRY
     */
    private void validateLogin() {
        if (!UserContext.isValidUUID()) {
            throw new UnauthorizedException("Unauthorized: You should login");
        }
    }

    private void validateAdmin() {
        if (!UserContext.isValidUUID() || !UserContext.isRoleAdmin()) {
            throw new ForbiddenException("Access forbidden: Insufficient permissions");
        }
    }

    private void validateAdminOrOwner() {
        if (!UserContext.isValidUUID()
                || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner())) {
            throw new ForbiddenException("Access forbidden: Insufficient permissions");
        }
    }

    private void validateAdminOrOwnerOrManager() {
        if (!UserContext.isValidUUID()
                || !(UserContext.isRoleAdmin() || UserContext.isRoleOwner() || UserContext.isRoleManager())) {
            throw new ForbiddenException("Access forbidden: Insufficient permissions");
        }
    }


    private void validateUser() {
        if (!UserContext.isValidUUID()) {
            throw new ForbiddenException("Access forbidden: Insufficient permissions");
        }
    }
}
