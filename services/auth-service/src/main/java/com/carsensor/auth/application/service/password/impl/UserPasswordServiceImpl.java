package com.carsensor.auth.application.service.password.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.password.UserPasswordService;
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
public class UserPasswordServiceImpl implements UserPasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        log.info("Changing password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + userId));

        validateUserAccess(user);

        if (user.isLocked()) {
            throw new PlatformException.UserBlockedException(
                    "Невозможно сменить пароль. Аккаунт заблокирован.");
        }

        if (!user.isActive()) {
            throw new PlatformException.UserBlockedException(
                    "Невозможно сменить пароль. Аккаунт деактивирован.");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new PlatformException.InvalidCredentialsException();
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password changed successfully for user: {}", userId);
    }

    @Override
    public void resetPassword(Long userId, String newPassword) {
        log.info("Resetting password for user: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PlatformException.UserNotFoundException("id: " + userId));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", userId);
    }

    private void validateUserAccess(User user) {
        if (!canEdit(user)) {
            throw new PlatformException.AccessDeniedException(
                    getCurrentUsername(),
                    "У вас нет прав для смены пароля этого пользователя");
        }
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