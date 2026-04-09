package com.carsensor.auth.application.service.command.impl;

import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.command.RoleCommandService;
import com.carsensor.auth.application.service.internal.PermissionInternalService;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.repository.RoleRepository;
import com.carsensor.platform.dto.RoleDto;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class RoleCommandServiceImpl implements RoleCommandService {

    private final RoleRepository roleRepository;
    private final PermissionInternalService permissionInternalService;

    @Override
    public RoleDto createRole(RoleDto roleDto) {
        log.info("Creating new role: {}", roleDto.name());

        // Проверка уникальности имени
        if (roleRepository.existsByName(roleDto.name())) {
            throw new PlatformException.DuplicateResourceException("Role", "name: " + roleDto.name());
        }

        // Получение разрешений
        Set<Permission> permissions = getPermissions(roleDto.permissions());

        // Создание роли
        Role role = Role.builder()
                .name(roleDto.name())
                .description(roleDto.description())
                .permissions(permissions)
                .build();

        Role savedRole = roleRepository.save(role);
        log.info("Role created successfully with id: {}", savedRole.getId());

        return mapToDto(savedRole);
    }

    @Override
    public RoleDto updateRole(Long id, RoleDto roleDto) {
        log.info("Updating role with id: {}", id);

        Role role = findRoleByIdOrThrow(id);

        // Проверка уникальности имени при изменении
        if (!role.getName().equals(roleDto.name()) && roleRepository.existsByName(roleDto.name())) {
            throw new PlatformException.DuplicateResourceException("Role", "name: " + roleDto.name());
        }

        // Обновление полей
        role.setName(roleDto.name());
        role.setDescription(roleDto.description());

        // Обновление разрешений, если они предоставлены
        if (roleDto.permissions() != null && !roleDto.permissions().isEmpty()) {
            Set<Permission> permissions = getPermissions(roleDto.permissions());
            role.setPermissions(permissions);
        }

        Role updatedRole = roleRepository.save(role);
        log.info("Role updated successfully: {}", id);

        return mapToDto(updatedRole);
    }

    @Override
    public void deleteRole(Long id) {
        log.info("Deleting role with id: {}", id);

        Role role = findRoleByIdOrThrow(id);

        // Проверка, используется ли роль пользователями
        long usersCount = roleRepository.countUsersByRoleId(id);
        if (usersCount > 0) {
            throw new PlatformException.ValidationException(
                    "Cannot delete role that is assigned to " + usersCount + " users"
            );
        }

        roleRepository.delete(role);
        log.info("Role deleted successfully: {}", id);
    }

    // Приватные методы
    private Role findRoleByIdOrThrow(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("Role not found with id: " + roleId));
    }

    private Set<Permission> getPermissions(java.util.List<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return Set.of();
        }

        Set<Permission> permissions = permissionInternalService.findPermissionEntitiesByNames(permissionNames);
        validatePermissionsExist(permissions, permissionNames);

        return permissions;
    }

    private void validatePermissionsExist(Set<Permission> permissions, java.util.List<String> permissionNames) {
        if (permissions.size() != permissionNames.size()) {
            Set<String> foundIds = permissions.stream()
                    .map(Permission::getName)
                    .collect(java.util.stream.Collectors.toSet());

            java.util.List<String> missingIds = permissionNames.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            throw new PlatformException.ValidationException(
                    "Permissions not found with names: " + missingIds
            );
        }
    }

    private RoleDto mapToDto(Role role) {
        return new RoleDto(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream()
                        .map(Permission::getName)
                        .collect(java.util.stream.Collectors.toList()),
                role.getCreatedAt(),
                role.getUpdatedAt(),
                role.getVersion()
        );
    }
}