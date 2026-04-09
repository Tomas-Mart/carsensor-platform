// application/service/query/impl/PermissionQueryServiceImpl.java
package com.carsensor.auth.application.service.query.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.query.PermissionQueryService;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.auth.domain.repository.PermissionRepository;
import com.carsensor.platform.dto.PermissionDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionQueryServiceImpl implements PermissionQueryService {

    private final PermissionRepository permissionRepository;

    @Override
    public Optional<PermissionDto> getPermissionById(Long id) {
        log.debug("Fetching permission by id: {}", id);
        return permissionRepository.findById(id)
                .map(this::mapToDto);
    }

    @Override
    public Optional<PermissionDto> getPermissionByName(String name) {
        log.debug("Fetching permission by name: {}", name);
        return permissionRepository.findByName(name)
                .map(this::mapToDto);
    }

    @Override
    public Page<PermissionDto> getAllPermissions(Pageable pageable) {
        log.debug("Fetching all permissions with pagination");
        return permissionRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Override
    public List<PermissionDto> getAllPermissions() {
        log.debug("Fetching all permissions");
        return permissionRepository.findAllOrderedByName()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByName(String name) {
        return permissionRepository.existsByName(name);
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