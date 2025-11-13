package com.example.bookingservice.controller;

import com.example.bookingservice.dto.AuthRequestDTO;
import com.example.bookingservice.dto.AuthResponseDTO;
import com.example.bookingservice.dto.UserDTO;
import com.example.bookingservice.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public AuthResponseDTO register(@RequestBody @Valid UserDTO userDTO) {
        return authService.register(userDTO);
    }

    @PostMapping("/auth")
    public AuthResponseDTO authenticate(@RequestBody @Valid AuthRequestDTO request) {
        return authService.authenticate(request);
    }
}