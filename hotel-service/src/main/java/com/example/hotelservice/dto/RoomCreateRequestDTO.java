package com.example.hotelservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreateRequestDTO {
    private String number;

    private Long hotelId;

    private Boolean available = true;

    private Integer timesBooked = 0;
}