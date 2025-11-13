package com.example.hotelservice.service;

import com.example.hotelservice.dto.HotelDTO;
import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.mapper.HotelMapper;
import com.example.hotelservice.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    public HotelDTO createHotel(HotelDTO hotelDTO) {
        log.info("Creating new hotel: {}", hotelDTO.getName());

        Hotel hotel = hotelMapper.toEntity(hotelDTO);
        Hotel savedHotel = hotelRepository.save(hotel);

        log.info("Hotel created successfully with ID: {}", savedHotel.getId());
        return hotelMapper.toDto(savedHotel);
    }

    @Transactional(readOnly = true)
    public List<HotelDTO> getAllHotels() {
        log.info("Retrieving all hotels");
        List<Hotel> hotels = hotelRepository.findAll();
        return hotelMapper.toDtoList(hotels);
    }

    @Transactional(readOnly = true)
    public HotelDTO getHotelById(Long id) {
        log.info("Retrieving hotel by ID: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Hotel not found with ID: {}", id);
                    return new RuntimeException("Hotel not found with ID: " + id);
                });
        return hotelMapper.toDto(hotel);
    }

    public HotelDTO updateHotel(Long id, HotelDTO hotelDTO) {
        log.info("Updating hotel with ID: {}", id);

        Hotel existingHotel = hotelRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Hotel not found with ID: {}", id);
                    return new RuntimeException("Hotel not found with ID: " + id);
                });

        hotelMapper.updateHotelFromDto(hotelDTO, existingHotel);
        Hotel updatedHotel = hotelRepository.save(existingHotel);

        log.info("Hotel updated successfully with ID: {}", id);
        return hotelMapper.toDto(updatedHotel);
    }

    public void deleteHotel(Long id) {
        log.info("Deleting hotel with ID: {}", id);

        if (!hotelRepository.existsById(id)) {
            log.error("Hotel not found with ID: {}", id);
            throw new RuntimeException("Hotel not found with ID: " + id);
        }

        hotelRepository.deleteById(id);
        log.info("Hotel deleted successfully with ID: {}", id);
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return hotelRepository.existsById(id);
    }
}