package com.example.bookingservice.mapper;

import com.example.bookingservice.dto.UserDTO;
import com.example.bookingservice.dto.UserShortDTO;
import com.example.bookingservice.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserMapper MAPPER = org.mapstruct.factory.Mappers.getMapper(UserMapper.class);

    @Mapping(target = "createdAt", ignore = true)
    User toEntity(UserDTO userDTO);

    UserDTO toDto(User user);

    UserShortDTO toShortDto(User user);

    List<UserDTO> toDtoList(List<User> users);

    default void updateUserFromDto(UserDTO userDTO, User user) {
        if (userDTO == null) {
            return;
        }

        if (userDTO.getUsername() != null) {
            user.setUsername(userDTO.getUsername());
        }
        if (userDTO.getPassword() != null) {
            user.setPassword(userDTO.getPassword());
        }
        if (userDTO.getRole() != null) {
            user.setRole(userDTO.getRole());
        }
    }
}