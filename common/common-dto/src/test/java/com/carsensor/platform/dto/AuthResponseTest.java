package com.carsensor.platform.dto;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuthResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testAuthResponseSerialization() throws Exception {
        // Arrange
        AuthResponse original = new AuthResponse(
                "eyJhbGciOiJIUzI1NiIs...",
                "eyJhbGciOiJIUzI1NiIs...",
                AuthResponse.TOKEN_TYPE,
                900,
                "admin",
                new String[]{"ROLE_ADMIN", "ROLE_USER"}
        );

        // Act
        String json = mapper.writeValueAsString(original);
        AuthResponse deserialized = mapper.readValue(json, AuthResponse.class);

        // Assert
        assertEquals(original.accessToken(), deserialized.accessToken());
        assertEquals(original.refreshToken(), deserialized.refreshToken());
        assertEquals(original.tokenType(), deserialized.tokenType());
        assertEquals(original.expiresIn(), deserialized.expiresIn());
        assertEquals(original.username(), deserialized.username());
        assertArrayEquals(original.roles(), deserialized.roles());
    }

    @Test
    void testAuthResponseBuilder() {
        // Act
        AuthResponse response = AuthResponse.builder()
                .accessToken("token")
                .tokenType(AuthResponse.TOKEN_TYPE)
                .expiresIn(900)
                .username("admin")
                .roles(new String[]{"ROLE_ADMIN"})
                .build();

        // Assert
        assertNotNull(response);
        assertEquals("token", response.accessToken());
        assertEquals(AuthResponse.TOKEN_TYPE, response.tokenType());
        assertEquals(900, response.expiresIn());
        assertEquals("admin", response.username());
        assertArrayEquals(new String[]{"ROLE_ADMIN"}, response.roles());
        assertNull(response.refreshToken());
    }

    @Test
    void testAuthResponseCompactConstructor() {
        // Act
        AuthResponse response = new AuthResponse(
                "token",
                null,
                null,
                900,
                "admin",
                null
        );

        // Assert
        assertEquals("token", response.accessToken());
        assertEquals(AuthResponse.TOKEN_TYPE, response.tokenType());
        assertNull(response.refreshToken());
        assertNull(response.roles());
    }
}