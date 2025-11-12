package com.example.bookingservice.service;

import com.example.bookingservice.dto.UserDTO;
import com.example.bookingservice.dto.UserUpdateDTO;
import com.example.bookingservice.entity.User;
import com.example.bookingservice.exception.UserNotFoundException;
import com.example.bookingservice.exception.UsernameExistsException;
import com.example.bookingservice.mapper.UserMapper;
import com.example.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserDTO registerUser(UserDTO userDTO) {
        log.info("Register user...");

        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new UsernameExistsException();
        }
        User user = userMapper.toEntity(userDTO);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        User registeredUser = userRepository.save(user);
        log.info("User registered with ID: {}", registeredUser.getId());
        return userMapper.toDto(registeredUser);
    }

    public void deleteUser(Long id) {
        log.info("Deleting user with ID: {}", id);

        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with ID: " + id);
        }

        userRepository.deleteById(id);
        log.info("User deleted successfully with ID: {}", id);
    }

    public UserDTO updateUser(Long id, UserUpdateDTO updateDTO) {
        log.info("Updating user with ID: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        // Обновляем только переданные поля
        if (updateDTO.getUsername() != null && !updateDTO.getUsername().isBlank()) {
            // Проверяем, не занят ли username другим пользователем
            if (userRepository.existsByUsernameAndIdNot(updateDTO.getUsername(), id)) {
                throw new RuntimeException("Username is already taken");
            }
            user.setUsername(updateDTO.getUsername());
        }

        if (updateDTO.getPassword() != null && !updateDTO.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updateDTO.getPassword()));
        }

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully with ID: {}", id);

        return userMapper.toDto(updatedUser);
    }
}