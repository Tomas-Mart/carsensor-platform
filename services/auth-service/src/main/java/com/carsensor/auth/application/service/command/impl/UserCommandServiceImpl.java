package com.carsensor.auth.application.service.command.impl;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.dto.UserDto;
import com.carsensor.auth.application.mapper.UserMapper;
import com.carsensor.auth.application.service.command.UserCommandService;
import com.carsensor.auth.application.service.internal.RoleInternalService;
import com.carsensor.auth.domain.entity.Role;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.domain.repository.UserRepository;
import com.carsensor.auth.domain.service.PasswordEncoder;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandServiceImpl implements UserCommandService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleInternalService roleInternalService;

    @Override
    public UserDto createUser(UserDto userDto) {
        log.info("Creating new user: {}", userDto.username());

        validateUserDto(userDto);
        validateUserUniqueness(userDto.username(), userDto.email());

        User user = userMapper.toEntity(userDto);
        user.setPassword(passwordEncoder.encode(userDto.password()));
        user.setRoles(getDefaultRoles());

        User savedUser = userRepository.save(user);
        log.info("User created successfully with id: {}", savedUser.getId());

        return userMapper.toDto(savedUser);
    }

    @Override
    public UserDto register(UserDto userDto) {
        log.info("Registering new user: {}", userDto.username());
        return createUser(userDto);  // reuse existing logic
    }

    @Override
    public UserDto updateUser(Long id, UserDto userDto) {
        log.info("Updating user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + id));

        validateUserAccess(user);
        validateVersion(user, userDto.version());
        validateUniquenessForUpdate(user, userDto);

        userMapper.updateEntity(userDto, user);

        User updatedUser = userRepository.save(user);
        log.info("User updated successfully: {}", id);

        return userMapper.toDto(updatedUser);
    }

    @Override
    public UserDto patchUser(Long id, UserDto userDto) {
        log.info("Patching user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + id));

        validateUserAccess(user);
        patchUserFields(user, userDto);

        User patchedUser = userRepository.save(user);
        log.info("User patched successfully: {}", id);

        return userMapper.toDto(patchedUser);
    }

    @Override
    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + id));

        validateUserAccess(user);

        userRepository.deleteById(id);
        log.info("User deleted successfully: {}", id);
    }

    // Приватные методы
    private void validateUserDto(UserDto userDto) {
        if (userDto.username() == null || userDto.username().isBlank()) {
            throw new PlatformException.ValidationException("Имя пользователя обязательно");
        }
        if (userDto.username().length() < 3 || userDto.username().length() > 50) {
            throw new PlatformException.ValidationException("Имя пользователя должно быть от 3 до 50 символов");
        }
        if (userDto.email() == null || userDto.email().isBlank()) {
            throw new PlatformException.ValidationException("Email обязателен");
        }
        if (!userDto.email().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new PlatformException.ValidationException("Некорректный формат email");
        }
        if (userDto.password() == null || userDto.password().length() < 6) {
            throw new PlatformException.ValidationException("Пароль должен быть не менее 6 символов");
        }
    }

    private void validateUserUniqueness(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new PlatformException.DuplicateResourceException("User", "username: " + username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new PlatformException.DuplicateResourceException("User", "email: " + email);
        }
    }

    private void validateUserAccess(User user) {
        if (!canEdit(user)) {
            throw new PlatformException.AccessDeniedException(
                    getCurrentUsername(),
                    "У вас нет прав для редактирования этого пользователя");
        }
    }

    private void validateVersion(User user, Long version) {
        if (version != null && !user.getVersion().equals(version)) {
            throw new PlatformException.OptimisticLockException(
                    "User", user.getId(), version, user.getVersion()
            );
        }
    }

    private void validateUniquenessForUpdate(User user, UserDto userDto) {
        if (!user.getUsername().equals(userDto.username()) &&
            userRepository.existsByUsername(userDto.username())) {
            throw new PlatformException.DuplicateResourceException("User", "username: " + userDto.username());
        }

        if (!user.getEmail().equals(userDto.email()) &&
            userRepository.existsByEmail(userDto.email())) {
            throw new PlatformException.DuplicateResourceException("User", "email: " + userDto.email());
        }
    }

    private Set<Role> getDefaultRoles() {
        return roleInternalService.findRoleEntityByName("ROLE_USER")
                .map(Set::of)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("ROLE_USER"));
    }

    private void patchUserFields(User user, UserDto userDto) {
        if (userDto.username() != null) {
            if (!user.getUsername().equals(userDto.username()) &&
                userRepository.existsByUsername(userDto.username())) {
                throw new PlatformException.DuplicateResourceException(
                        "User", "username: " + userDto.username()
                );
            }
            user.setUsername(userDto.username());
        }

        if (userDto.email() != null) {
            if (!user.getEmail().equals(userDto.email()) &&
                userRepository.existsByEmail(userDto.email())) {
                throw new PlatformException.DuplicateResourceException(
                        "User", "email: " + userDto.email()
                );
            }
            user.setEmail(userDto.email());
        }

        if (userDto.password() != null && !userDto.password().isBlank()) {
            user.setPassword(passwordEncoder.encode(userDto.password()));
        }

        if (userDto.firstName() != null) {
            user.setFirstName(userDto.firstName());
        }

        if (userDto.lastName() != null) {
            user.setLastName(userDto.lastName());
        }

        if (userDto.roles() != null && !userDto.roles().isEmpty()) {
            user.setRoles(getRolesByName(userDto.roles()));
        }
    }

    private Set<Role> getRolesByName(List<String> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            return Set.of();
        }
        return roleInternalService.findRoleEntitiesByNames(roleNames);
    }

    private String getCurrentUsername() {
        return "current_user";
    }

    private boolean canEdit(User user) {
        return getCurrentUsername().equals(user.getUsername()) || isAdmin();
    }

    private boolean isAdmin() {
        return false;
    }
}