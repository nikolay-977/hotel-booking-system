package com.example.hotelservice.controller;

import com.example.hotelservice.dto.RoomDTO;
import com.example.hotelservice.dto.AvailabilityRequestDTO;
import com.example.hotelservice.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public RoomDTO createRoom(@RequestBody RoomDTO roomDTO) {
        return roomService.createRoom(roomDTO);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<RoomDTO> getAvailableRooms() {
        return roomService.getAvailableRooms();
    }

    @GetMapping("/recommend")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<RoomDTO> getRecommendedRooms() {
        return roomService.getRecommendedRooms();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public RoomDTO getRoom(@PathVariable Long id) {
        return roomService.getRoomById(id);
    }

    @GetMapping("/hotel/{hotelId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public List<RoomDTO> getRoomsByHotel(@PathVariable Long hotelId) {
        return roomService.getRoomsByHotelId(hotelId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RoomDTO updateRoom(@PathVariable Long id, @RequestBody RoomDTO roomDTO) {
        return roomService.updateRoom(id, roomDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        roomService.deleteRoom(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/confirm-availability")
    public ResponseEntity<Boolean> confirmAvailability(
            @PathVariable Long id,
            @RequestBody AvailabilityRequestDTO request) {
        boolean available = roomService.confirmAvailability(id, request);
        return ResponseEntity.ok(available);
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseTemporaryLock(
            @PathVariable Long id,
            @RequestParam String correlationId) {
        roomService.releaseTemporaryLock(id, correlationId);
        return ResponseEntity.ok().build();
    }
}