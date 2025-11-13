package com.example.hotelservice.service;

import com.example.hotelservice.dto.RoomDTO;
import com.example.hotelservice.dto.AvailabilityRequestDTO;
import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.entity.Room;
import com.example.hotelservice.mapper.RoomMapper;
import com.example.hotelservice.repository.HotelRepository;
import com.example.hotelservice.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;
    private final RoomMapper roomMapper;

    private final ConcurrentHashMap<String, Long> temporaryLocks = new ConcurrentHashMap<>();

    public RoomDTO createRoom(RoomDTO roomDTO) {
        log.info("Creating new room for hotel ID: {}", roomDTO.getHotelId());

        Hotel hotel = hotelRepository.findById(roomDTO.getHotelId())
                .orElseThrow(() -> {
                    log.error("Hotel not found with ID: {}", roomDTO.getHotelId());
                    return new RuntimeException("Hotel not found with ID: " + roomDTO.getHotelId());
                });

        Room room = roomMapper.toEntityWithHotel(roomDTO, hotel);
        Room savedRoom = roomRepository.save(room);

        log.info("Room created successfully with ID: {} for hotel ID: {}",
                savedRoom.getId(), roomDTO.getHotelId());
        return roomMapper.toDto(savedRoom);
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getAvailableRooms() {
        log.info("Retrieving all available rooms");
        List<Room> rooms = roomRepository.findAvailableRoomsOrderByTimesBooked();
        return roomMapper.toDtoList(rooms);
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getRecommendedRooms() {
        log.info("Retrieving recommended rooms (sorted by times booked)");
        List<Room> rooms = roomRepository.findAvailableRoomsOrderByTimesBooked();
        return roomMapper.toDtoList(rooms);
    }

    @Transactional(readOnly = true)
    public RoomDTO getRoomById(Long id) {
        log.info("Retrieving room by ID: {}", id);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Room not found with ID: {}", id);
                    return new RuntimeException("Room not found with ID: " + id);
                });
        return roomMapper.toDto(room);
    }

    @Transactional(readOnly = true)
    public List<RoomDTO> getRoomsByHotelId(Long hotelId) {
        log.info("Retrieving rooms for hotel ID: {}", hotelId);
        List<Room> rooms = roomRepository.findByHotelIdAndAvailableTrue(hotelId);
        return roomMapper.toDtoList(rooms);
    }

    public boolean confirmAvailability(Long roomId, AvailabilityRequestDTO request) {
        try {
            log.info("Confirming availability for room ID: {} with correlationId: {}",
                    roomId, request.getCorrelationId());

            if (temporaryLocks.containsKey(request.getCorrelationId())) {
                log.info("Request already processed for correlationId: {}", request.getCorrelationId());
                return true;
            }

            if (!roomRepository.existsById(roomId)) {
                log.warn("Room not found with ID: {}", roomId);
                return false;
            }

            Room room = roomRepository.findById(roomId)
                    .orElseThrow(() -> {
                        log.error("Room not found with ID: {}", roomId);
                        return new RuntimeException("Room not found with ID: " + roomId);
                    });

            if (!room.getAvailable()) {
                log.warn("Room {} is not available", roomId);
                return false;
            }

            temporaryLocks.put(request.getCorrelationId(), roomId);
            log.info("Temporary lock created for room {} with correlationId {}",
                    roomId, request.getCorrelationId());

            incrementTimesBooked(roomId);
            return true;

        } catch (Exception e) {
            log.error("Error confirming availability for room {}: {}", roomId, e.getMessage());
            return false;
        }
    }

    public void releaseTemporaryLock(Long roomId, String correlationId) {
        temporaryLocks.remove(correlationId);
        log.info("Temporary lock released for room {} with correlationId {}",
                roomId, correlationId);
    }

    public void incrementTimesBooked(Long roomId) {
        log.info("Incrementing times booked for room ID: {}", roomId);

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> {
                    log.error("Room not found with ID: {}", roomId);
                    return new RuntimeException("Room not found with ID: " + roomId);
                });

        room.setTimesBooked(room.getTimesBooked() + 1);
        roomRepository.save(room);

        log.info("Times booked incremented for room ID: {}, new value: {}",
                roomId, room.getTimesBooked());
    }

    public RoomDTO updateRoom(Long id, RoomDTO roomDTO) {
        log.info("Updating room with ID: {}", id);

        Room existingRoom = roomRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Room not found with ID: {}", id);
                    return new RuntimeException("Room not found with ID: " + id);
                });

        roomMapper.updateRoomFromDto(roomDTO, existingRoom);

        if (roomDTO.getHotelId() != null && !roomDTO.getHotelId().equals(existingRoom.getHotel().getId())) {
            Hotel newHotel = hotelRepository.findById(roomDTO.getHotelId())
                    .orElseThrow(() -> new RuntimeException("Hotel not found with ID: " + roomDTO.getHotelId()));
            existingRoom.setHotel(newHotel);
        }

        Room updatedRoom = roomRepository.save(existingRoom);

        log.info("Room updated successfully with ID: {}", id);
        return roomMapper.toDto(updatedRoom);
    }

    public void deleteRoom(Long id) {
        log.info("Deleting room with ID: {}", id);

        if (!roomRepository.existsById(id)) {
            log.error("Room not found with ID: {}", id);
            throw new RuntimeException("Room not found with ID: " + id);
        }

        roomRepository.deleteById(id);
        log.info("Room deleted successfully with ID: {}", id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return roomRepository.existsById(id);
    }
}