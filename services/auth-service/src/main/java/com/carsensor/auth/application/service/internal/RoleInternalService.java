package com.carsensor.auth.application.service.internal;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.carsensor.auth.domain.entity.Role;

public interface RoleInternalService {

    Optional<Role> findRoleEntityByName(String name);

    Set<Role> findRoleEntitiesByNames(List<String> names);
}