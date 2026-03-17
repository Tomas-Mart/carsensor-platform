package com.carsensor.platform.dto;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RoleDtoTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void testRoleDtoSerialization() throws Exception {
        // Arrange
        RoleDto original = RoleDto.builder()
                .id(1L)
                .name("ROLE_ADMIN")
                .description("Administrator")
                .permissions(List.of("CAR_VIEW", "CAR_EDIT", "USER_MANAGE"))
                .build();

        // Act
        String json = mapper.writeValueAsString(original);
        RoleDto deserialized = mapper.readValue(json, RoleDto.class);

        // Assert
        assertEquals(original.id(), deserialized.id());
        assertEquals(original.name(), deserialized.name());
        assertEquals(original.description(), deserialized.description());
        assertEquals(original.permissions(), deserialized.permissions());
    }

    @Test
    void testRoleDtoWithoutPermissions() {
        // Act
        RoleDto role = RoleDto.builder()
                .id(1L)
                .name("ROLE_USER")
                .build();

        // Assert
        assertNotNull(role);
        assertEquals("ROLE_USER", role.name());
        assertNull(role.permissions());
        assertNull(role.description());
    }
}