package com.carsensor.auth.application.service.management.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.internal.PermissionInternalService;
import com.carsensor.auth.application.service.management.RoleManagementService;
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
public class RoleManagementServiceImpl implements RoleManagementService {

    private final RoleRepository roleRepository;
    private final PermissionInternalService permissionInternalService;

    @Override
    public RoleDto assignPermissions(Long roleId, List<Long> permissionIds) {
        log.info("Assigning permissions {} to role: {}", permissionIds, roleId);

        // Проверка наличия permissionIds
        if (permissionIds == null || permissionIds.isEmpty()) {
            log.warn("No permissions to assign to role: {}", roleId);
            throw new PlatformException.ValidationException("At least one permission ID must be provided");
        }

        // Проверка существования роли
        Role role = findRoleByIdOrThrow(roleId);

        // Получение разрешений через сервис (с проверкой существования)
        Set<Permission> permissions = permissionInternalService.findPermissionEntitiesByIds(permissionIds);

        // Проверка, что все разрешения найдены
        validatePermissionsExistByIds(permissions, permissionIds);

        // Добавление разрешений
        role.getPermissions().addAll(permissions);

        Role updatedRole = roleRepository.save(role);
        log.info("Permissions assigned successfully to role: {}", roleId);

        return mapToDto(updatedRole);
    }

    @Override
    public RoleDto removePermissions(Long roleId, List<Long> permissionIds) {
        log.info("Removing permissions {} from role: {}", permissionIds, roleId);

        // Проверка наличия permissionIds
        if (permissionIds == null || permissionIds.isEmpty()) {
            log.warn("No permissions to remove from role: {}", roleId);
            throw new PlatformException.ValidationException("At least one permission ID must be provided");
        }

        // Проверка существования роли
        Role role = findRoleByIdOrThrow(roleId);

        // Получение разрешений через сервис (с проверкой существования)
        Set<Permission> permissionsToRemove = permissionInternalService.findPermissionEntitiesByIds(permissionIds);

        // Проверка, что все разрешения для удаления найдены
        validatePermissionsExistByIds(permissionsToRemove, permissionIds);

        // Удаление разрешений
        role.getPermissions().removeAll(permissionsToRemove);

        Role updatedRole = roleRepository.save(role);
        log.info("Permissions removed successfully from role: {}", roleId);

        return mapToDto(updatedRole);
    }

    // Приватные методы
    private Role findRoleByIdOrThrow(Long roleId) {
        return roleRepository.findById(roleId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("Role not found with id: " + roleId));
    }

    private void validatePermissionsExistByIds(Set<Permission> permissions, List<Long> permissionIds) {
        if (permissions.size() != permissionIds.size()) {
            Set<Long> foundIds = permissions.stream()
                    .map(Permission::getId)
                    .collect(Collectors.toSet());

            List<Long> missingIds = permissionIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            throw new PlatformException.ValidationException(
                    "Permissions not found with ids: " + missingIds
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