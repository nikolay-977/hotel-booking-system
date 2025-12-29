package com.example.bookingservice.service;

import com.example.bookingservice.client.HotelServiceClient;
import com.example.bookingservice.dto.AvailabilityRequestDTO;
import com.example.bookingservice.dto.BookingDTO;
import com.example.bookingservice.dto.BookingRequestDTO;
import com.example.bookingservice.dto.RoomDTO;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.entity.User;
import com.example.bookingservice.exception.BookingConflictException;
import com.example.bookingservice.exception.BookingNotFoundException;
import com.example.bookingservice.mapper.BookingMapper;
import com.example.bookingservice.repository.BookingRepository;
import com.example.bookingservice.repository.UserRepository;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.retry.annotation.EnableRetry;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@EnableRetry
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HotelServiceClient hotelServiceClient;

    @Mock
    private BookingMapper bookingMapper;

    @InjectMocks
    private BookingService bookingService;

    private User testUser;
    private Booking testBooking;
    private BookingRequestDTO validRequest;
    private RoomDTO testRoom;
    private LocalDate tomorrow;
    private LocalDate nextWeek;
    private Request feignRequest;

    @BeforeEach
    void setUp() {
        tomorrow = LocalDate.now().plusDays(1);
        nextWeek = tomorrow.plusDays(7);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .build();

        testBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(nextWeek)
                .status(Booking.BookingStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .correlationId("test-correlation-id")
                .build();

        validRequest = BookingRequestDTO.builder()
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(nextWeek)
                .autoSelect(false)
                .build();

        testRoom = RoomDTO.builder()
                .id(101L)
                .number("101")
                .available(true)
                .timesBooked(5)
                .hotelId(1L)
                .build();

        // Создаем мок Request для FeignException
        feignRequest = Request.create(
                Request.HttpMethod.GET,
                "http://localhost:8080/api/rooms",
                new HashMap<>(),
                null,
                new RequestTemplate()
        );
    }

    private FeignException.Unauthorized createUnauthorizedException() {
        return new FeignException.Unauthorized(
                "Unauthorized",
                feignRequest,
                "Unauthorized".getBytes(),
                new HashMap<>()
        );
    }

    private FeignException.Forbidden createForbiddenException() {
        return new FeignException.Forbidden(
                "Forbidden",
                feignRequest,
                "Forbidden".getBytes(),
                new HashMap<>()
        );
    }

    private FeignException.BadGateway createBadGatewayException() {
        return new FeignException.BadGateway(
                "Bad Gateway",
                feignRequest,
                "Bad Gateway".getBytes(),
                new HashMap<>()
        );
    }

    private FeignException createGenericFeignException() {
        return new FeignException.InternalServerError(
                "Internal Server Error",
                feignRequest,
                "Internal Server Error".getBytes(),
                new HashMap<>()
        );
    }

    @Test
    void createBooking_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.existsConflictingBooking(101L, tomorrow, nextWeek)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(1L);
            return booking;
        });
        when(hotelServiceClient.confirmAvailability(eq(101L), any(AvailabilityRequestDTO.class))).thenReturn(true);
        when(bookingRepository.findConflictingBookings(101L, tomorrow, nextWeek)).thenReturn(Collections.emptyList());

        BookingDTO bookingDTO = BookingDTO.builder()
                .id(1L)
                .userId(1L)
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(nextWeek)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(bookingDTO);

        // Act
        BookingDTO result = bookingService.createBooking(validRequest, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(Booking.BookingStatus.CONFIRMED, result.getStatus());

        verify(userRepository, times(1)).findById(1L);
        verify(bookingRepository, times(1)).existsConflictingBooking(101L, tomorrow, nextWeek);
        verify(bookingRepository, times(2)).save(any(Booking.class));
        verify(hotelServiceClient, times(1)).confirmAvailability(eq(101L), any(AvailabilityRequestDTO.class));
        verify(bookingMapper, times(1)).toDto(any(Booking.class));
    }

    @Test
    void createBooking_AutoSelectRoom_Success() {
        // Arrange
        BookingRequestDTO autoSelectRequest = BookingRequestDTO.builder()
                .startDate(tomorrow)
                .endDate(nextWeek)
                .autoSelect(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(hotelServiceClient.getRecommendedRooms()).thenReturn(Arrays.asList(testRoom));
        when(bookingRepository.existsConflictingBooking(101L, tomorrow, nextWeek)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(1L);
            return booking;
        });
        when(hotelServiceClient.confirmAvailability(eq(101L), any(AvailabilityRequestDTO.class))).thenReturn(true);
        when(bookingRepository.findConflictingBookings(101L, tomorrow, nextWeek)).thenReturn(Collections.emptyList());

        BookingDTO bookingDTO = BookingDTO.builder()
                .id(1L)
                .userId(1L)
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(nextWeek)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
        when(bookingMapper.toDto(any(Booking.class))).thenReturn(bookingDTO);

        // Act
        BookingDTO result = bookingService.createBooking(autoSelectRequest, 1L);

        // Assert
        assertNotNull(result);
        verify(hotelServiceClient, times(1)).getRecommendedRooms();
        verify(bookingRepository).existsConflictingBooking(101L, tomorrow, nextWeek);
    }

    @Test
    void createBooking_AutoSelectNoRoomsAvailable_ThrowsException() {
        // Arrange
        BookingRequestDTO autoSelectRequest = BookingRequestDTO.builder()
                .startDate(tomorrow)
                .endDate(nextWeek)
                .autoSelect(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(hotelServiceClient.getRecommendedRooms()).thenReturn(Collections.emptyList());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(autoSelectRequest, 1L);
        });

        assertTrue(exception.getMessage().contains("No available rooms found"));
        verify(hotelServiceClient, times(1)).getRecommendedRooms();
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(validRequest, 1L);
        });

        assertTrue(exception.getMessage().contains("User not found"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_BookingConflict_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.existsConflictingBooking(101L, tomorrow, nextWeek)).thenReturn(true);
        when(bookingRepository.findConflictingBookings(101L, tomorrow, nextWeek))
                .thenReturn(Arrays.asList(testBooking));

        // Act & Assert
        BookingConflictException exception = assertThrows(BookingConflictException.class, () -> {
            bookingService.createBooking(validRequest, 1L);
        });

        assertTrue(exception.getMessage().contains("Room is already booked"));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_InvalidDates_StartDateInPast_ThrowsException() {
        // Arrange
        LocalDate yesterday = LocalDate.now().minusDays(1);
        BookingRequestDTO invalidRequest = BookingRequestDTO.builder()
                .roomId(101L)
                .startDate(yesterday)
                .endDate(nextWeek)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(invalidRequest, 1L);
        });

        assertEquals("Start date cannot be in the past", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_InvalidDates_EndDateBeforeStart_ThrowsException() {
        // Arrange
        BookingRequestDTO invalidRequest = BookingRequestDTO.builder()
                .roomId(101L)
                .startDate(nextWeek)
                .endDate(tomorrow)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(invalidRequest, 1L);
        });

        assertEquals("End date cannot be before start date", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_InvalidDates_SameStartAndEnd_ThrowsException() {
        // Arrange
        BookingRequestDTO invalidRequest = BookingRequestDTO.builder()
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(tomorrow)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(invalidRequest, 1L);
        });

        assertEquals("Minimum booking period is 1 day", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_InvalidDates_TooLongPeriod_ThrowsException() {
        // Arrange
        LocalDate farFuture = tomorrow.plusDays(31);
        BookingRequestDTO invalidRequest = BookingRequestDTO.builder()
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(farFuture)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(invalidRequest, 1L);
        });

        assertEquals("Maximum booking period is 30 days", exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_RoomNotAvailable_CancelsBooking() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.existsConflictingBooking(101L, tomorrow, nextWeek)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(1L);
            return booking;
        });
        when(hotelServiceClient.confirmAvailability(eq(101L), any(AvailabilityRequestDTO.class))).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(validRequest, 1L);
        });

        assertTrue(exception.getMessage().contains("Room not available"));

        // Verify booking was saved with CANCELLED status
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, times(2)).save(bookingCaptor.capture());

        // Первое сохранение - PENDING, второе - CANCELLED
        assertEquals(Booking.BookingStatus.CANCELLED, bookingCaptor.getAllValues().get(1).getStatus());
    }

    @Test
    void createBooking_FeignException_Unauthorized_AutoSelect() {
        // Arrange
        BookingRequestDTO autoSelectRequest = BookingRequestDTO.builder()
                .startDate(tomorrow)
                .endDate(nextWeek)
                .autoSelect(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(hotelServiceClient.getRecommendedRooms()).thenThrow(createUnauthorizedException());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(autoSelectRequest, 1L);
        });

        assertNotNull(exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_FeignException_Forbidden_AutoSelect() {
        // Arrange
        BookingRequestDTO autoSelectRequest = BookingRequestDTO.builder()
                .startDate(tomorrow)
                .endDate(nextWeek)
                .autoSelect(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(hotelServiceClient.getRecommendedRooms()).thenThrow(createForbiddenException());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(autoSelectRequest, 1L);
        });

        assertNotNull(exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_FeignException_CommunicationError_AutoSelect() {
        // Arrange
        BookingRequestDTO autoSelectRequest = BookingRequestDTO.builder()
                .startDate(tomorrow)
                .endDate(nextWeek)
                .autoSelect(true)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(hotelServiceClient.getRecommendedRooms()).thenThrow(createBadGatewayException());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(autoSelectRequest, 1L);
        });

        assertNotNull(exception.getMessage());
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void createBooking_ConflictDetectedBeforeConfirmation_CancelsBooking() {
        // Arrange
        Booking conflictingBooking = Booking.builder()
                .id(2L)
                .user(testUser)
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(nextWeek)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.existsConflictingBooking(101L, tomorrow, nextWeek)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(1L);
            return booking;
        });
        when(hotelServiceClient.confirmAvailability(eq(101L), any(AvailabilityRequestDTO.class))).thenReturn(true);
        when(bookingRepository.findConflictingBookings(101L, tomorrow, nextWeek))
                .thenReturn(Arrays.asList(conflictingBooking));

        // Act & Assert
        BookingConflictException exception = assertThrows(BookingConflictException.class, () -> {
            bookingService.createBooking(validRequest, 1L);
        });

        assertEquals("Room is no longer available for selected dates", exception.getMessage());

        // Verify booking was cancelled
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, times(2)).save(bookingCaptor.capture());
        assertEquals(Booking.BookingStatus.CANCELLED, bookingCaptor.getAllValues().get(1).getStatus());
    }

    @Test
    void createBooking_FeignExceptionDuringConfirmation_CancelsBookingAndReleasesLock() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.existsConflictingBooking(101L, tomorrow, nextWeek)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(1L);
            booking.setCorrelationId("test-correlation-id");
            return booking;
        });
        when(hotelServiceClient.confirmAvailability(eq(101L), any(AvailabilityRequestDTO.class)))
                .thenThrow(createGenericFeignException());
        doNothing().when(hotelServiceClient).releaseTemporaryLock(anyLong(), anyString());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(validRequest, 1L);
        });

        assertTrue(exception.getMessage().contains("Booking failed"));

        // Verify booking was cancelled and lock was released
        ArgumentCaptor<Booking> bookingCaptor = ArgumentCaptor.forClass(Booking.class);
        verify(bookingRepository, times(2)).save(bookingCaptor.capture());
        assertEquals(Booking.BookingStatus.CANCELLED, bookingCaptor.getAllValues().get(1).getStatus());
        verify(hotelServiceClient, times(1)).releaseTemporaryLock(eq(101L), anyString());
    }

    @Test
    void getUserBookings_Success() {
        // Arrange
        List<Booking> bookings = Arrays.asList(testBooking);
        List<BookingDTO> bookingDTOs = Arrays.asList(
                BookingDTO.builder()
                        .id(1L)
                        .userId(1L)
                        .roomId(101L)
                        .startDate(tomorrow)
                        .endDate(nextWeek)
                        .status(Booking.BookingStatus.CONFIRMED)
                        .build()
        );

        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(bookings);
        when(bookingMapper.toDtoList(bookings)).thenReturn(bookingDTOs);

        // Act
        List<BookingDTO> result = bookingService.getUserBookings(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
        verify(bookingRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
        verify(bookingMapper, times(1)).toDtoList(bookings);
    }

    @Test
    void getUserBookings_NoBookings_ReturnsEmptyList() {
        // Arrange
        when(bookingRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());
        when(bookingMapper.toDtoList(Collections.emptyList())).thenReturn(Collections.emptyList());

        // Act
        List<BookingDTO> result = bookingService.getUserBookings(1L);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(bookingRepository, times(1)).findByUserIdOrderByCreatedAtDesc(1L);
        verify(bookingMapper, times(1)).toDtoList(Collections.emptyList());
    }

    @Test
    void getBooking_Success() {
        // Arrange
        BookingDTO bookingDTO = BookingDTO.builder()
                .id(1L)
                .userId(1L)
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(nextWeek)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(testBooking));
        when(bookingMapper.toDto(testBooking)).thenReturn(bookingDTO);

        // Act
        BookingDTO result = bookingService.getBooking(1L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(bookingRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(bookingMapper, times(1)).toDto(testBooking);
    }

    @Test
    void getBooking_NotFound_ThrowsException() {
        // Arrange
        when(bookingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BookingNotFoundException.class, () -> {
            bookingService.getBooking(1L, 1L);
        });

        verify(bookingRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(bookingMapper, never()).toDto(any());
    }

    @Test
    void cancelBooking_ConfirmedBooking_Success() {
        // Arrange
        Booking confirmedBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(nextWeek)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(bookingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(confirmedBooking));

        // Act
        bookingService.cancelBooking(1L, 1L);

        // Assert
        verify(bookingRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(bookingRepository, times(1)).save(confirmedBooking);
        assertEquals(Booking.BookingStatus.CANCELLED, confirmedBooking.getStatus());
    }

    @Test
    void cancelBooking_PendingBooking_NoChange() {
        // Arrange
        Booking pendingBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(nextWeek)
                .status(Booking.BookingStatus.PENDING)
                .build();

        when(bookingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(pendingBooking));

        // Act
        bookingService.cancelBooking(1L, 1L);

        // Assert
        verify(bookingRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(bookingRepository, never()).save(any());
        assertEquals(Booking.BookingStatus.PENDING, pendingBooking.getStatus());
    }

    @Test
    void cancelBooking_AlreadyCancelled_NoChange() {
        // Arrange
        Booking cancelledBooking = Booking.builder()
                .id(1L)
                .user(testUser)
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(nextWeek)
                .status(Booking.BookingStatus.CANCELLED)
                .build();

        when(bookingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(cancelledBooking));

        // Act
        bookingService.cancelBooking(1L, 1L);

        // Assert
        verify(bookingRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(bookingRepository, never()).save(any());
        assertEquals(Booking.BookingStatus.CANCELLED, cancelledBooking.getStatus());
    }

    @Test
    void cancelBooking_NotFound_ThrowsException() {
        // Arrange
        when(bookingRepository.findByIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(BookingNotFoundException.class, () -> {
            bookingService.cancelBooking(1L, 1L);
        });

        verify(bookingRepository, times(1)).findByIdAndUserId(1L, 1L);
        verify(bookingRepository, never()).save(any());
    }

    @Test
    void testValidateBookingDates_NullStartDate() {
        // Arrange
        BookingRequestDTO request = BookingRequestDTO.builder()
                .roomId(101L)
                .startDate(null)
                .endDate(nextWeek)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(request, 1L);
        });

        assertEquals("Start date and end date are required", exception.getMessage());
    }

    @Test
    void testValidateBookingDates_NullEndDate() {
        // Arrange
        BookingRequestDTO request = BookingRequestDTO.builder()
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(null)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(request, 1L);
        });

        assertEquals("Start date and end date are required", exception.getMessage());
    }

    @Test
    void createBooking_FeignExceptionDuringConfirmation_FailsToReleaseLock() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.existsConflictingBooking(101L, tomorrow, nextWeek)).thenReturn(false);
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            booking.setId(1L);
            booking.setCorrelationId("test-correlation-id");
            return booking;
        });
        when(hotelServiceClient.confirmAvailability(eq(101L), any(AvailabilityRequestDTO.class)))
                .thenThrow(createGenericFeignException());
        doThrow(new RuntimeException("Failed to release lock"))
                .when(hotelServiceClient).releaseTemporaryLock(anyLong(), anyString());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            bookingService.createBooking(validRequest, 1L);
        });

        assertTrue(exception.getMessage().contains("Booking failed"));

        // Verify booking was cancelled and release lock was attempted
        verify(bookingRepository, times(2)).save(any(Booking.class));
        verify(hotelServiceClient, times(1)).releaseTemporaryLock(anyLong(), anyString());
    }

    @Test
    void createBooking_MultipleConflictingBookings_ThrowsExceptionWithCount() {
        // Arrange
        Booking conflictingBooking1 = Booking.builder()
                .id(2L)
                .user(testUser)
                .roomId(101L)
                .startDate(tomorrow)
                .endDate(nextWeek)
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        Booking conflictingBooking2 = Booking.builder()
                .id(3L)
                .user(testUser)
                .roomId(101L)
                .startDate(tomorrow.minusDays(2))
                .endDate(nextWeek.plusDays(2))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookingRepository.existsConflictingBooking(101L, tomorrow, nextWeek)).thenReturn(true);
        when(bookingRepository.findConflictingBookings(101L, tomorrow, nextWeek))
                .thenReturn(Arrays.asList(conflictingBooking1, conflictingBooking2));

        // Act & Assert
        BookingConflictException exception = assertThrows(BookingConflictException.class, () -> {
            bookingService.createBooking(validRequest, 1L);
        });

        assertTrue(exception.getMessage().contains("Conflicting bookings: 2"));
        verify(bookingRepository, never()).save(any());
    }
}