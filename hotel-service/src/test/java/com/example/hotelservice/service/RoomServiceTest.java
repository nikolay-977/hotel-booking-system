package com.example.hotelservice.service;

import com.example.hotelservice.dto.AvailabilityRequestDTO;
import com.example.hotelservice.dto.RoomDTO;
import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.entity.Room;
import com.example.hotelservice.mapper.RoomMapper;
import com.example.hotelservice.repository.HotelRepository;
import com.example.hotelservice.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    private Hotel hotel;
    private Room room;
    private RoomDTO roomDTO;
    private Room savedRoom;
    private RoomDTO savedRoomDTO;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder()
                .id(1L)
                .name("Test Hotel")
                .address("Test Address")
                .build();

        room = Room.builder()
                .id(1L)
                .number("101")
                .available(true)
                .timesBooked(0)
                .hotel(hotel)
                .build();

        roomDTO = RoomDTO.builder()
                .id(1L)
                .number("101")
                .available(true)
                .timesBooked(0)
                .hotelId(1L)
                .build();

        savedRoom = Room.builder()
                .id(1L)
                .number("101")
                .available(true)
                .timesBooked(0)
                .hotel(hotel)
                .build();

        savedRoomDTO = RoomDTO.builder()
                .id(1L)
                .number("101")
                .available(true)
                .timesBooked(0)
                .hotelId(1L)
                .build();
    }

    @Test
    void createRoom_Success() {
        // Arrange
        when(hotelRepository.findById(roomDTO.getHotelId())).thenReturn(Optional.of(hotel));
        when(roomMapper.toEntityWithHotel(roomDTO, hotel)).thenReturn(room);
        when(roomRepository.save(room)).thenReturn(savedRoom);
        when(roomMapper.toDto(savedRoom)).thenReturn(savedRoomDTO);

        // Act
        RoomDTO result = roomService.createRoom(roomDTO);

        // Assert
        assertNotNull(result);
        assertEquals(savedRoomDTO.getId(), result.getId());
        assertEquals(savedRoomDTO.getNumber(), result.getNumber());

        verify(hotelRepository).findById(roomDTO.getHotelId());
        verify(roomMapper).toEntityWithHotel(roomDTO, hotel);
        verify(roomRepository).save(room);
        verify(roomMapper).toDto(savedRoom);
    }

    @Test
    void createRoom_HotelNotFound() {
        // Arrange
        Long hotelId = 999L;
        roomDTO.setHotelId(hotelId);
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> roomService.createRoom(roomDTO));

        assertTrue(exception.getMessage().contains("Hotel not found"));

        verify(hotelRepository).findById(hotelId);
        verify(roomRepository, never()).save(any());
    }

    @Test
    void getAvailableRooms_Success() {
        // Arrange
        List<Room> rooms = Arrays.asList(room);
        List<RoomDTO> roomDTOs = Arrays.asList(roomDTO);

        when(roomRepository.findAvailableRoomsOrderByTimesBooked()).thenReturn(rooms);
        when(roomMapper.toDtoList(rooms)).thenReturn(roomDTOs);

        // Act
        List<RoomDTO> result = roomService.getAvailableRooms();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(roomDTOs, result);

        verify(roomRepository).findAvailableRoomsOrderByTimesBooked();
        verify(roomMapper).toDtoList(rooms);
    }

    @Test
    void getRecommendedRooms_Success() {
        // Arrange
        List<Room> rooms = Arrays.asList(room);
        List<RoomDTO> roomDTOs = Arrays.asList(roomDTO);

        when(roomRepository.findAvailableRoomsOrderByTimesBooked()).thenReturn(rooms);
        when(roomMapper.toDtoList(rooms)).thenReturn(roomDTOs);

        // Act
        List<RoomDTO> result = roomService.getRecommendedRooms();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(roomRepository).findAvailableRoomsOrderByTimesBooked();
        verify(roomMapper).toDtoList(rooms);
    }

    @Test
    void getRoomById_Success() {
        // Arrange
        Long roomId = 1L;
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomMapper.toDto(room)).thenReturn(roomDTO);

        // Act
        RoomDTO result = roomService.getRoomById(roomId);

        // Assert
        assertNotNull(result);
        assertEquals(roomDTO.getId(), result.getId());

        verify(roomRepository).findById(roomId);
        verify(roomMapper).toDto(room);
    }

    @Test
    void getRoomById_NotFound() {
        // Arrange
        Long roomId = 999L;
        when(roomRepository.findById(roomId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> roomService.getRoomById(roomId));

        assertTrue(exception.getMessage().contains("Room not found"));

        verify(roomRepository).findById(roomId);
        verify(roomMapper, never()).toDto(any());
    }

    @Test
    void getRoomsByHotelId_Success() {
        // Arrange
        Long hotelId = 1L;
        List<Room> rooms = Arrays.asList(room);
        List<RoomDTO> roomDTOs = Arrays.asList(roomDTO);

        when(roomRepository.findByHotelIdAndAvailableTrue(hotelId)).thenReturn(rooms);
        when(roomMapper.toDtoList(rooms)).thenReturn(roomDTOs);

        // Act
        List<RoomDTO> result = roomService.getRoomsByHotelId(hotelId);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());

        verify(roomRepository).findByHotelIdAndAvailableTrue(hotelId);
        verify(roomMapper).toDtoList(rooms);
    }

    @Test
    void confirmAvailability_Success() {
        // Arrange
        Long roomId = 1L;
        String correlationId = "test-correlation-id";
        AvailabilityRequestDTO request = AvailabilityRequestDTO.builder()
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .correlationId(correlationId)
                .build();

        when(roomRepository.existsById(roomId)).thenReturn(true);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));

        // Act
        boolean result = roomService.confirmAvailability(roomId, request);

        // Assert
        assertTrue(result);
        verify(roomRepository).existsById(roomId);
        verify(roomRepository, times(2)).findById(roomId);
        verify(roomRepository).save(room);
    }

    @Test
    void confirmAvailability_AlreadyProcessed() {
        // Arrange
        Long roomId = 1L;
        String correlationId = "test-correlation-id";
        AvailabilityRequestDTO request = AvailabilityRequestDTO.builder()
                .correlationId(correlationId)
                .build();

        // Manually add correlationId to simulate already processed request
        ConcurrentHashMap<String, Long> locks = getLocksMap();
        locks.put(correlationId, roomId);

        // Act
        boolean result = roomService.confirmAvailability(roomId, request);

        // Assert
        assertTrue(result);
        verify(roomRepository, never()).findById(any());
    }

    @Test
    void confirmAvailability_RoomNotFound() {
        // Arrange
        Long roomId = 999L;
        AvailabilityRequestDTO request = AvailabilityRequestDTO.builder()
                .correlationId("test-correlation-id")
                .build();

        when(roomRepository.existsById(roomId)).thenReturn(false);

        // Act
        boolean result = roomService.confirmAvailability(roomId, request);

        // Assert
        assertFalse(result);
        verify(roomRepository).existsById(roomId);
        verify(roomRepository, never()).findById(any());
    }

    @Test
    void confirmAvailability_RoomNotAvailable() {
        // Arrange
        Long roomId = 1L;
        AvailabilityRequestDTO request = AvailabilityRequestDTO.builder()
                .correlationId("test-correlation-id")
                .build();

        Room unavailableRoom = Room.builder()
                .id(roomId)
                .available(false)
                .build();

        when(roomRepository.existsById(roomId)).thenReturn(true);
        when(roomRepository.findById(roomId)).thenReturn(Optional.of(unavailableRoom));

        // Act
        boolean result = roomService.confirmAvailability(roomId, request);

        // Assert
        assertFalse(result);
        verify(roomRepository).findById(roomId);
    }

    @Test
    void incrementTimesBooked_Success() {
        // Arrange
        Long roomId = 1L;
        room.setTimesBooked(5);

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomRepository.save(room)).thenReturn(room);

        // Act
        roomService.incrementTimesBooked(roomId);

        // Assert
        assertEquals(6, room.getTimesBooked());
        verify(roomRepository).findById(roomId);
        verify(roomRepository).save(room);
    }

    @Test
    void releaseTemporaryLock_Success() {
        // Arrange
        Long roomId = 1L;
        String correlationId = "test-correlation-id";
        ConcurrentHashMap<String, Long> locks = getLocksMap();
        locks.put(correlationId, roomId);

        // Act
        roomService.releaseTemporaryLock(roomId, correlationId);

        // Assert
        assertFalse(locks.containsKey(correlationId));
    }

    @Test
    void updateRoom_Success() {
        // Arrange
        Long roomId = 1L;
        RoomDTO updateDTO = RoomDTO.builder()
                .number("102")
                .available(false)
                .timesBooked(10)
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(roomRepository.save(room)).thenReturn(savedRoom);
        when(roomMapper.toDto(savedRoom)).thenReturn(savedRoomDTO);

        // Act
        RoomDTO result = roomService.updateRoom(roomId, updateDTO);

        // Assert
        assertNotNull(result);
        verify(roomRepository).findById(roomId);
        verify(roomMapper).updateRoomFromDto(updateDTO, room);
        verify(roomRepository).save(room);
    }

    @Test
    void updateRoom_ChangeHotel() {
        // Arrange
        Long roomId = 1L;
        Long newHotelId = 2L;
        Hotel newHotel = Hotel.builder().id(newHotelId).build();

        RoomDTO updateDTO = RoomDTO.builder()
                .hotelId(newHotelId)
                .build();

        when(roomRepository.findById(roomId)).thenReturn(Optional.of(room));
        when(hotelRepository.findById(newHotelId)).thenReturn(Optional.of(newHotel));
        when(roomRepository.save(room)).thenReturn(savedRoom);
        when(roomMapper.toDto(savedRoom)).thenReturn(savedRoomDTO);

        // Act
        RoomDTO result = roomService.updateRoom(roomId, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals(newHotel, room.getHotel());
        verify(hotelRepository).findById(newHotelId);
    }

    @Test
    void deleteRoom_Success() {
        // Arrange
        Long roomId = 1L;
        when(roomRepository.existsById(roomId)).thenReturn(true);

        // Act
        roomService.deleteRoom(roomId);

        // Assert
        verify(roomRepository).existsById(roomId);
        verify(roomRepository).deleteById(roomId);
    }

    private ConcurrentHashMap<String, Long> getLocksMap() {
        // Helper method to access the private locks field
        try {
            var field = RoomService.class.getDeclaredField("temporaryLocks");
            field.setAccessible(true);
            return (ConcurrentHashMap<String, Long>) field.get(roomService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}