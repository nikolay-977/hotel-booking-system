package com.example.hotelservice.config;

import com.example.hotelservice.entity.Hotel;
import com.example.hotelservice.entity.Room;
import com.example.hotelservice.repository.HotelRepository;
import com.example.hotelservice.repository.RoomRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting data initialization...");

        roomRepository.deleteAll();
        hotelRepository.deleteAll();

        Hotel grandHotel = createHotel("Grand Hotel", "123 Main Street, City Center");
        Hotel plazaHotel = createHotel("Plaza Hotel", "456 Oak Avenue, Downtown");
        Hotel seasideResort = createHotel("Seaside Resort", "789 Beach Boulevard, Coastline");

        List<Hotel> hotels = Arrays.asList(grandHotel, plazaHotel, seasideResort);
        hotelRepository.saveAll(hotels);

        List<Room> grandHotelRooms = Arrays.asList(
                createRoom("101", true, 5, grandHotel),
                createRoom("102", true, 3, grandHotel),
                createRoom("103", false, 8, grandHotel),
                createRoom("201", true, 2, grandHotel),
                createRoom("202", true, 7, grandHotel),
                createRoom("301", true, 1, grandHotel)
        );

        List<Room> plazaHotelRooms = Arrays.asList(
                createRoom("101", true, 4, plazaHotel),
                createRoom("102", true, 6, plazaHotel),
                createRoom("103", true, 2, plazaHotel),
                createRoom("201", false, 9, plazaHotel),
                createRoom("202", true, 3, plazaHotel),
                createRoom("301", true, 1, plazaHotel)
        );

        List<Room> seasideResortRooms = Arrays.asList(
                createRoom("101", true, 0, seasideResort),
                createRoom("102", true, 2, seasideResort),
                createRoom("103", true, 5, seasideResort),
                createRoom("201", true, 3, seasideResort),
                createRoom("202", false, 7, seasideResort),
                createRoom("301", true, 1, seasideResort)
        );

        roomRepository.saveAll(grandHotelRooms);
        roomRepository.saveAll(plazaHotelRooms);
        roomRepository.saveAll(seasideResortRooms);

        log.info("Data initialization completed successfully!");
        log.info("Created {} hotels and {} rooms",
                hotelRepository.count(),
                roomRepository.count());

        logAvailableRooms();
    }

    private Hotel createHotel(String name, String address) {
        Hotel hotel = new Hotel();
        hotel.setName(name);
        hotel.setAddress(address);
        return hotel;
    }

    private Room createRoom(String number, boolean available, int timesBooked, Hotel hotel) {
        Room room = new Room();
        room.setNumber(number);
        room.setAvailable(available);
        room.setTimesBooked(timesBooked);
        room.setHotel(hotel);
        return room;
    }

    @Transactional
    private void logAvailableRooms() {
        List<Room> availableRooms = roomRepository.findAvailableRoomsWithHotel();
        log.info("Available rooms ordered by popularity (timesBooked ASC):");
        availableRooms.forEach(room ->
                log.info("ID:{} - Room {} in Hotel {} - Times Booked: {}",
                        room.getId(),
                        room.getNumber(),
                        room.getHotel().getName(),
                        room.getTimesBooked())
        );
    }
}