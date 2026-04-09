package com.carsensor.auth.application.service.command;


import com.carsensor.platform.dto.RoleDto;

public interface RoleCommandService {

    RoleDto createRole(RoleDto roleDto);

    RoleDto updateRole(Long id, RoleDto roleDto);

    void deleteRole(Long id);
}