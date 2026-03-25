package com.carsensor.auth.application.service.impl;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.carsensor.auth.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация UserDetailsService с поддержкой тестов и production окружения.
 *
 * <p>Использует REQUIRES_NEW для обеспечения видимости данных между транзакциями в тестах.
 * Это критически важно для интеграционных тестов с @Sql аннотацией.</p>
 *
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @PersistenceContext
    private final EntityManager entityManager;

    /**
     * Загружает пользователя по имени.
     *
     * <p>Использует REQUIRES_NEW для обеспечения свежих данных в тестах.
     * Это позволяет видеть данные, вставленные @Sql в родительской транзакции.</p>
     *
     * @param username имя пользователя
     * @return UserDetails объект
     * @throws UsernameNotFoundException если пользователь не найден
     */
    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        // Очищаем persistence context для получения свежих данных
        entityManager.clear();

        // Пробуем найти через repository
        var userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            log.debug("User found: {}", username);
            return userOpt.get();
        }

        log.error("User not found: {}", username);
        throw new UsernameNotFoundException("User not found: " + username);
    }
}