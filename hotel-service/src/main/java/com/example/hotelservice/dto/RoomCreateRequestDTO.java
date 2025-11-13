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

    @Builder.Default
    private Boolean available = true;

    @Builder.Default
    private Integer timesBooked = 0;
}