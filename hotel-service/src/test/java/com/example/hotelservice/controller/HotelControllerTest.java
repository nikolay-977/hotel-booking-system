package com.example.hotelservice.controller;

import com.example.hotelservice.dto.HotelDTO;
import com.example.hotelservice.dto.Role;
import com.example.hotelservice.dto.UserShortDTO;
import com.example.hotelservice.service.HotelService;
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
class HotelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private HotelService hotelService;

    private static final Long USER_ID = 1L;
    private static final Long ADMIN_ID = 2L;
    private static final Long HOTEL_ID = 1L;

    @BeforeEach
    void setUp() {
        setupMockUserAuthentication();
    }

    @Test
    void createHotel_WithAdminRole_ShouldReturnHotel() throws Exception {
        setupMockAdminAuthentication();

        HotelDTO request = createHotelDTO();
        HotelDTO expectedResponse = createHotelDTO();

        when(hotelService.createHotel(any(HotelDTO.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(HOTEL_ID),
                        jsonPath("$.name").value("Test Hotel"),
                        jsonPath("$.address").value("123 Test Street")
                );

        verify(hotelService).createHotel(any(HotelDTO.class));
    }

    @Test
    void createHotel_WithUserRole_ShouldReturnForbidden() throws Exception {
        HotelDTO request = createHotelDTO();

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(hotelService, never()).createHotel(any(HotelDTO.class));
    }

    @Test
    void getAllHotels_WithUserRole_ShouldReturnHotelsList() throws Exception {
        List<HotelDTO> expectedHotels = List.of(createHotelDTO());

        when(hotelService.getAllHotels())
                .thenReturn(expectedHotels);

        mockMvc.perform(get("/api/hotels"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1),
                        jsonPath("$[0].id").value(HOTEL_ID),
                        jsonPath("$[0].name").value("Test Hotel"),
                        jsonPath("$[0].address").value("123 Test Street")
                );

        verify(hotelService).getAllHotels();
    }

    @Test
    void getAllHotels_WithAdminRole_ShouldReturnHotelsList() throws Exception {
        setupMockAdminAuthentication();

        List<HotelDTO> expectedHotels = List.of(createHotelDTO());

        when(hotelService.getAllHotels())
                .thenReturn(expectedHotels);

        mockMvc.perform(get("/api/hotels"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(1)
                );

        verify(hotelService).getAllHotels();
    }

    @Test
    void getHotel_WithUserRole_ShouldReturnHotel() throws Exception {
        HotelDTO expectedHotel = createHotelDTO();

        when(hotelService.getHotelById(eq(HOTEL_ID)))
                .thenReturn(expectedHotel);

        mockMvc.perform(get("/api/hotels/{id}", HOTEL_ID))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(HOTEL_ID),
                        jsonPath("$.name").value("Test Hotel"),
                        jsonPath("$.address").value("123 Test Street")
                );

        verify(hotelService).getHotelById(eq(HOTEL_ID));
    }

    @Test
    void deleteHotel_WithAdminRole_ShouldReturnOk() throws Exception {
        setupMockAdminAuthentication();

        doNothing().when(hotelService).deleteHotel(HOTEL_ID);

        mockMvc.perform(delete("/api/hotels/{id}", HOTEL_ID))
                .andExpect(status().isOk());

        verify(hotelService).deleteHotel(HOTEL_ID);
    }

    @Test
    void deleteHotel_WithUserRole_ShouldReturnForbidden() throws Exception {
        mockMvc.perform(delete("/api/hotels/{id}", HOTEL_ID))
                .andExpect(status().isForbidden());

        verify(hotelService, never()).deleteHotel(HOTEL_ID);
    }

    @Test
    void getAllHotels_WhenNoHotels_ShouldReturnEmptyList() throws Exception {
        when(hotelService.getAllHotels())
                .thenReturn(List.of());

        mockMvc.perform(get("/api/hotels"))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.length()").value(0)
                );

        verify(hotelService).getAllHotels();
    }

    @Test
    void getHotel_WhenNotFound_ShouldReturnNotFound() throws Exception {
        when(hotelService.getHotelById(eq(HOTEL_ID)))
                .thenReturn(null);

        mockMvc.perform(get("/api/hotels/{id}", HOTEL_ID))
                .andExpect(status().isOk());

        verify(hotelService).getHotelById(eq(HOTEL_ID));
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

    private HotelDTO createHotelDTO() {
        return HotelDTO.builder()
                .id(HOTEL_ID)
                .name("Test Hotel")
                .address("123 Test Street")
                .build();
    }
}