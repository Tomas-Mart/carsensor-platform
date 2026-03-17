package com.carsensor.platform.dto;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRequestTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    private final Validator validator = factory.getValidator();

    @Test
    void testLoginRequestSerialization() throws Exception {
        // Arrange
        LoginRequest original = new LoginRequest("admin", "admin123");

        // Act
        String json = mapper.writeValueAsString(original);
        LoginRequest deserialized = mapper.readValue(json, LoginRequest.class);

        // Assert
        assertEquals(original.username(), deserialized.username());
        assertEquals(original.password(), deserialized.password());
    }

    @Test
    void testLoginRequestValidation() {
        // Act & Assert - валидные данные
        LoginRequest valid = new LoginRequest("admin", "admin123");
        assertTrue(validator.validate(valid).isEmpty());

        // Act & Assert - невалидные данные
        LoginRequest invalid = new LoginRequest("", "");
        assertFalse(validator.validate(invalid).isEmpty());
    }
}