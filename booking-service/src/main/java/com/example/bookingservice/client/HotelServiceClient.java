package com.example.bookingservice.client;

import com.example.bookingservice.config.FeignConfig;
import com.example.bookingservice.dto.AvailabilityRequestDTO;
import com.example.bookingservice.dto.RoomDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "hotel-service",
        path = "/api/rooms",
        configuration = FeignConfig.class
)
public interface HotelServiceClient {

    @GetMapping("/recommend")
    List<RoomDTO> getRecommendedRooms();

    @PostMapping("/{id}/confirm-availability")
    Boolean confirmAvailability(@PathVariable Long id, @RequestBody AvailabilityRequestDTO request);

    @PostMapping("/{id}/release")
    void releaseTemporaryLock(@PathVariable Long id, @RequestParam String correlationId);
}