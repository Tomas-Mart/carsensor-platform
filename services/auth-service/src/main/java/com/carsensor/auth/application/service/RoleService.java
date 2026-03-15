package com.carsensor.auth.application.service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Интерфейс сервиса для работы с ролями
 */
public interface RoleService {

    RoleDto createRole(RoleDto roleDto);

    RoleDto getRoleById(Long id);

    RoleDto getRoleByName(String name);

    Page<RoleDto> getAllRoles(Pageable pageable);

    List<RoleDto> getAllRoles();

    RoleDto updateRole(Long id, RoleDto roleDto);

    void deleteRole(Long id);

    RoleDto assignPermissions(Long roleId, List<Long> permissionIds);

    RoleDto removePermissions(Long roleId, List<Long> permissionIds);

    boolean existsByName(String name);

    record RoleDto(
            Long id,
            String name,
            String description,
            List<String> permissions
    ) {
    }
}