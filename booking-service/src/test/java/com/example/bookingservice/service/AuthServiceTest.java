package com.example.bookingservice.service;

import com.example.bookingservice.dto.AuthRequestDTO;
import com.example.bookingservice.dto.AuthResponseDTO;
import com.example.bookingservice.dto.UserDTO;
import com.example.bookingservice.dto.UserShortDTO;
import com.example.bookingservice.entity.Role;
import com.example.bookingservice.entity.User;
import com.example.bookingservice.exception.UserNotFoundException;
import com.example.bookingservice.mapper.UserMapper;
import com.example.bookingservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private UserService userService; // Мокаем UserService

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private AuthRequestDTO authRequest;
    private UserDTO userDTO;

    @BeforeEach
    void setUp() {
        // Подготовка тестовых данных
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        authRequest = AuthRequestDTO.builder()
                .username("testuser")
                .password("password123")
                .build();

        userDTO = UserDTO.builder()
                .id(1L)
                .username("newuser")
                .password("password123")
                .role(Role.USER)
                .build();
    }

    @Test
    void authenticate_Success() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(1L)).thenReturn("test.jwt.token");

        UserShortDTO userShortDTO = UserShortDTO.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .build();
        when(userMapper.toShortDto(testUser)).thenReturn(userShortDTO);

        // Act
        AuthResponseDTO response = authService.authenticate(authRequest);

        // Assert
        assertNotNull(response);
        assertEquals("test.jwt.token", response.getToken());
        assertEquals(userShortDTO, response.getUser());

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("password123", "encodedPassword");
        verify(jwtService, times(1)).generateToken(1L);
        verify(userMapper, times(1)).toShortDto(testUser);
    }

    @Test
    void authenticate_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            authService.authenticate(authRequest);
        });

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(jwtService, never()).generateToken(anyLong());
        verify(userMapper, never()).toShortDto(any());
    }

    @Test
    void authenticate_WrongPassword_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(false);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            authService.authenticate(authRequest);
        });

        verify(userRepository, times(1)).findByUsername("testuser");
        verify(passwordEncoder, times(1)).matches("password123", "encodedPassword");
        verify(jwtService, never()).generateToken(anyLong());
        verify(userMapper, never()).toShortDto(any());
    }

    @Test
    void authenticate_NullRequest_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            authService.authenticate(null);
        });
    }

    @Test
    void authenticate_EmptyUsername_ThrowsException() {
        // Arrange
        AuthRequestDTO emptyRequest = AuthRequestDTO.builder()
                .username("")
                .password("password123")
                .build();
        when(userRepository.findByUsername("")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            authService.authenticate(emptyRequest);
        });

        verify(userRepository, times(1)).findByUsername("");
    }

    @Test
    void register_Success() {
        // Arrange
        UserDTO registeredUserDTO = UserDTO.builder()
                .id(1L)
                .username("newuser")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        UserShortDTO userShortDTO = UserShortDTO.builder()
                .id(1L)
                .username("newuser")
                .role(Role.USER)
                .build();

        when(userService.registerUser(userDTO)).thenReturn(registeredUserDTO);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toShortDto(testUser)).thenReturn(userShortDTO);
        when(jwtService.generateToken(1L)).thenReturn("test.jwt.token");

        // Act
        AuthResponseDTO response = authService.register(userDTO);

        // Assert
        assertNotNull(response);
        assertEquals("test.jwt.token", response.getToken());
        assertEquals(userShortDTO, response.getUser());

        verify(userService, times(1)).registerUser(userDTO);
        verify(userRepository, times(1)).findById(1L);
        verify(jwtService, times(1)).generateToken(1L);
        verify(userMapper, times(1)).toShortDto(testUser);
    }

    @Test
    void register_UserNotFoundAfterRegistration_ThrowsException() {
        // Arrange
        UserDTO registeredUserDTO = UserDTO.builder()
                .id(1L)
                .username("newuser")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userService.registerUser(userDTO)).thenReturn(registeredUserDTO);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            authService.register(userDTO);
        });

        verify(userService, times(1)).registerUser(userDTO);
        verify(userRepository, times(1)).findById(1L);
        verify(jwtService, never()).generateToken(anyLong());
        verify(userMapper, never()).toShortDto(any());
    }

    @Test
    void register_NullUserDTO_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            authService.register(null);
        });
    }

    @Test
    void register_WithAdminRole_Success() {
        // Arrange
        UserDTO adminUserDTO = UserDTO.builder()
                .username("adminuser")
                .password("adminPass123")
                .role(Role.ADMIN)
                .build();

        User adminUserEntity = User.builder()
                .id(2L)
                .username("adminuser")
                .password("encodedAdminPassword")
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build();

        UserDTO registeredAdminDTO = UserDTO.builder()
                .id(2L)
                .username("adminuser")
                .password("encodedAdminPassword")
                .role(Role.ADMIN)
                .build();

        UserShortDTO adminShortDTO = UserShortDTO.builder()
                .id(2L)
                .username("adminuser")
                .role(Role.ADMIN)
                .build();

        when(userService.registerUser(adminUserDTO)).thenReturn(registeredAdminDTO);
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUserEntity));
        when(userMapper.toShortDto(adminUserEntity)).thenReturn(adminShortDTO);
        when(jwtService.generateToken(2L)).thenReturn("admin.jwt.token");

        // Act
        AuthResponseDTO response = authService.register(adminUserDTO);

        // Assert
        assertNotNull(response);
        assertEquals("admin.jwt.token", response.getToken());
        assertEquals(adminShortDTO, response.getUser());
        assertEquals(Role.ADMIN, response.getUser().getRole());

        verify(userService, times(1)).registerUser(adminUserDTO);
        verify(userRepository, times(1)).findById(2L);
        verify(jwtService, times(1)).generateToken(2L);
        verify(userMapper, times(1)).toShortDto(adminUserEntity);
    }

    @Test
    void authenticate_MultipleCalls_CallsRepositoryEachTime() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken(1L)).thenReturn("token1", "token2");

        UserShortDTO userShortDTO = UserShortDTO.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .build();
        when(userMapper.toShortDto(testUser)).thenReturn(userShortDTO);

        // Act
        AuthResponseDTO response1 = authService.authenticate(authRequest);
        AuthResponseDTO response2 = authService.authenticate(authRequest);

        // Assert
        assertEquals("token1", response1.getToken());
        assertEquals("token2", response2.getToken());

        verify(userRepository, times(2)).findByUsername("testuser");
        verify(passwordEncoder, times(2)).matches("password123", "encodedPassword");
        verify(jwtService, times(2)).generateToken(1L);
        verify(userMapper, times(2)).toShortDto(testUser);
    }

    @Test
    void register_EmptyPassword_ShouldBeHandledByUserService() {
        // Arrange
        UserDTO userWithEmptyPassword = UserDTO.builder()
                .username("testuser")
                .password("")
                .role(Role.USER)
                .build();

        UserDTO registeredUser = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .password("encodedEmpty")
                .role(Role.USER)
                .build();

        when(userService.registerUser(userWithEmptyPassword)).thenReturn(registeredUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userMapper.toShortDto(testUser)).thenReturn(UserShortDTO.builder()
                .id(1L)
                .username("testuser")
                .role(Role.USER)
                .build());
        when(jwtService.generateToken(1L)).thenReturn("token");

        // Act
        AuthResponseDTO response = authService.register(userWithEmptyPassword);

        // Assert
        assertNotNull(response);
        verify(userService, times(1)).registerUser(userWithEmptyPassword);
    }

    @Test
    void register_ExistingUsername_ShouldBeHandledByUserService() {
        // Arrange
        UserDTO userWithExistingUsername = UserDTO.builder()
                .username("existinguser")
                .password("password123")
                .role(Role.USER)
                .build();

        // UserService должен бросить UsernameExistsException
        when(userService.registerUser(userWithExistingUsername))
                .thenThrow(new com.example.bookingservice.exception.UsernameExistsException());

        // Act & Assert
        assertThrows(com.example.bookingservice.exception.UsernameExistsException.class, () -> {
            authService.register(userWithExistingUsername);
        });

        verify(userService, times(1)).registerUser(userWithExistingUsername);
        verify(userRepository, never()).findById(any());
        verify(jwtService, never()).generateToken(anyLong());
    }
}