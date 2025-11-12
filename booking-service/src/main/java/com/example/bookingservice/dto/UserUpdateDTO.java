package com.example.bookingservice.dto;

import lombok.Data;
import jakarta.validation.constraints.Size;

@Data
public class UserUpdateDTO {

    @Size(min = 2, max = 50, message = "Username must be between 2 and 50 characters")
    private String username;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
}
