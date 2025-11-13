package com.example.bookingservice.repository;

import com.example.bookingservice.entity.Booking;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Booking> findByIdAndUserId(Long id, Long userId);

    List<Booking> findByRoomIdAndStatus(Long roomId, Booking.BookingStatus status);

    List<Booking> findByRoomIdAndStatusAndEndDateAfterAndStartDateBefore(
            Long roomId,
            Booking.BookingStatus status,
            LocalDate startDate,
            LocalDate endDate);

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE " +
            "b.roomId = :roomId AND " +
            "b.status = 'CONFIRMED' AND " +
            "(:startDate < b.endDate AND :endDate > b.startDate)")
    boolean existsConflictingBooking(@Param("roomId") Long roomId,
                                     @Param("startDate") LocalDate startDate,
                                     @Param("endDate") LocalDate endDate);

    @Query("SELECT b FROM Booking b WHERE " +
            "b.roomId = :roomId AND " +
            "b.status = 'CONFIRMED' AND " +
            "(:startDate < b.endDate AND :endDate > b.startDate)")
    List<Booking> findConflictingBookings(@Param("roomId") Long roomId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);
}