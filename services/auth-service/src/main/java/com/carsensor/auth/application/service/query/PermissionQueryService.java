package com.carsensor.auth.application.service.query;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.carsensor.platform.dto.PermissionDto;

public interface PermissionQueryService {

    Optional<PermissionDto> getPermissionById(Long id);

    Optional<PermissionDto> getPermissionByName(String name);

    Page<PermissionDto> getAllPermissions(Pageable pageable);

    List<PermissionDto> getAllPermissions();

    boolean existsByName(String name);
}