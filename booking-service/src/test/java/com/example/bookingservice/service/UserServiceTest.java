package com.example.bookingservice.service;

import com.example.bookingservice.dto.UserDTO;
import com.example.bookingservice.dto.UserUpdateDTO;
import com.example.bookingservice.entity.Role;
import com.example.bookingservice.entity.User;
import com.example.bookingservice.exception.UserNotFoundException;
import com.example.bookingservice.exception.UsernameExistsException;
import com.example.bookingservice.mapper.UserMapper;
import com.example.bookingservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Добавляем ленивый режим
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserDTO testUserDTO;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        testUserDTO = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .role(Role.USER)
                .build();
    }

    @Test
    void registerUser_Success() {
        // Arrange
        UserDTO newUserDTO = UserDTO.builder()
                .username("newuser")
                .password("password123")
                .role(Role.USER)
                .build();

        User newUserEntity = User.builder()
                .username("newuser")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        User savedUser = User.builder()
                .id(2L)
                .username("newuser")
                .password("encodedPassword")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        UserDTO savedUserDTO = UserDTO.builder()
                .id(2L)
                .username("newuser")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userMapper.toEntity(newUserDTO)).thenReturn(newUserEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(savedUserDTO);

        // Act
        UserDTO result = userService.registerUser(newUserDTO);

        // Assert
        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("newuser", result.getUsername());

        verify(userRepository).existsByUsername("newuser");
        verify(passwordEncoder).encode("encodedPassword");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_UsernameExists_ThrowsException() {
        // Arrange
        UserDTO existingUserDTO = UserDTO.builder()
                .username("existinguser")
                .password("password123")
                .role(Role.USER)
                .build();

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        assertThrows(UsernameExistsException.class, () -> {
            userService.registerUser(existingUserDTO);
        });

        verify(userRepository).existsByUsername("existinguser");
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerUser_WithAdminRole_Success() {
        // Arrange
        UserDTO adminUserDTO = UserDTO.builder()
                .username("adminuser")
                .password("adminPass123")
                .role(Role.ADMIN)
                .build();

        User adminUserEntity = User.builder()
                .username("adminuser")
                .password("encodedAdminPassword")
                .role(Role.ADMIN)
                .build();

        User savedAdminUser = User.builder()
                .id(3L)
                .username("adminuser")
                .password("encodedAdminPassword")
                .role(Role.ADMIN)
                .createdAt(LocalDateTime.now())
                .build();

        UserDTO savedAdminDTO = UserDTO.builder()
                .id(3L)
                .username("adminuser")
                .password("encodedAdminPassword")
                .role(Role.ADMIN)
                .build();

        when(userRepository.existsByUsername("adminuser")).thenReturn(false);
        when(userMapper.toEntity(adminUserDTO)).thenReturn(adminUserEntity);
        when(passwordEncoder.encode("adminPass123")).thenReturn("encodedAdminPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedAdminUser);
        when(userMapper.toDto(savedAdminUser)).thenReturn(savedAdminDTO);

        // Act
        UserDTO result = userService.registerUser(adminUserDTO);

        // Assert
        assertNotNull(result);
        assertEquals(Role.ADMIN, result.getRole());
        verify(userRepository).existsByUsername("adminuser");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void deleteUser_Success() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);
        doNothing().when(userRepository).deleteById(1L);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).existsById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.deleteUser(999L);
        });

        verify(userRepository).existsById(999L);
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void updateUser_UpdateUsername_Success() {
        // Arrange
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("updateduser")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsernameAndIdNot("updateduser", 1L)).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDTO);

        // Act
        UserDTO result = userService.updateUser(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(userRepository).save(testUser);
        assertEquals("updateduser", testUser.getUsername());
    }

    @Test
    void updateUser_UpdatePassword_Success() {
        // Arrange
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .password("newPassword123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDTO);

        // Act
        UserDTO result = userService.updateUser(1L, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("newEncodedPassword", testUser.getPassword());
    }

    @Test
    void updateUser_UpdateBothUsernameAndPassword_Success() {
        // Arrange
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("updateduser")
                .password("newPassword123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsernameAndIdNot("updateduser", 1L)).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDTO);

        // Act
        UserDTO result = userService.updateUser(1L, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("updateduser", testUser.getUsername());
        assertEquals("newEncodedPassword", testUser.getPassword());
    }

    @Test
    void updateUser_UserNotFound_ThrowsException() {
        // Arrange
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("updateduser")
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> {
            userService.updateUser(999L, updateDTO);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_UsernameTaken_ThrowsException() {
        // Arrange
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("takenusername")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsernameAndIdNot("takenusername", 1L)).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUser(1L, updateDTO);
        });

        assertEquals("Username is already taken", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_EmptyUsername_NoUpdate() {
        // Arrange
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("")
                .password("newPassword123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDTO);

        // Act
        UserDTO result = userService.updateUser(1L, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", testUser.getUsername()); // Имя не должно измениться
        assertEquals("newEncodedPassword", testUser.getPassword());
        verify(userRepository, never()).existsByUsernameAndIdNot(anyString(), anyLong());
    }

    @Test
    void updateUser_NullUsername_NoUpdate() {
        // Arrange
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username(null)
                .password("newPassword123")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDTO);

        // Act
        UserDTO result = userService.updateUser(1L, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("testuser", testUser.getUsername()); // Имя не должно измениться
        assertEquals("newEncodedPassword", testUser.getPassword());
        verify(userRepository, never()).existsByUsernameAndIdNot(anyString(), anyLong());
    }

    @Test
    void updateUser_EmptyPassword_NoUpdate() {
        // Arrange
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("updateduser")
                .password("")
                .build();

        String originalPassword = testUser.getPassword();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsernameAndIdNot("updateduser", 1L)).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDTO);

        // Act
        UserDTO result = userService.updateUser(1L, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("updateduser", testUser.getUsername());
        assertEquals(originalPassword, testUser.getPassword()); // Пароль не должен измениться
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void updateUser_NullPassword_NoUpdate() {
        // Arrange
        UserUpdateDTO updateDTO = UserUpdateDTO.builder()
                .username("updateduser")
                .password(null)
                .build();

        String originalPassword = testUser.getPassword();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsernameAndIdNot("updateduser", 1L)).thenReturn(false);
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDTO);

        // Act
        UserDTO result = userService.updateUser(1L, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("updateduser", testUser.getUsername());
        assertEquals(originalPassword, testUser.getPassword()); // Пароль не должен измениться
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void updateUser_NoChanges_StillSaves() {
        // Arrange
        UserUpdateDTO updateDTO = UserUpdateDTO.builder().build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(testUser)).thenReturn(testUser);
        when(userMapper.toDto(testUser)).thenReturn(testUserDTO);

        // Act
        UserDTO result = userService.updateUser(1L, updateDTO);

        // Assert
        assertNotNull(result);
        verify(userRepository).save(testUser);
    }

    @Test
    void registerUser_WithSpecialCharactersInUsername_Success() {
        // Arrange
        UserDTO specialUserDTO = UserDTO.builder()
                .username("user.name_123")
                .password("password123")
                .role(Role.USER)
                .build();

        User specialUserEntity = User.builder()
                .username("user.name_123")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        User savedUser = User.builder()
                .id(4L)
                .username("user.name_123")
                .password("encodedPassword")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        UserDTO savedUserDTO = UserDTO.builder()
                .id(4L)
                .username("user.name_123")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.existsByUsername("user.name_123")).thenReturn(false);
        when(userMapper.toEntity(specialUserDTO)).thenReturn(specialUserEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(savedUserDTO);

        // Act
        UserDTO result = userService.registerUser(specialUserDTO);

        // Assert
        assertNotNull(result);
        assertEquals("user.name_123", result.getUsername());
        verify(userRepository).existsByUsername("user.name_123");
    }

    @Test
    void registerUser_LongUsername_Success() {
        // Arrange
        String longUsername = "a".repeat(50); // Максимальная длина
        UserDTO longUserDTO = UserDTO.builder()
                .username(longUsername)
                .password("password123")
                .role(Role.USER)
                .build();

        User longUserEntity = User.builder()
                .username(longUsername)
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        User savedUser = User.builder()
                .id(5L)
                .username(longUsername)
                .password("encodedPassword")
                .role(Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        UserDTO savedUserDTO = UserDTO.builder()
                .id(5L)
                .username(longUsername)
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.existsByUsername(longUsername)).thenReturn(false);
        when(userMapper.toEntity(longUserDTO)).thenReturn(longUserEntity);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userMapper.toDto(savedUser)).thenReturn(savedUserDTO);

        // Act
        UserDTO result = userService.registerUser(longUserDTO);

        // Assert
        assertNotNull(result);
        assertEquals(longUsername, result.getUsername());
        verify(userRepository).existsByUsername(longUsername);
    }
}