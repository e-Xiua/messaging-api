package com.iwellness.messaging.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests para JwtUtil")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private String secretKey = "testSecretKeyForJwtTokenGenerationAndValidation1234567890";
    private String validToken;
    private String expiredToken;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // Configurar la clave secreta usando reflexión o manualmente
        // Para este test, crearemos tokens de prueba directamente
        
        // Token válido
        validToken = Jwts.builder()
                .setSubject("testuser")
                .claim("providerId", 100L)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10)) // 10 horas
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        // Token expirado
        expiredToken = Jwts.builder()
                .setSubject("expireduser")
                .claim("providerId", 200L)
                .setIssuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24)) // Hace 24 horas
                .setExpiration(new Date(System.currentTimeMillis() - 1000 * 60 * 60)) // Expirado hace 1 hora
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }

    @Test
    @DisplayName("extractUsername - Extraer nombre de usuario del token")
    void testExtractUsername() {
        // When
        String username = jwtUtil.extractUsername(validToken);

        // Then
        assertNotNull(username);
        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("extractProviderId - Extraer providerId del token")
    void testExtractProviderId() {
        // When
        Long providerId = jwtUtil.extractProviderId(validToken);

        // Then
        assertNotNull(providerId);
        assertEquals(100L, providerId);
    }

    @Test
    @DisplayName("extractExpiration - Extraer fecha de expiración")
    void testExtractExpiration() {
        // When
        Date expiration = jwtUtil.extractExpiration(validToken);

        // Then
        assertNotNull(expiration);
        assertTrue(expiration.after(new Date()));
    }

    @Test
    @DisplayName("validateToken - Token válido")
    void testValidateToken_Valid() {
        // When
        Boolean isValid = jwtUtil.validateToken(validToken);

        // Then
        assertTrue(isValid);
    }

    @Test
    @DisplayName("validateToken - Token expirado")
    void testValidateToken_Expired() {
        // When & Then
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken(expiredToken);
        });
    }

    @Test
    @DisplayName("validateToken - Token nulo")
    void testValidateToken_Null() {
        // When & Then
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken(null);
        });
    }

    @Test
    @DisplayName("validateToken - Token inválido (formato incorrecto)")
    void testValidateToken_InvalidFormat() {
        // Given
        String invalidToken = "invalid.token.format";

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken(invalidToken);
        });
    }

    @Test
    @DisplayName("extractClaim - Extraer claim personalizado")
    void testExtractClaim_CustomClaim() {
        // When
        Long providerId = jwtUtil.extractClaim(validToken, claims -> claims.get("providerId", Long.class));

        // Then
        assertNotNull(providerId);
        assertEquals(100L, providerId);
    }

    @Test
    @DisplayName("extractClaim - Extraer subject")
    void testExtractClaim_Subject() {
        // When
        String subject = jwtUtil.extractClaim(validToken, Claims::getSubject);

        // Then
        assertNotNull(subject);
        assertEquals("testuser", subject);
    }

    @Test
    @DisplayName("validateToken - Token vacío")
    void testValidateToken_EmptyString() {
        // When & Then
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken("");
        });
    }

    @Test
    @DisplayName("extractUsername - Token con diferentes usuarios")
    void testExtractUsername_DifferentUsers() {
        // Given
        String token1 = Jwts.builder()
                .setSubject("user1")
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        String token2 = Jwts.builder()
                .setSubject("user2")
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        // When
        String username1 = jwtUtil.extractUsername(token1);
        String username2 = jwtUtil.extractUsername(token2);

        // Then
        assertEquals("user1", username1);
        assertEquals("user2", username2);
        assertNotEquals(username1, username2);
    }

    @Test
    @DisplayName("extractProviderId - Token sin providerId claim")
    void testExtractProviderId_MissingClaim() {
        // Given
        String tokenWithoutProviderId = Jwts.builder()
                .setSubject("testuser")
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();

        // When
        Long providerId = jwtUtil.extractProviderId(tokenWithoutProviderId);

        // Then
        assertNull(providerId);
    }

    @Test
    @DisplayName("validateToken - Token con firma incorrecta")
    void testValidateToken_InvalidSignature() {
        // Given
        String tokenWithDifferentKey = Jwts.builder()
                .setSubject("testuser")
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(SignatureAlgorithm.HS256, "differentSecretKey123456789012345678901234567890")
                .compact();

        // When & Then
        assertThrows(Exception.class, () -> {
            jwtUtil.validateToken(tokenWithDifferentKey);
        });
    }
}
