package com.example.bookingservice.controller;

import com.example.bookingservice.dto.BookingDTO;
import com.example.bookingservice.dto.BookingRequestDTO;
import com.example.bookingservice.dto.UserShortDTO;
import com.example.bookingservice.entity.Booking;
import com.example.bookingservice.entity.Role;
import com.example.bookingservice.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class BookingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BookingService bookingService;

    private static final Long USER_ID = 1L;
    private static final Long ROOM_ID = 101L;
    private static final Long BOOKING_ID = 1L;

    @BeforeEach
    void setUp() {
        setupMockAuthentication();
    }

    @Test
    void createBooking_WithUserRole_ShouldReturnBooking() throws Exception {
        BookingRequestDTO request = createBookingRequestDTO();
        BookingDTO expectedResponse = createBookingDTO();

        when(bookingService.createBooking(any(BookingRequestDTO.class), eq(USER_ID)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(BOOKING_ID),
                        jsonPath("$.userId").value(USER_ID),
                        jsonPath("$.roomId").value(ROOM_ID),
                        jsonPath("$.status").value("CONFIRMED")
                );

        verify(bookingService).createBooking(any(BookingRequestDTO.class), eq(USER_ID));
    }

    @Test
    void getUserBookings_ShouldReturnBookingsList() throws Exception {
        List<BookingDTO> expectedBookings = List.of(createBookingDTO());

        when(bookingService.getUserBookings(eq(USER_ID)))
                .thenReturn(expectedBookings);

        mockMvc.perform(get("/api/bookings"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].id").value(BOOKING_ID),
                        jsonPath("$[0].userId").value(USER_ID),
                        jsonPath("$[0].roomId").value(ROOM_ID),
                        jsonPath("$[0].status").value("CONFIRMED")
                );

        verify(bookingService).getUserBookings(eq(USER_ID));
    }

    @Test
    void getBooking_ShouldReturnBooking() throws Exception {
        BookingDTO expectedBooking = createBookingDTO();

        when(bookingService.getBooking(eq(BOOKING_ID), eq(USER_ID)))
                .thenReturn(expectedBooking);

        mockMvc.perform(get("/api/bookings/{id}", BOOKING_ID))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(BOOKING_ID),
                        jsonPath("$.userId").value(USER_ID),
                        jsonPath("$.roomId").value(ROOM_ID),
                        jsonPath("$.status").value("CONFIRMED")
                );

        verify(bookingService).getBooking(eq(BOOKING_ID), eq(USER_ID));
    }

    @Test
    void cancelBooking_ShouldReturnOk() throws Exception {
        doNothing().when(bookingService).cancelBooking(BOOKING_ID, USER_ID);

        mockMvc.perform(delete("/api/bookings/{id}", BOOKING_ID))
                .andExpect(status().isOk());

        verify(bookingService).cancelBooking(BOOKING_ID, USER_ID);
    }

    @Test
    void getUserBookings_WhenNoBookings_ShouldReturnEmptyList() throws Exception {
        when(bookingService.getUserBookings(eq(USER_ID)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/bookings"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(0)
                );

        verify(bookingService).getUserBookings(eq(USER_ID));
    }

    @Test
    void createBooking_WithAutoSelect_ShouldReturnBooking() throws Exception {
        BookingRequestDTO request = BookingRequestDTO.builder()
                .autoSelect(true)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();

        BookingDTO expectedResponse = createBookingDTO();

        when(bookingService.createBooking(any(BookingRequestDTO.class), eq(USER_ID)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/bookings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(BOOKING_ID)
                );

        verify(bookingService).createBooking(any(BookingRequestDTO.class), eq(USER_ID));
    }

    private void setupMockAuthentication() {
        UserShortDTO user = UserShortDTO.builder()
                .id(USER_ID)
                .username("testuser")
                .role(Role.USER)
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private BookingRequestDTO createBookingRequestDTO() {
        return BookingRequestDTO.builder()
                .roomId(ROOM_ID)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .build();
    }

    private BookingDTO createBookingDTO() {
        return BookingDTO.builder()
                .id(BOOKING_ID)
                .userId(USER_ID)
                .roomId(ROOM_ID)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .status(Booking.BookingStatus.CONFIRMED)
                .build();
    }
}