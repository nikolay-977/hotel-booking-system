package com.example.bookingservice.service;

import com.example.bookingservice.dto.AuthRequestDTO;
import com.example.bookingservice.dto.AuthResponseDTO;
import com.example.bookingservice.dto.UserDTO;
import com.example.bookingservice.dto.UserShortDTO;
import com.example.bookingservice.entity.User;
import com.example.bookingservice.exception.UserNotFoundException;
import com.example.bookingservice.mapper.UserMapper;
import com.example.bookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public AuthResponseDTO authenticate(AuthRequestDTO request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty() || !passwordEncoder.matches(request.getPassword(), userOpt.get().getPassword())) {
            throw new UserNotFoundException();
        }

        User user = userOpt.get();
        String token = jwtService.generateToken(user.getId());
        UserShortDTO userShortDTO = userMapper.toShortDto(user);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(token);
        response.setUser(userShortDTO);

        return response;
    }

    public AuthResponseDTO register(UserDTO userDTO) {
        UserService userService = new UserService(userRepository, userMapper, passwordEncoder);
        UserDTO registeredUser = userService.registerUser(userDTO);

        User user = userRepository.findById(registeredUser.getId())
                .orElseThrow(UserNotFoundException::new);

        String token = jwtService.generateToken(user.getId());
        UserShortDTO userShortDTO = userMapper.toShortDto(user);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(token);
        response.setUser(userShortDTO);

        return response;
    }
}