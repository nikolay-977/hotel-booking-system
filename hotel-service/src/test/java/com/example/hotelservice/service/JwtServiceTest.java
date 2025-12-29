package com.example.hotelservice.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private String validSecret;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        // Generate a valid 32-character secret key
        validSecret = generateRandomBase64Secret();
        jwtService = new JwtService(validSecret);

        // Generate the same key for token creation
        byte[] keyBytes = Base64.getDecoder().decode(validSecret);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    void constructor_WithValidSecret_ShouldInitialize() {
        // Act & Assert
        assertNotNull(jwtService);
    }

    @Test
    void constructor_WithShortSecret_ShouldThrowException() {
        // Arrange
        String shortSecret = "short-secret"; // Less than 32 characters

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new JwtService(shortSecret));

        assertTrue(exception.getMessage().contains("JWT secret must be at least 32 characters"));
    }

    @Test
    void validateToken_WithValidToken_ShouldReturnAuthentication() {
        // Arrange
        String token = createValidJwtToken("testuser", "USER");

        // Act
        Authentication authentication = jwtService.validateToken(token);

        // Assert
        assertNotNull(authentication);
        assertEquals("testuser", authentication.getName());

        List<? extends GrantedAuthority> authorities =
                List.copyOf(authentication.getAuthorities());
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.get(0).getAuthority());
        assertNull(authentication.getCredentials()); // No password in JWT auth
    }

    @Test
    void validateToken_WithAdminRole_ShouldReturnAdminAuthority() {
        // Arrange
        String token = createValidJwtToken("admin", "ADMIN");

        // Act
        Authentication authentication = jwtService.validateToken(token);

        // Assert
        assertNotNull(authentication);
        assertEquals("admin", authentication.getName());

        List<? extends GrantedAuthority> authorities =
                List.copyOf(authentication.getAuthorities());
        assertEquals(1, authorities.size());
        assertEquals("ROLE_ADMIN", authorities.get(0).getAuthority());
    }

    @Test
    void validateToken_WithMissingRole_ShouldReturnNull() {
        // Arrange
        String token = createJwtTokenWithoutRole("testuser");

        // Act
        Authentication authentication = jwtService.validateToken(token);

        // Assert
        assertNull(authentication);
    }

    @Test
    void validateToken_WithMissingSubject_ShouldReturnNull() {
        // Arrange
        String token = createJwtTokenWithoutSubject();

        // Act
        Authentication authentication = jwtService.validateToken(token);

        // Assert
        assertNull(authentication);
    }

    @Test
    void validateToken_WithInvalidToken_ShouldReturnNull() {
        // Arrange
        String invalidToken = "invalid.token.string";

        // Act
        Authentication authentication = jwtService.validateToken(invalidToken);

        // Assert
        assertNull(authentication);
    }

    @Test
    void validateToken_WithExpiredToken_ShouldReturnNull() {
        // Arrange
        String expiredToken = createExpiredJwtToken("testuser", "USER");

        // Act
        Authentication authentication = jwtService.validateToken(expiredToken);

        // Assert
        assertNull(authentication);
    }

    @Test
    void validateToken_WithWrongSignature_ShouldReturnNull() {
        // Arrange
        String token = createValidJwtToken("testuser", "USER");

        // Create JwtService with different secret
        String differentSecret = generateRandomBase64Secret();
        JwtService differentJwtService = new JwtService(differentSecret);

        // Act
        Authentication authentication = differentJwtService.validateToken(token);

        // Assert
        assertNull(authentication);
    }

    @Test
    void validateToken_WithEmptyToken_ShouldReturnNull() {
        // Act
        Authentication authentication = jwtService.validateToken("");

        // Assert
        assertNull(authentication);
    }

    @Test
    void validateToken_WithNullToken_ShouldReturnNull() {
        // Act
        Authentication authentication = jwtService.validateToken(null);

        // Assert
        assertNull(authentication);
    }

    @Test
    void validateToken_WithMalformedToken_ShouldReturnNull() {
        // Arrange
        String malformedToken = "header.payload"; // Missing signature

        // Act
        Authentication authentication = jwtService.validateToken(malformedToken);

        // Assert
        assertNull(authentication);
    }

    private String generateRandomBase64Secret() {
        // Generate 32 random bytes and encode as base64
        byte[] bytes = new byte[32];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (Math.random() * 256);
        }
        return Base64.getEncoder().encodeToString(bytes);
    }

    private String createValidJwtToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(secretKey)
                .compact();
    }

    private String createJwtTokenWithoutRole(String username) {
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();
    }

    private String createJwtTokenWithoutSubject() {
        return Jwts.builder()
                .claim("role", "USER")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(secretKey)
                .compact();
    }

    private String createExpiredJwtToken(String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(secretKey)
                .compact();
    }
}