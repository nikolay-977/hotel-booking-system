package com.example.bookingservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequestDTO {
    private Long roomId;
    private LocalDate startDate;
    private LocalDate endDate;
    @Builder.Default
    private Boolean autoSelect = false;
}