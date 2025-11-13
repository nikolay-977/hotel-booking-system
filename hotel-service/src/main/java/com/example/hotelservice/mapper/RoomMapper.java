package com.example.hotelservice.mapper;

import com.example.hotelservice.dto.RoomDTO;
import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.entity.Room;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface RoomMapper {

    @Mapping(target = "hotel", ignore = true)
    Room toEntity(RoomDTO roomDTO);

    @Mapping(source = "hotel.id", target = "hotelId")
    RoomDTO toDto(Room room);

    List<RoomDTO> toDtoList(List<Room> rooms);

    List<Room> toEntityList(List<RoomDTO> roomDTOs);

    default Room toEntityWithHotel(RoomDTO roomDTO, Hotel hotel) {
        if (roomDTO == null || hotel == null) {
            return null;
        }

        Room room = toEntity(roomDTO);
        room.setHotel(hotel);
        return room;
    }

    default void updateRoomFromDto(RoomDTO roomDTO, Room room) {
        if (roomDTO == null) {
            return;
        }

        if (roomDTO.getNumber() != null) {
            room.setNumber(roomDTO.getNumber());
        }
        if (roomDTO.getAvailable() != null) {
            room.setAvailable(roomDTO.getAvailable());
        }
        if (roomDTO.getTimesBooked() != null) {
            room.setTimesBooked(roomDTO.getTimesBooked());
        }
    }
}