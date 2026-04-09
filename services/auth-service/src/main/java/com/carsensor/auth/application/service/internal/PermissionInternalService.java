package com.carsensor.auth.application.service.internal;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.carsensor.auth.domain.entity.Permission;

public interface PermissionInternalService {

    Optional<Permission> findPermissionEntityByName(String name);

    Set<Permission> findPermissionEntitiesByNames(List<String> names);

    Set<Permission> findPermissionEntitiesByIds(List<Long> ids);
}