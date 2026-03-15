package com.carsensor.platform.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.With;

/**
 * DTO для пользователя
 */
@Builder
@With
public record UserDto(
        Long id,

        @NotBlank(message = "Имя пользователя обязательно")
        @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
        @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Имя пользователя может содержать только буквы, цифры и подчеркивание")
        String username,

        @NotBlank(message = "Email обязателен")
        @Email(message = "Неверный формат email")
        @Size(max = 100, message = "Email не должен превышать 100 символов")
        String email,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Size(min = 6, message = "Пароль должен содержать минимум 6 символов")
        String password,

        @Size(max = 50, message = "Имя не должно превышать 50 символов")
        String firstName,

        @Size(max = 50, message = "Фамилия не должна превышать 50 символов")
        String lastName,

        @JsonProperty("is_active")
        boolean isActive,

        List<String> roles,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt,

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime updatedAt
) {
}