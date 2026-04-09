package com.carsensor.auth.application.service.internal.impl;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.application.service.internal.UserInternalService;
import com.carsensor.auth.domain.entity.User;
import com.carsensor.auth.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserInternalServiceImpl implements UserInternalService {

    private final UserRepository userRepository;

    @Override
    public Optional<User> findUserEntityByUsername(String username) {
        log.debug("Fetching user entity by username: {}", username);
        return userRepository.findByUsername(username);
    }
}