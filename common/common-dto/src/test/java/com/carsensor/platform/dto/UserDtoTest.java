package com.carsensor.platform.dto;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testUserDtoSerialization() throws Exception {
        // Arrange
        UserDto original = UserDto.builder()
                .id(1L)
                .username("admin")
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .isActive(true)
                .roles(List.of("ROLE_ADMIN", "ROLE_USER"))
                .build();

        // Act
        String json = mapper.writeValueAsString(original);
        UserDto deserialized = mapper.readValue(json, UserDto.class);

        // Assert
        assertEquals(original.id(), deserialized.id());
        assertEquals(original.username(), deserialized.username());
        assertEquals(original.email(), deserialized.email());
        assertEquals(original.firstName(), deserialized.firstName());
        assertEquals(original.lastName(), deserialized.lastName());
        assertEquals(original.isActive(), deserialized.isActive());
        assertEquals(original.roles(), deserialized.roles());
    }

    @Test
    void testUserDtoWithMinimalFields() {
        // Act
        UserDto user = UserDto.builder()
                .username("user")
                .email("user@example.com")
                .password("password123")
                .build();

        // Assert
        assertNotNull(user);
        assertEquals("user", user.username());
        assertEquals("user@example.com", user.email());
        assertEquals("password123", user.password());
        assertNull(user.id());
        assertNull(user.firstName());
        assertNull(user.roles());
        assertFalse(user.isActive()); // должно быть false по умолчанию
    }

    @Test
    void testPasswordIsWriteOnly() throws Exception {
        // Arrange
        UserDto original = UserDto.builder()
                .username("user")
                .email("user@example.com")
                .password("secret123")
                .build();

        // Act
        String json = mapper.writeValueAsString(original);

        // Assert - пароль не должен быть в JSON
        assertFalse(json.contains("secret123"));
        assertFalse(json.contains("password"));
    }
}