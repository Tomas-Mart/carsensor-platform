package com.carsensor.auth.application.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.platform.dto.PermissionDto;

/**
 * Маппер для Permission <-> PermissionDto.
 *
 * <p>Без MapStruct, использует явное преобразование.
 * Предоставляет методы для конвертации между Entity и DTO,
 * включая преобразование списков и обновление существующих сущностей.
 *
 * <p><b>Особенности:</b>
 * <ul>
 *   <li>Поля аудита (createdAt, updatedAt, version) копируются</li>
 *   <li>Поддерживает частичное обновление через updateEntity</li>
 * </ul>
 *
 * @author CarSensor Platform Team
 * @see Permission
 * @see PermissionDto
 * @since 1.0
 */
@Component
public class PermissionMapper {

    /**
     * Преобразует Permission в PermissionDto.
     *
     * @param permission сущность разрешения
     * @return DTO разрешения или null если входной параметр null
     */
    public PermissionDto toDto(Permission permission) {
        if (permission == null) {
            return null;
        }

        return new PermissionDto(
                permission.getId(),
                permission.getName(),
                permission.getDescription(),
                permission.getCreatedAt(),
                permission.getUpdatedAt(),
                permission.getVersion()
        );
    }

    /**
     * Преобразует PermissionDto в Permission.
     *
     * <p><b>Важно:</b> Поля аудита (createdAt, updatedAt, version)
     * не копируются, они устанавливаются автоматически при сохранении.
     *
     * @param dto DTO разрешения
     * @return сущность разрешения или null если входной параметр null
     */
    public Permission toEntity(PermissionDto dto) {
        if (dto == null) {
            return null;
        }

        return Permission.builder()
                .id(dto.id())
                .name(dto.name())
                .description(dto.description())
                .build();
    }

    /**
     * Обновляет существующую сущность Permission из PermissionDto.
     *
     * <p><b>Особенности:</b>
     * <ul>
     *   <li>Обновляет только name и description</li>
     *   <li>Поля аудита не обновляются (управляются Hibernate)</li>
     *   <li>Поддерживает частичное обновление (только не-null поля)</li>
     * </ul>
     *
     * @param dto        DTO разрешения с новыми данными
     * @param permission существующая сущность для обновления
     */
    public void updateEntity(PermissionDto dto, Permission permission) {
        if (dto == null || permission == null) {
            return;
        }

        if (dto.name() != null && !dto.name().equals(permission.getName())) {
            permission.setName(dto.name());
        }
        if (dto.description() != null) {
            permission.setDescription(dto.description());
        }
    }

    /**
     * Преобразует список Permission в список PermissionDto.
     *
     * @param permissions список сущностей разрешений
     * @return список DTO разрешений (пустой список если входной параметр null)
     */
    public List<PermissionDto> toDtoList(List<Permission> permissions) {
        if (permissions == null) {
            return List.of();
        }
        return permissions.stream()
                .map(this::toDto)
                .toList();
    }
}