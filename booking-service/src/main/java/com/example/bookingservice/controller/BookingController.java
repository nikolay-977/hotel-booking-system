package com.example.bookingservice.controller;

import com.example.bookingservice.dto.BookingDTO;
import com.example.bookingservice.dto.BookingRequestDTO;
import com.example.bookingservice.dto.UserShortDTO;
import com.example.bookingservice.service.BookingService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public BookingDTO createBooking(
            @RequestBody BookingRequestDTO request,
            @AuthenticationPrincipal UserShortDTO user) {
        return bookingService.createBooking(request, user.getId());
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public List<BookingDTO> getUserBookings(
            @AuthenticationPrincipal UserShortDTO user) {
        return bookingService.getUserBookings(user.getId());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public BookingDTO getBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserShortDTO user) {
        return bookingService.getBooking(id, user.getId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public void cancelBooking(
            @PathVariable Long id,
            @AuthenticationPrincipal UserShortDTO user) {
        bookingService.cancelBooking(id, user.getId());
    }
}