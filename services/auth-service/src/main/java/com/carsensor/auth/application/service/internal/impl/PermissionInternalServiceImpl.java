// application/service/internal/impl/PermissionInternalServiceImpl.java
package com.carsensor.auth.application.service.internal.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.internal.PermissionInternalService;
import com.carsensor.auth.domain.entity.Permission;
import com.carsensor.auth.domain.repository.PermissionRepository;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionInternalServiceImpl implements PermissionInternalService {

    private final PermissionRepository permissionRepository;

    @Override
    public Optional<Permission> findPermissionEntityByName(String name) {
        log.debug("Fetching permission entity by name: {}", name);
        return permissionRepository.findByName(name);
    }

    @Override
    public Set<Permission> findPermissionEntitiesByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        Set<Permission> permissions = permissionRepository.findByNameIn(Set.copyOf(names));

        if (permissions.size() != names.size()) {
            Set<String> foundNames = permissions.stream()
                    .map(Permission::getName)
                    .collect(Collectors.toSet());

            List<String> missingNames = names.stream()
                    .filter(name -> !foundNames.contains(name))
                    .toList();

            throw new PlatformException.ValidationException(
                    "Permissions not found: " + String.join(", ", missingNames)
            );
        }

        return permissions;
    }

    @Override
    public Set<Permission> findPermissionEntitiesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        return ids.stream()
                .map(id -> permissionRepository.findById(id)
                        .orElseThrow(() -> new PlatformException.UserNotFoundException("Permission not found with id: " + id)))
                .collect(Collectors.toSet());
    }
}