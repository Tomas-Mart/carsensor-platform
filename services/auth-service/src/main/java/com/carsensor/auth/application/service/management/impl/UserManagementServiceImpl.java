package com.carsensor.auth.application.service.management.impl;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.dto.UserDto;
import com.carsensor.auth.application.mapper.UserMapper;
import com.carsensor.auth.application.service.internal.RoleInternalService;
import com.carsensor.auth.application.service.management.UserManagementService;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.domain.repository.UserRepository;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserManagementServiceImpl implements UserManagementService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RoleInternalService roleInternalService;

    @Override
    public void blockUser(Long id) {
        log.info("Blocking user with id: {}", id);

        validateAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + id));

        user.block();
        userRepository.save(user);

        log.info("User blocked successfully: {}", id);
    }

    @Override
    public void unblockUser(Long id) {
        log.info("Unblocking user with id: {}", id);

        validateAdmin();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + id));

        user.unblock();
        userRepository.save(user);

        log.info("User unblocked successfully: {}", id);
    }

    @Override
    public UserDto assignRoles(Long userId, List<String> roleNames) {
        log.info("Assigning roles {} to user: {}", roleNames, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + userId));

        Set<Role> roles = getRolesByName(roleNames);
        user.getRoles().addAll(roles);

        User updatedUser = userRepository.save(user);
        log.info("Roles assigned successfully to user: {}", userId);

        return userMapper.toDto(updatedUser);
    }

    @Override
    public UserDto removeRoles(Long userId, List<String> roleNames) {
        log.info("Removing roles {} from user: {}", roleNames, userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + userId));

        Set<Role> rolesToRemove = getRolesByName(roleNames);
        user.getRoles().removeAll(rolesToRemove);

        User updatedUser = userRepository.save(user);
        log.info("Roles removed successfully from user: {}", userId);

        return userMapper.toDto(updatedUser);
    }

    private Set<Role> getRolesByName(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }
        return roleInternalService.findRoleEntitiesByNames(roleNames);
    }

    private void validateAdmin() {
        if (!isAdmin()) {
            throw new PlatformException.AccessDeniedException(
                    getCurrentUsername(),
                    "ADMIN",
                    "Только администратор может выполнять эту операцию"
            );
        }
    }

    private String getCurrentUsername() {
        return "current_user";
    }

    private boolean isAdmin() {
        return false;
    }
}