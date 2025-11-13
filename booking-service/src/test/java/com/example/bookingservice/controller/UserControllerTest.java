package com.example.bookingservice.controller;

import com.example.bookingservice.dto.UserDTO;
import com.example.bookingservice.dto.UserUpdateDTO;
import com.example.bookingservice.entity.Role;
import com.example.bookingservice.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false"
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private UserService userService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateUser_WithAdminRole_ShouldReturnUpdatedUser() throws Exception {
        Long userId = 1L;
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("updateduser")
                .build();

        UserDTO expectedResponse = UserDTO.builder()
                .id(userId)
                .username("updateduser")
                .role(Role.ADMIN)
                .build();

        when(userService.updateUser(eq(userId), any(UserUpdateDTO.class)))
                .thenReturn(expectedResponse);

        mockMvc.perform(patch("/api/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpectAll(
                        status().isOk(),
                        jsonPath("$.id").value(userId),
                        jsonPath("$.username").value("updateduser"),
                        jsonPath("$.role").value("ADMIN")
                );

        verify(userService).updateUser(eq(userId), any(UserUpdateDTO.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void updateUser_WithUserRole_ShouldReturnForbidden() throws Exception {
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("updateduser")
                .build();

        mockMvc.perform(patch("/api/admin/users/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUser_ShouldReturnOk() throws Exception {
        Long userId = 1L;
        doNothing().when(userService).deleteUser(userId);

        mockMvc.perform(delete("/api/admin/users/{id}", userId))
                .andExpect(status().isOk());

        verify(userService).deleteUser(userId);
    }
}