package com.carsensor.platform.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * DTO для роли
 */
@Builder
@Schema(description = "Роль пользователя")
public record RoleDto(
        @Schema(description = "ID роли",
                example = "1",
                accessMode = Schema.AccessMode.READ_ONLY)
        Long id,

        @NotBlank(message = "Название роли обязательно")
        @Size(min = 3, max = 50, message = "Название роли должно содержать от 3 до 50 символов")
        @Schema(description = "Название роли",
                example = "ROLE_ADMIN",
                requiredMode = Schema.RequiredMode.REQUIRED,
                minLength = 3,
                maxLength = 50)
        String name,

        @Size(max = 200, message = "Описание не должно превышать 200 символов")
        @Schema(description = "Описание роли",
                example = "Администратор с полными правами",
                maxLength = 200)
        String description,

        @JsonProperty("permissions")
        @Schema(description = "Список разрешений, связанных с ролью",
                example = "[\"CAR_VIEW\", \"CAR_EDIT\", \"USER_MANAGE\"]")
        List<String> permissions
) {
}