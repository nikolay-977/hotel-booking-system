package com.example.hotelservice.mapper;

import com.example.hotelservice.dto.HotelDTO;
import com.example.hotelservice.entity.Hotel;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface HotelMapper {

    Hotel toEntity(HotelDTO hotelDTO);

    HotelDTO toDto(Hotel hotel);

    List<HotelDTO> toDtoList(List<Hotel> hotels);

    List<Hotel> toEntityList(List<HotelDTO> hotelDTOs);

    default void updateHotelFromDto(HotelDTO hotelDTO, Hotel hotel) {
        if (hotelDTO == null) {
            return;
        }

        if (hotelDTO.getName() != null) {
            hotel.setName(hotelDTO.getName());
        }
        if (hotelDTO.getAddress() != null) {
            hotel.setAddress(hotelDTO.getAddress());
        }
    }
}