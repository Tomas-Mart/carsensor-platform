package com.carsensor.auth.application.service.query.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.query.RoleQueryService;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.repository.RoleRepository;
import com.carsensor.platform.dto.RoleDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleQueryServiceImpl implements RoleQueryService {

    private final RoleRepository roleRepository;

    @Override
    public Optional<RoleDto> getRoleById(Long id) {
        log.debug("Fetching role by id: {}", id);
        return roleRepository.findById(id)
                .map(this::mapToDto);
    }

    @Override
    public Optional<RoleDto> getRoleByName(String name) {
        log.debug("Fetching role by name: {}", name);
        return roleRepository.findByNameWithPermissions(name)
                .map(this::mapToDto);
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
    public boolean existsByName(String name) {
        return roleRepository.existsByName(name);
    }

    // Приватные методы
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