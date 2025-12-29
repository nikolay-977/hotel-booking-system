package com.example.hotelservice.service;

import com.example.hotelservice.dto.HotelDTO;
import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.mapper.HotelMapper;
import com.example.hotelservice.repository.HotelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelServiceTest {

    @Mock
    private HotelRepository hotelRepository;

    @Mock
    private HotelMapper hotelMapper;

    @InjectMocks
    private HotelService hotelService;

    private Hotel hotel;
    private HotelDTO hotelDTO;
    private Hotel savedHotel;
    private HotelDTO savedHotelDTO;

    @BeforeEach
    void setUp() {
        hotel = Hotel.builder()
                .id(1L)
                .name("Test Hotel")
                .address("Test Address")
                .build();

        hotelDTO = HotelDTO.builder()
                .id(1L)
                .name("Test Hotel")
                .address("Test Address")
                .build();

        savedHotel = Hotel.builder()
                .id(1L)
                .name("Test Hotel")
                .address("Test Address")
                .build();

        savedHotelDTO = HotelDTO.builder()
                .id(1L)
                .name("Test Hotel")
                .address("Test Address")
                .build();
    }

    @Test
    void createHotel_Success() {
        // Arrange
        when(hotelMapper.toEntity(hotelDTO)).thenReturn(hotel);
        when(hotelRepository.save(hotel)).thenReturn(savedHotel);
        when(hotelMapper.toDto(savedHotel)).thenReturn(savedHotelDTO);

        // Act
        HotelDTO result = hotelService.createHotel(hotelDTO);

        // Assert
        assertNotNull(result);
        assertEquals(savedHotelDTO.getId(), result.getId());
        assertEquals(savedHotelDTO.getName(), result.getName());
        assertEquals(savedHotelDTO.getAddress(), result.getAddress());

        verify(hotelMapper).toEntity(hotelDTO);
        verify(hotelRepository).save(hotel);
        verify(hotelMapper).toDto(savedHotel);
    }

    @Test
    void getAllHotels_Success() {
        // Arrange
        List<Hotel> hotels = Arrays.asList(hotel);
        List<HotelDTO> hotelDTOs = Arrays.asList(hotelDTO);

        when(hotelRepository.findAll()).thenReturn(hotels);
        when(hotelMapper.toDtoList(hotels)).thenReturn(hotelDTOs);

        // Act
        List<HotelDTO> result = hotelService.getAllHotels();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(hotelDTOs, result);

        verify(hotelRepository).findAll();
        verify(hotelMapper).toDtoList(hotels);
    }

    @Test
    void getHotelById_Success() {
        // Arrange
        Long hotelId = 1L;
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(hotelMapper.toDto(hotel)).thenReturn(hotelDTO);

        // Act
        HotelDTO result = hotelService.getHotelById(hotelId);

        // Assert
        assertNotNull(result);
        assertEquals(hotelDTO.getId(), result.getId());

        verify(hotelRepository).findById(hotelId);
        verify(hotelMapper).toDto(hotel);
    }

    @Test
    void getHotelById_NotFound() {
        // Arrange
        Long hotelId = 999L;
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> hotelService.getHotelById(hotelId));

        assertTrue(exception.getMessage().contains("Hotel not found"));

        verify(hotelRepository).findById(hotelId);
        verify(hotelMapper, never()).toDto(any());
    }

    @Test
    void updateHotel_Success() {
        // Arrange
        Long hotelId = 1L;
        HotelDTO updateDTO = HotelDTO.builder()
                .name("Updated Hotel")
                .address("Updated Address")
                .build();

        when(hotelRepository.findById(hotelId)).thenReturn(Optional.of(hotel));
        when(hotelRepository.save(hotel)).thenReturn(savedHotel);
        when(hotelMapper.toDto(savedHotel)).thenReturn(savedHotelDTO);

        // Act
        HotelDTO result = hotelService.updateHotel(hotelId, updateDTO);

        // Assert
        assertNotNull(result);
        verify(hotelRepository).findById(hotelId);
        verify(hotelRepository).save(hotel);
        verify(hotelMapper).updateHotelFromDto(updateDTO, hotel);
        verify(hotelMapper).toDto(savedHotel);
    }

    @Test
    void updateHotel_NotFound() {
        // Arrange
        Long hotelId = 999L;
        when(hotelRepository.findById(hotelId)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> hotelService.updateHotel(hotelId, hotelDTO));

        assertTrue(exception.getMessage().contains("Hotel not found"));

        verify(hotelRepository).findById(hotelId);
        verify(hotelRepository, never()).save(any());
    }

    @Test
    void deleteHotel_Success() {
        // Arrange
        Long hotelId = 1L;
        when(hotelRepository.existsById(hotelId)).thenReturn(true);

        // Act
        hotelService.deleteHotel(hotelId);

        // Assert
        verify(hotelRepository).existsById(hotelId);
        verify(hotelRepository).deleteById(hotelId);
    }

    @Test
    void deleteHotel_NotFound() {
        // Arrange
        Long hotelId = 999L;
        when(hotelRepository.existsById(hotelId)).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> hotelService.deleteHotel(hotelId));

        assertTrue(exception.getMessage().contains("Hotel not found"));

        verify(hotelRepository).existsById(hotelId);
        verify(hotelRepository, never()).deleteById(any());
    }

    @Test
    void existsById_ReturnsTrue() {
        // Arrange
        Long hotelId = 1L;
        when(hotelRepository.existsById(hotelId)).thenReturn(true);

        // Act
        boolean result = hotelService.existsById(hotelId);

        // Assert
        assertTrue(result);
        verify(hotelRepository).existsById(hotelId);
    }

    @Test
    void existsById_ReturnsFalse() {
        // Arrange
        Long hotelId = 999L;
        when(hotelRepository.existsById(hotelId)).thenReturn(false);

        // Act
        boolean result = hotelService.existsById(hotelId);

        // Assert
        assertFalse(result);
        verify(hotelRepository).existsById(hotelId);
    }
}