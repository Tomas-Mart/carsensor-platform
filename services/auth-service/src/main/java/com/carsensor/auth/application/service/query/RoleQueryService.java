package com.carsensor.auth.application.service.query;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.carsensor.platform.dto.RoleDto;

public interface RoleQueryService {

    Optional<RoleDto> getRoleById(Long id);

    Optional<RoleDto> getRoleByName(String name);

    Page<RoleDto> getAllRoles(Pageable pageable);

    List<RoleDto> getAllRoles();

    boolean existsByName(String name);
}