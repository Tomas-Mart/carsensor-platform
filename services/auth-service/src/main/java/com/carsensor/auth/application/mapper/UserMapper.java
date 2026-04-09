package com.carsensor.auth.application.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import com.carsensor.auth.application.dto.UserDto;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;

/**
 * Маппер для User <-> UserDto
 * Без MapStruct, использует явное преобразование
 */
@Component
public class UserMapper {

    /**
     * Преобразует User в UserDto
     */
    public UserDto toDto(User user) {
        if (user == null) {
            return null;
        }

        return new UserDto(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                null, // пароль не передаем в DTO
                user.getFirstName(),
                user.getLastName(),
                user.isActive(),
                user.isLocked(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .toList(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getVersion()
        );
    }

    /**
     * Преобразует UserDto в User (без пароля, без ролей)
     */
    public User toEntity(UserDto dto) {
        if (dto == null) {
            return null;
        }

        return User.builder()
                .id(dto.id())
                .username(dto.username())
                .email(dto.email())
                .password(dto.password()) // будет закодирован в сервисе
                .firstName(dto.firstName())
                .lastName(dto.lastName())
                .isActive(dto.isActive())
                .isLocked(dto.isLocked())
                .version(dto.version())
                .build();
    }

    /**
     * Обновляет существующего User из UserDto
     */
    public void updateEntity(UserDto dto, User user) {
        if (dto == null || user == null) {
            return;
        }

        if (dto.username() != null && !dto.username().equals(user.getUsername())) {
            user.setUsername(dto.username());
        }
        if (dto.email() != null && !dto.email().equals(user.getEmail())) {
            user.setEmail(dto.email());
        }
        if (dto.firstName() != null) {
            user.setFirstName(dto.firstName());
        }
        if (dto.lastName() != null) {
            user.setLastName(dto.lastName());
        }
        if (dto.version() != null) {
            user.setVersion(dto.version());
        }
        // isActive и isLocked не обновляем здесь — это делается отдельными методами
    }

    /**
     * Преобразует список User в список UserDto
     */
    public List<UserDto> toDtoList(List<User> users) {
        if (users == null) {
            return List.of();
        }
        return users.stream()
                .map(this::toDto)
                .toList();
    }
}