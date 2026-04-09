package com.carsensor.auth.application.service.command;


import com.carsensor.platform.dto.PermissionDto;

public interface PermissionCommandService {

    PermissionDto createPermission(PermissionDto permissionDto);

    PermissionDto updatePermission(Long id, PermissionDto permissionDto);

    void deletePermission(Long id);
}