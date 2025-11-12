package com.example.hotelservice.repository;

import com.example.hotelservice.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findByHotelIdAndAvailableTrue(Long hotelId);

    List<Room> findByAvailableTrue();

    List<Room> findByHotelId(Long hotelId);

    Optional<Room> findByNumberAndHotelId(String number, Long hotelId);

    @Query("SELECT r FROM Room r WHERE r.available = true ORDER BY r.timesBooked ASC, r.id ASC")
    List<Room> findAvailableRoomsOrderByTimesBooked();

    @Query("SELECT r FROM Room r JOIN FETCH r.hotel WHERE r.available = true ORDER BY r.timesBooked ASC")
    List<Room> findAvailableRoomsWithHotel();

    @Query("SELECT r FROM Room r JOIN FETCH r.hotel WHERE r.hotel.id = :hotelId AND r.available = true ORDER BY r.timesBooked ASC")
    List<Room> findAvailableRoomsByHotelOrderByPopularity(Long hotelId);
}