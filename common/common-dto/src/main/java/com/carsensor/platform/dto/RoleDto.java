package com.carsensor.platform.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * DTO для роли
 */
@Builder
public record RoleDto(
        Long id,

        @NotBlank(message = "Название роли обязательно")
        @Size(min = 3, max = 50, message = "Название роли должно содержать от 3 до 50 символов")
        String name,

        @Size(max = 200, message = "Описание не должно превышать 200 символов")
        String description,

        @JsonProperty("permissions")
        List<String> permissions
) {
}