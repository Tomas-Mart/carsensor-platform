package com.carsensor.platform.dto;

import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ErrorResponseTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testErrorResponseSerialization() throws Exception {
        // Arrange
        ErrorResponse original = ErrorResponse.builder()
                .timestamp(Instant.now().toString())
                .status(400)
                .error("Bad Request")
                .message("Invalid input")
                .path("/api/v1/cars")
                .errorCode("VALIDATION_FAILED")
                .fieldErrors(Map.of("brand", "Марка не может быть пустой"))
                .build();

        // Act
        String json = mapper.writeValueAsString(original);
        ErrorResponse deserialized = mapper.readValue(json, ErrorResponse.class);

        // Assert
        assertEquals(original.status(), deserialized.status());
        assertEquals(original.error(), deserialized.error());
        assertEquals(original.message(), deserialized.message());
        assertEquals(original.path(), deserialized.path());
        assertEquals(original.errorCode(), deserialized.errorCode());
        assertEquals(original.fieldErrors(), deserialized.fieldErrors());
    }

    @Test
    void testErrorResponseCompactConstructor() {
        // Act
        ErrorResponse response = new ErrorResponse(
                null,
                401,
                "Unauthorized",
                "Invalid credentials",
                "/api/v1/auth/login",
                null,
                null
        );

        // Assert
        assertEquals(401, response.status());
        assertEquals("Unauthorized", response.error());
        assertEquals("Invalid credentials", response.message());
        assertEquals("/api/v1/auth/login", response.path());
        assertNull(response.errorCode());
        assertNull(response.fieldErrors());
        assertNotNull(response.timestamp()); // должен быть сгенерирован
    }
}