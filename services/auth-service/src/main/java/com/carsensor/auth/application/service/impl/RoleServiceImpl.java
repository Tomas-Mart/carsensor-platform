package com.carsensor.auth.application.service.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.RoleService;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.repository.PermissionRepository;
import com.carsensor.auth.domain.repository.RoleRepository;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация сервиса для работы с ролями
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional
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
    public RoleDto getRoleById(Long id) {
        log.debug("Fetching role by id: {}", id);

        return roleRepository.findById(id)
                .map(this::mapToDto)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("Role not found with id: " + id));
    }

    @Override
    public RoleDto getRoleByName(String name) {
        log.debug("Fetching role by name: {}", name);

        return roleRepository.findByNameWithPermissions(name)
                .map(this::mapToDto)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("Role not found with name: " + name));
    }

    @Override
    public Page<RoleDto> getAllRoles(Pageable pageable) {
        log.debug("Fetching all roles with pagination");

        return roleRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Override
    public List<RoleDto> getAllRoles() {
        log.debug("Fetching all roles");

        return roleRepository.findAllWithPermissions()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RoleDto updateRole(Long id, RoleDto roleDto) {
        log.info("Updating role with id: {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("Role not found with id: " + id));

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
    @Transactional
    public void deleteRole(Long id) {
        log.info("Deleting role with id: {}", id);

        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("Role not found with id: " + id));

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

    @Override
    @Transactional
    public RoleDto assignPermissions(Long roleId, List<Long> permissionIds) {
        log.info("Assigning permissions {} to role: {}", permissionIds, roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("Role not found with id: " + roleId));

        Set<Permission> permissions = permissionIds.stream()
                .map(id -> permissionRepository.findById(id)
                        .orElseThrow(() -> new PlatformException.UserNotFoundException("Permission not found with id: " + id)))
                .collect(Collectors.toSet());

        role.getPermissions().addAll(permissions);

        Role updatedRole = roleRepository.save(role);
        log.info("Permissions assigned successfully to role: {}", roleId);

        return mapToDto(updatedRole);
    }

    @Override
    @Transactional
    public RoleDto removePermissions(Long roleId, List<Long> permissionIds) {
        log.info("Removing permissions {} from role: {}", permissionIds, roleId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("Role not found with id: " + roleId));

        Set<Permission> permissionsToRemove = permissionIds.stream()
                .map(id -> permissionRepository.findById(id)
                        .orElseThrow(() -> new PlatformException.UserNotFoundException("Permission not found with id: " + id)))
                .collect(Collectors.toSet());

        role.getPermissions().removeAll(permissionsToRemove);

        Role updatedRole = roleRepository.save(role);
        log.info("Permissions removed successfully from role: {}", roleId);

        return mapToDto(updatedRole);
    }

    @Override
    public boolean existsByName(String name) {
        return roleRepository.existsByName(name);
    }

    // Приватные вспомогательные методы
    private Set<Permission> getPermissions(List<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return Set.of();
        }

        Set<Permission> permissions = permissionRepository.findByNameIn(Set.copyOf(permissionNames));

        // Проверка, что все запрошенные разрешения существуют
        if (permissions.size() != permissionNames.size()) {
            Set<String> foundNames = permissions.stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet());

            List<String> missingNames = permissionNames.stream()
                    .filter(name -> !foundNames.contains(name))
                    .toList();

            throw new PlatformException.ValidationException(
                    "Permissions not found: " + String.join(", ", missingNames)
            );
        }

        return permissions;
    }

    private RoleDto mapToDto(Role role) {
        return new RoleDto(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getPermissions().stream()
                        .map(Permission::getName)
                        .collect(Collectors.toList())
        );
    }
}