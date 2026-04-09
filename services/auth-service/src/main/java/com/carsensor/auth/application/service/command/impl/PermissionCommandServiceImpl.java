package com.carsensor.auth.application.service.command.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.command.PermissionCommandService;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.auth.domain.repository.PermissionRepository;
import com.carsensor.platform.dto.PermissionDto;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PermissionCommandServiceImpl implements PermissionCommandService {

    private final PermissionRepository permissionRepository;

    @Override
    public PermissionDto createPermission(PermissionDto dto) {
        log.info("Creating new permission: {}", dto.name());

        if (permissionRepository.existsByName(dto.name())) {
            throw new PlatformException.DuplicateResourceException("Permission", "name: " + dto.name());
        }

        Permission permission = Permission.builder()
                .name(dto.name())
                .description(dto.description())
                .build();

        Permission saved = permissionRepository.save(permission);
        log.info("Permission created successfully with id: {}", saved.getId());

        return mapToDto(saved);
    }

    @Override
    public PermissionDto updatePermission(Long id, PermissionDto dto) {
        log.info("Updating permission with id: {}", id);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("Permission not found with id: " + id));

        if (!permission.getName().equals(dto.name()) && permissionRepository.existsByName(dto.name())) {
            throw new PlatformException.DuplicateResourceException("Permission", "name: " + dto.name());
        }

        permission.setName(dto.name());
        permission.setDescription(dto.description());

        Permission updated = permissionRepository.save(permission);
        log.info("Permission updated successfully: {}", id);

        return mapToDto(updated);
    }

    @Override
    public void deletePermission(Long id) {
        log.info("Deleting permission with id: {}", id);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("Permission not found with id: " + id));

        long rolesCount = permissionRepository.countRolesByPermissionId(id);
        if (rolesCount > 0) {
            throw new PlatformException.ValidationException(
                    "Cannot delete permission that is assigned to " + rolesCount + " roles"
            );
        }

        permissionRepository.delete(permission);
        log.info("Permission deleted successfully: {}", id);
    }

    private PermissionDto mapToDto(Permission permission) {
        return new PermissionDto(
                permission.getId(),
                permission.getName(),
                permission.getDescription(),
                permission.getCreatedAt(),
                permission.getUpdatedAt(),
                permission.getVersion()
        );
    }
}