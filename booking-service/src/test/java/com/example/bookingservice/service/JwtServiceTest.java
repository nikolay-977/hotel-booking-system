package com.example.bookingservice.service;

import com.example.bookingservice.dto.UserShortDTO;
import com.example.bookingservice.entity.Role;
import com.example.bookingservice.entity.User;
import com.example.bookingservice.mapper.UserMapper;
import com.example.bookingservice.repository.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JwtService jwtService;

    private User testUser;
    private String validJwtSecret;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() throws Exception {
        // Создаем тестовый секретный ключ (минимум 32 символа для Base64)
        validJwtSecret = Base64.getEncoder().encodeToString(
                "ThisIsAValid32CharacterSecretKeyForTest".getBytes()
        );

        ReflectionTestUtils.setField(jwtService, "jwtSecret", validJwtSecret);
        ReflectionTestUtils.setField(jwtService, "expiration", 3600000L);

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("password")
                .role(Role.USER)
                .build();

        // Инициализируем секретный ключ
        byte[] keyBytes = Decoders.BASE64.decode(validJwtSecret);
        secretKey = Keys.hmacShaKeyFor(keyBytes);

        // Устанавливаем секретный ключ через рефлексию
        ReflectionTestUtils.setField(jwtService, "secretKey", secretKey);
    }

    @Test
    void generateToken_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        String token = jwtService.generateToken(1L);

        // Assert
        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3); // Проверяем формат JWT

        // Проверяем, что токен можно распарсить
        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build();

            Jws<Claims> claimsJws = parser.parseClaimsJws(token);
            Claims claims = claimsJws.getBody();

            assertEquals("testuser", claims.getSubject());
            assertEquals("USER", claims.get("role"));
            assertEquals(1L, ((Number) claims.get("userId")).longValue());
            assertNotNull(claims.getIssuedAt());
            assertNotNull(claims.getExpiration());

        } catch (Exception e) {
            fail("Generated token should be valid: " + e.getMessage());
        }

        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void generateToken_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            jwtService.generateToken(1L);
        });

        assertEquals("User not found", exception.getMessage());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void extractUser_InvalidToken_ReturnsEmpty() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        Optional<UserShortDTO> result = jwtService.extractUser(invalidToken);

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void extractUser_ExpiredToken_ReturnsEmpty() {
        // Arrange
        // Создаем просроченный токен
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .claim("role", "USER")
                .claim("userId", 1L)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 часа назад
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 час назад
                .signWith(secretKey)
                .compact();

        // Act
        Optional<UserShortDTO> result = jwtService.extractUser(expiredToken);

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository, never()).findByUsername(anyString());
    }

    @Test
    void extractUser_UserNotFound_ReturnsEmpty() {
        // Arrange
        String token = Jwts.builder()
                .setSubject("nonexistent")
                .claim("role", "USER")
                .claim("userId", 999L)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();

        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<UserShortDTO> result = jwtService.extractUser(token);

        // Assert
        assertTrue(result.isEmpty());
        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    void validateToken_ValidToken_ReturnsTrue() {
        // Arrange
        String validToken = Jwts.builder()
                .setSubject("testuser")
                .claim("role", "USER")
                .claim("userId", 1L)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();

        // Act
        boolean result = jwtService.validateToken(validToken);

        // Assert
        assertTrue(result);
    }

    @Test
    void validateToken_InvalidToken_ReturnsFalse() {
        // Arrange
        String invalidToken = "invalid.jwt.token";

        // Act
        boolean result = jwtService.validateToken(invalidToken);

        // Assert
        assertFalse(result);
    }

    @Test
    void validateToken_ExpiredToken_ReturnsFalse() {
        // Arrange
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .claim("role", "USER")
                .claim("userId", 1L)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000))
                .setExpiration(new Date(System.currentTimeMillis() - 3600000))
                .signWith(secretKey)
                .compact();

        // Act
        boolean result = jwtService.validateToken(expiredToken);

        // Assert
        assertFalse(result);
    }

    @Test
    void init_ValidSecretKey_Success() throws Exception {
        // Arrange
        String validSecret = Base64.getEncoder().encodeToString(
                "AnotherValid32CharacterSecretKeyForTests".getBytes()
        );
        ReflectionTestUtils.setField(jwtService, "jwtSecret", validSecret);

        // Act
        Method initMethod = JwtService.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(jwtService);

        // Assert
        SecretKey key = (SecretKey) ReflectionTestUtils.getField(jwtService, "secretKey");
        assertNotNull(key);
    }

    @Test
    void init_ShortSecretKey_ThrowsException() throws Exception {
        // Arrange
        String shortSecret = "short";  // Менее 32 символов
        ReflectionTestUtils.setField(jwtService, "jwtSecret", shortSecret);

        // Act
        Method initMethod = JwtService.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);

        // Assert
        InvocationTargetException exception = assertThrows(InvocationTargetException.class, () -> {
            initMethod.invoke(jwtService);
        });

        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(exception.getCause().getMessage().contains("JWT secret must be at least 32 characters"));
    }

    @Test
    void generateToken_WithAdminRole_Success() {
        // Arrange
        User adminUser = User.builder()
                .id(2L)
                .username("admin")
                .password("adminpass")
                .role(Role.ADMIN)
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));

        // Act
        String token = jwtService.generateToken(2L);

        // Assert
        assertNotNull(token);

        // Проверяем содержимое токена
        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build();

            Jws<Claims> claimsJws = parser.parseClaimsJws(token);
            Claims claims = claimsJws.getBody();

            assertEquals("admin", claims.getSubject());
            assertEquals("ADMIN", claims.get("role"));
            assertEquals(2L, ((Number) claims.get("userId")).longValue());

        } catch (Exception e) {
            fail("Generated token should be valid: " + e.getMessage());
        }

        verify(userRepository, times(1)).findById(2L);
    }

    @Test
    void generateToken_DifferentExpirationTimes() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Устанавливаем разное время истечения
        ReflectionTestUtils.setField(jwtService, "expiration", 60000L); // 1 минута

        // Act
        String token = jwtService.generateToken(1L);

        // Assert
        assertNotNull(token);

        try {
            JwtParser parser = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build();

            Jws<Claims> claimsJws = parser.parseClaimsJws(token);
            Claims claims = claimsJws.getBody();

            Date expiration = claims.getExpiration();
            Date issuedAt = claims.getIssuedAt();

            // Проверяем, что срок истечения примерно через 1 минуту
            long diff = expiration.getTime() - issuedAt.getTime();
            assertTrue(Math.abs(diff - 60000) < 1000); // Допуск 1 секунда

        } catch (Exception e) {
            fail("Generated token should be valid: " + e.getMessage());
        }
    }

    @Test
    void extractUser_MalformedToken_ReturnsEmpty() {
        // Arrange
        String malformedToken = "header.payload.signature"; // Неправильный формат

        // Act
        Optional<UserShortDTO> result = jwtService.extractUser(malformedToken);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void validateToken_WithWrongKey_ReturnsFalse() {
        // Arrange
        // Создаем токен с другим ключом
        String wrongKeySecret = Base64.getEncoder().encodeToString(
                "Different32CharacterSecretKeyForTesting".getBytes()
        );
        byte[] wrongKeyBytes = Decoders.BASE64.decode(wrongKeySecret);
        SecretKey wrongKey = Keys.hmacShaKeyFor(wrongKeyBytes);

        String tokenWithWrongKey = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(wrongKey)
                .compact();

        // Act
        boolean result = jwtService.validateToken(tokenWithWrongKey);

        // Assert
        assertFalse(result);
    }
}