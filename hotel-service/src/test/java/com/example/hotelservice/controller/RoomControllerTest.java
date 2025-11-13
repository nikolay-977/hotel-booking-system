package com.example.hotelservice.controller;

import com.example.hotelservice.dto.Role;
import com.example.hotelservice.dto.RoomDTO;
import com.example.hotelservice.dto.AvailabilityRequestDTO;
import com.example.hotelservice.dto.UserShortDTO;
import com.example.hotelservice.service.RoomService;
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
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoomService roomService;

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long ROOM_ID = 1L;
    private static final Long HOTEL_ID = 1L;
    private static final String CORRELATION_ID = "test-correlation-123";

    @BeforeEach
    void setUp() {
        setupMockUserAuthentication();
    }

    @Test
    void createRoom_WithAdminRole_ShouldReturnRoom() throws Exception {
        setupMockAdminAuthentication();

        RoomDTO request = createRoomDTO();
        RoomDTO expectedResponse = createRoomDTO();

        when(roomService.createRoom(any(RoomDTO.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(ROOM_ID),
                        jsonPath("$.number").value("101"),
                        jsonPath("$.timesBooked").value(0),
                        jsonPath("$.available").value(true)
                );

        verify(roomService).createRoom(any(RoomDTO.class));
    }

    @Test
    void createRoom_WithUserRole_ShouldReturnForbidden() throws Exception {
        RoomDTO request = createRoomDTO();

        mockMvc.perform(post("/api/rooms")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(roomService, never()).createRoom(any(RoomDTO.class));
    }

    @Test
    void getAvailableRooms_WithUserRole_ShouldReturnRoomsList() throws Exception {
        List<RoomDTO> expectedRooms = List.of(createRoomDTO());

        when(roomService.getAvailableRooms())
                .thenReturn(expectedRooms);

        mockMvc.perform(get("/api/rooms"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].id").value(ROOM_ID),
                        jsonPath("$[0].number").value("101"),
                        jsonPath("$[0].timesBooked").value(0)
                );

        verify(roomService).getAvailableRooms();
    }

    @Test
    void getRecommendedRooms_WithUserRole_ShouldReturnRoomsList() throws Exception {
        List<RoomDTO> expectedRooms = List.of(createRoomDTO());

        when(roomService.getRecommendedRooms())
                .thenReturn(expectedRooms);

        mockMvc.perform(get("/api/rooms/recommend"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].id").value(ROOM_ID)
                );

        verify(roomService).getRecommendedRooms();
    }

    @Test
    void getRoom_WithUserRole_ShouldReturnRoom() throws Exception {
        RoomDTO expectedRoom = createRoomDTO();

        when(roomService.getRoomById(eq(ROOM_ID)))
                .thenReturn(expectedRoom);

        mockMvc.perform(get("/api/rooms/{id}", ROOM_ID))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(ROOM_ID),
                        jsonPath("$.number").value("101"),
                        jsonPath("$.timesBooked").value(0)
                );

        verify(roomService).getRoomById(eq(ROOM_ID));
    }

    @Test
    void getRoomsByHotel_WithUserRole_ShouldReturnRoomsList() throws Exception {
        List<RoomDTO> expectedRooms = List.of(createRoomDTO());

        when(roomService.getRoomsByHotelId(eq(HOTEL_ID)))
                .thenReturn(expectedRooms);

        mockMvc.perform(get("/api/rooms/hotel/{hotelId}", HOTEL_ID))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].id").value(ROOM_ID),
                        jsonPath("$[0].hotelId").value(HOTEL_ID)
                );

        verify(roomService).getRoomsByHotelId(eq(HOTEL_ID));
    }

    @Test
    void updateRoom_WithAdminRole_ShouldReturnUpdatedRoom() throws Exception {
        setupMockAdminAuthentication();

        RoomDTO request = createRoomDTO();
        RoomDTO expectedResponse = createRoomDTO();

        when(roomService.updateRoom(eq(ROOM_ID), any(RoomDTO.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(put("/api/rooms/{id}", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(ROOM_ID),
                        jsonPath("$.number").value("101"));

        verify(roomService).updateRoom(eq(ROOM_ID), any(RoomDTO.class));
    }

    @Test
    void updateRoom_WithUserRole_ShouldReturnForbidden() throws Exception {
        RoomDTO request = createRoomDTO();

        mockMvc.perform(put("/api/rooms/{id}", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(roomService, never()).updateRoom(eq(ROOM_ID), any(RoomDTO.class));
    }

    @Test
    void deleteRoom_WithAdminRole_ShouldReturnOk() throws Exception {
        setupMockAdminAuthentication();

        doNothing().when(roomService).deleteRoom(ROOM_ID);

        mockMvc.perform(delete("/api/rooms/{id}", ROOM_ID))
                .andExpect(status().isOk());

        verify(roomService).deleteRoom(ROOM_ID);
    }

    @Test
    void deleteRoom_WithUserRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/rooms/{id}", ROOM_ID))
                .andExpect(status().isForbidden());

        verify(roomService, never()).deleteRoom(ROOM_ID);
    }

    @Test
    void confirmAvailability_ShouldReturnBoolean() throws Exception {
        AvailabilityRequestDTO request = AvailabilityRequestDTO.builder()
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .correlationId(CORRELATION_ID)
                .build();

        when(roomService.confirmAvailability(eq(ROOM_ID), any(AvailabilityRequestDTO.class)))
                .thenReturn(true);

        mockMvc.perform(post("/api/rooms/{id}/confirm-availability", ROOM_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$").value(true)
                );

        verify(roomService).confirmAvailability(eq(ROOM_ID), any(AvailabilityRequestDTO.class));
    }

    @Test
    void releaseTemporaryLock_ShouldReturnOk() throws Exception {
        doNothing().when(roomService).releaseTemporaryLock(ROOM_ID, CORRELATION_ID);

        mockMvc.perform(post("/api/rooms/{id}/release", ROOM_ID)
                        .param("correlationId", CORRELATION_ID))
                .andExpect(status().isOk());

        verify(roomService).releaseTemporaryLock(ROOM_ID, CORRELATION_ID);
    }

    @Test
    void getAvailableRooms_WhenNoRooms_ShouldReturnEmptyList() throws Exception {
        when(roomService.getAvailableRooms())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/rooms"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(0)
                );

        verify(roomService).getAvailableRooms();
    }

    private void setupMockUserAuthentication() {
        UserShortDTO user = UserShortDTO.builder()
                .id(USER_ID)
                .username("testuser")
                .role(Role.USER)
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void setupMockAdminAuthentication() {
        UserShortDTO admin = UserShortDTO.builder()
                .id(ADMIN_ID)
                .username("admin")
                .role(Role.ADMIN)
                .build();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private RoomDTO createRoomDTO() {
        return RoomDTO.builder()
                .id(ROOM_ID)
                .hotelId(HOTEL_ID)
                .number("101")
                .available(true)
                .timesBooked(0)
                .build();
    }
}