package com.carsensor.auth.application.service.internal.impl;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.internal.RoleInternalService;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoleInternalServiceImpl implements RoleInternalService {

    private final RoleRepository roleRepository;

    @Override
    public Optional<Role> findRoleEntityByName(String name) {
        log.debug("Fetching role entity by name: {}", name);
        return roleRepository.findByName(name);
    }

    @Override
    public Set<Role> findRoleEntitiesByNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return Set.of();
        }
        return names.stream()
                .map(this::findRoleEntityByName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }
}