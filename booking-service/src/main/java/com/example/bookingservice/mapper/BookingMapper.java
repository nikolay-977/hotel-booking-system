package com.example.bookingservice.mapper;

import com.example.bookingservice.dto.BookingDTO;
import com.example.bookingservice.entity.Booking;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    BookingMapper MAPPER = org.mapstruct.factory.Mappers.getMapper(BookingMapper.class);

    @Mapping(source = "user.id", target = "userId")
    BookingDTO toDto(Booking booking);

    @Mapping(source = "userId", target = "user.id")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "correlationId", ignore = true)
    Booking toEntity(BookingDTO bookingDTO);

    List<BookingDTO> toDtoList(List<Booking> bookings);

    default void updateBookingFromDto(BookingDTO bookingDTO, Booking booking) {
        if (bookingDTO == null) {
            return;
        }

        if (bookingDTO.getRoomId() != null) {
            booking.setRoomId(bookingDTO.getRoomId());
        }
        if (bookingDTO.getStartDate() != null) {
            booking.setStartDate(bookingDTO.getStartDate());
        }
        if (bookingDTO.getEndDate() != null) {
            booking.setEndDate(bookingDTO.getEndDate());
        }
        if (bookingDTO.getStatus() != null) {
            booking.setStatus(bookingDTO.getStatus());
        }
    }
}