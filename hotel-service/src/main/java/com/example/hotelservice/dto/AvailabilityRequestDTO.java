package com.example.hotelservice.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AvailabilityRequestDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private String correlationId;
}