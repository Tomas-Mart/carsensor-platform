package com.carsensor.auth.application.service.management;

import java.util.List;
import com.carsensor.platform.dto.RoleDto;

public interface RoleManagementService {

    RoleDto assignPermissions(Long roleId, List<Long> permissionIds);

    RoleDto removePermissions(Long roleId, List<Long> permissionIds);
}