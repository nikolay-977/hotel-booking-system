package com.example.bookingservice.service;

import com.example.bookingservice.client.HotelServiceClient;
import com.example.bookingservice.dto.*;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.entity.User;
import com.example.bookingservice.exception.BookingConflictException;
import com.example.bookingservice.exception.BookingNotFoundException;
import com.example.bookingservice.mapper.BookingMapper;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.UserRepository;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final HotelServiceClient hotelServiceClient;
    private final BookingMapper bookingMapper;

    @Retryable(
            value = {FeignException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public BookingDTO createBooking(BookingRequestDTO request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateBookingDates(request.getStartDate(), request.getEndDate());

        Long roomId = request.getRoomId();
        if (Boolean.TRUE.equals(request.getAutoSelect())) {
            try {
                List<RoomDTO> recommendedRooms = hotelServiceClient.getRecommendedRooms();
                if (recommendedRooms.isEmpty()) {
                    throw new RuntimeException("No available rooms found");
                }
                roomId = recommendedRooms.get(0).getId();
                log.info("Auto-selected room ID: {}", roomId);
            } catch (FeignException.Unauthorized e) {
                log.error("Unauthorized access to hotel service", e);
                throw new RuntimeException("Authentication error with hotel service");
            } catch (FeignException.Forbidden e) {
                log.error("Access forbidden to hotel service", e);
                throw new RuntimeException("Access denied to hotel service");
            } catch (FeignException e) {
                log.error("Communication error with hotel service", e);
                throw new RuntimeException("Hotel service temporarily unavailable");
            }
        }

        checkForBookingConflicts(roomId, request.getStartDate(), request.getEndDate());

        String correlationId = UUID.randomUUID().toString();

        Booking booking = Booking.builder()
                .user(user)
                .roomId(roomId)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(Booking.BookingStatus.PENDING)
                .correlationId(correlationId)
                .build();

        booking = bookingRepository.save(booking);
        log.info("Created booking in PENDING status with correlationId: {}", correlationId);

        try {
            AvailabilityRequestDTO availabilityRequest = new AvailabilityRequestDTO();
            availabilityRequest.setStartDate(request.getStartDate());
            availabilityRequest.setEndDate(request.getEndDate());
            availabilityRequest.setCorrelationId(correlationId);

            boolean available = hotelServiceClient.confirmAvailability(roomId, availabilityRequest);

            if (available) {
                if (hasBookingConflicts(roomId, request.getStartDate(), request.getEndDate(), booking.getId())) {
                    booking.setStatus(Booking.BookingStatus.CANCELLED);
                    bookingRepository.save(booking);
                    log.warn("Booking cancelled due to conflict detected before confirmation, correlationId: {}", correlationId);
                    throw new BookingConflictException("Room is no longer available for selected dates");
                }

                booking.setStatus(Booking.BookingStatus.CONFIRMED);
                bookingRepository.save(booking);
                log.info("Booking confirmed with correlationId: {}", correlationId);
            } else {
                booking.setStatus(Booking.BookingStatus.CANCELLED);
                bookingRepository.save(booking);
                log.info("Booking cancelled - room not available, correlationId: {}", correlationId);
                throw new RuntimeException("Room not available");
            }

        } catch (FeignException e) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            try {
                hotelServiceClient.releaseTemporaryLock(roomId, correlationId);
            } catch (Exception ex) {
                log.warn("Failed to release temporary lock for correlationId: {}", correlationId, ex);
            }

            log.error("Booking failed due to communication error, correlationId: {}", correlationId, e);
            throw new RuntimeException("Booking failed due to service unavailability");
        }

        return bookingMapper.toDto(booking);
    }

    private void validateBookingDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (startDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Start date cannot be in the past");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }

        if (startDate.equals(endDate)) {
            throw new IllegalArgumentException("Minimum booking period is 1 day");
        }

        if (startDate.plusDays(30).isBefore(endDate)) {
            throw new IllegalArgumentException("Maximum booking period is 30 days");
        }
    }

    private void checkForBookingConflicts(Long roomId, LocalDate startDate, LocalDate endDate) {
        boolean hasConflict = bookingRepository.existsConflictingBooking(roomId, startDate, endDate);

        if (hasConflict) {
            List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(roomId, startDate, endDate);
            log.warn("Booking conflict detected for room {}: {} conflicting bookings found",
                    roomId, conflictingBookings.size());
            throw new BookingConflictException(
                    String.format("Room is already booked for selected dates. Conflicting bookings: %d",
                            conflictingBookings.size())
            );
        }
    }

    private boolean hasBookingConflicts(Long roomId, LocalDate startDate, LocalDate endDate, Long excludeBookingId) {
        List<Booking> conflictingBookings = bookingRepository.findConflictingBookings(roomId, startDate, endDate);

        return conflictingBookings.stream()
                .anyMatch(booking -> !booking.getId().equals(excludeBookingId));
    }

    @Transactional(readOnly = true)
    public List<BookingDTO> getUserBookings(Long userId) {
        return bookingMapper.toDtoList(bookingRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Transactional(readOnly = true)
    public BookingDTO getBooking(Long id, Long userId) {
        Booking booking = bookingRepository.findByIdAndUserId(id, userId)
                .orElseThrow(BookingNotFoundException::new);
        return bookingMapper.toDto(booking);
    }

    public void cancelBooking(Long id, Long userId) {
        Booking booking = bookingRepository.findByIdAndUserId(id, userId)
                .orElseThrow(BookingNotFoundException::new);

        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            booking.setStatus(Booking.BookingStatus.CANCELLED);
            bookingRepository.save(booking);

            log.info("Booking {} cancelled by user {}", id, userId);
        }
    }
}