package com.example.bookingservice.controller;

import com.example.bookingservice.dto.AuthRequestDTO;
import com.example.bookingservice.dto.AuthResponseDTO;
import com.example.bookingservice.dto.UserShortDTO;
import com.example.bookingservice.entity.Role;
import com.example.bookingservice.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.security.oauth2.client.registration.test-client.client-id=test-client",
        "spring.security.oauth2.client.registration.test-client.client-secret=test-secret"
})
class AuthControllerAuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    private static final String AUTHENTICATE_URL = "/api/user/auth";
    private static final String JWT_TOKEN = "jwt-token";
    private static final String USERNAME = "testuser";
    private static final String PASSWORD = "password123";

    @Test
    void authenticate_WithValidCredentials_ShouldReturnAuthResponse() throws Exception {
        AuthRequestDTO authRequest = createAuthRequestDTO();
        AuthResponseDTO expectedResponse = createAuthResponseDTO();

        when(authService.authenticate(any(AuthRequestDTO.class))).thenReturn(expectedResponse);

        mockMvc.perform(post(AUTHENTICATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authRequest)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.token").value(JWT_TOKEN),
                        jsonPath("$.user.username").value(USERNAME),
                        jsonPath("$.user.role").value(Role.USER.name())
                );

        verify(authService).authenticate(any(AuthRequestDTO.class));
    }

    @ParameterizedTest
    @MethodSource("invalidAuthDataProvider")
    void authenticate_WithVariousInvalidData_ShouldReturnBadRequest(String username, String password) throws Exception {
        AuthRequestDTO invalidAuthRequest = AuthRequestDTO.builder()
                .username(username)
                .password(password)
                .build();

        mockMvc.perform(post(AUTHENTICATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidAuthRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticate_WithAdminUser_ShouldReturnAuthResponse() throws Exception {
        AuthRequestDTO adminAuthRequest = AuthRequestDTO.builder()
                .username("adminuser")
                .password("adminpass123")
                .build();

        UserShortDTO adminShortDTO = UserShortDTO.builder()
                .username("adminuser")
                .role(Role.ADMIN)
                .build();

        AuthResponseDTO authResponse = AuthResponseDTO.builder()
                .token("admin-jwt-token")
                .user(adminShortDTO)
                .build();

        when(authService.authenticate(any(AuthRequestDTO.class))).thenReturn(authResponse);

        mockMvc.perform(post(AUTHENTICATE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminAuthRequest)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.token").value("admin-jwt-token"),
                        jsonPath("$.user.username").value("adminuser"),
                        jsonPath("$.user.role").value(Role.ADMIN.name())
                );
    }

    private static Stream<Arguments> invalidAuthDataProvider() {
        return Stream.of(
                Arguments.of("", "validPassword"),
                Arguments.of("validUser", ""),
                Arguments.of("", ""),
                Arguments.of(null, "validPassword"),
                Arguments.of("validUser", null),
                Arguments.of(null, null),
                Arguments.of("  ", "validPassword"),
                Arguments.of("user", "   ")
        );
    }

    private AuthRequestDTO createAuthRequestDTO() {
        return AuthRequestDTO.builder()
                .username(USERNAME)
                .password(PASSWORD)
                .build();
    }

    private AuthResponseDTO createAuthResponseDTO() {
        UserShortDTO userShortDTO = UserShortDTO.builder()
                .username(USERNAME)
                .role(Role.USER)
                .build();

        return AuthResponseDTO.builder()
                .token(JWT_TOKEN)
                .user(userShortDTO)
                .build();
    }
}