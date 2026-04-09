package com.carsensor.auth.application.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.platform.dto.RoleDto;

/**
 * Маппер для Role <-> RoleDto.
 *
 * <p>Без MapStruct, использует явное преобразование.
 * Предоставляет методы для конвертации между Entity и DTO,
 * включая преобразование списков и обновление существующих сущностей.
 *
 * @author CarSensor Platform Team
 * @see Role
 * @see RoleDto
 * @since 1.0
 */
@Component
public class RoleMapper {

    /**
     * Преобразует Role в RoleDto.
     *
     * @param role сущность роли
     * @return DTO роли или null если входной параметр null
     */
    public RoleDto toDto(Role role) {
        if (role == null) {
            return null;
        }

        return new RoleDto(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream()
                        .map(Permission::getName)
                        .toList(),
                role.getCreatedAt(),
                role.getUpdatedAt(),
                role.getVersion()
        );
    }

    /**
     * Преобразует RoleDto в Role.
     *
     * <p><b>Важно:</b> Поля аудита (createdAt, updatedAt, version)
     * не копируются, они устанавливаются автоматически при сохранении.
     *
     * @param dto DTO роли
     * @return сущность роли или null если входной параметр null
     */
    public Role toEntity(RoleDto dto) {
        if (dto == null) {
            return null;
        }

        return Role.builder()
                .id(dto.id())
                .name(dto.name())
                .description(dto.description())
                .build();
    }

    /**
     * Обновляет существующую сущность Role из RoleDto.
     *
     * <p><b>Особенности:</b>
     * <ul>
     *   <li>Обновляет только name и description</li>
     *   <li>Permissions не обновляются здесь — это делается отдельными методами</li>
     *   <li>Поля аудита не обновляются (управляются Hibernate)</li>
     * </ul>
     *
     * @param dto  DTO роли с новыми данными
     * @param role существующая сущность для обновления
     */
    public void updateEntity(RoleDto dto, Role role) {
        if (dto == null || role == null) {
            return;
        }

        if (dto.name() != null && !dto.name().equals(role.getName())) {
            role.setName(dto.name());
        }
        if (dto.description() != null) {
            role.setDescription(dto.description());
        }
        // permissions не обновляем здесь — это делается отдельными методами
    }

    /**
     * Преобразует список Role в список RoleDto.
     *
     * @param roles список сущностей ролей
     * @return список DTO ролей (пустой список если входной параметр null)
     */
    public List<RoleDto> toDtoList(List<Role> roles) {
        if (roles == null) {
            return List.of();
        }
        return roles.stream()
                .map(this::toDto)
                .toList();
    }
}