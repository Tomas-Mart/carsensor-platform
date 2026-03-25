package com.carsensor.car.infrastructure.security;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.carsensor.platform.exception.PlatformException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Реализация UserDetailsService для car-service.
 *
 * <p><b>Архитектурное отличие от auth-service:</b>
 * <ul>
 *   <li><b>Auth-service:</b> хранит пользователей в БД, загружает их через репозиторий</li>
 *   <li><b>Car-service:</b> не хранит пользователей, вся информация из JWT токена</li>
 * </ul>
 *
 * <p><b>Принцип работы:</b>
 * <ol>
 *   <li>JwtAuthenticationFilter валидирует токен и извлекает username</li>
 *   <li>loadUserByUsername вызывается Spring Security</li>
 *   <li>Создается базовый UserDetails без ролей</li>
 *   <li>Реальные роли будут добавлены из JWT токена через createUserDetails</li>
 * </ol>
 *
 * @see JwtTokenProvider
 * @see JwtAuthenticationFilter
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final JwtTokenProvider tokenProvider;

    /**
     * Загружает пользователя по имени.
     *
     * <p><b>Важно:</b> В car-service нет своей базы пользователей, поэтому:
     * <ul>
     *   <li>Пользователь не ищется в БД</li>
     *   <li>Возвращается заглушка UserDetails с пустыми правами</li>
     *   <li>Реальные роли будут добавлены в JwtAuthenticationFilter</li>
     * </ul>
     *
     * @param username имя пользователя (из JWT токена)
     * @return UserDetails объект с пустыми правами
     * @throws UsernameNotFoundException если username пустой
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", username);

        if (username == null || username.isBlank()) {
            log.error("Username is null or empty");
            throw new PlatformException.UnauthorizedException("Неверный токен");
        }

        // Возвращаем пользователя без ролей - они будут добавлены из токена в фильтре
        return User.builder()
                .username(username)
                .password("")
                .authorities(List.of())
                .build();
    }

    /**
     * Создает UserDetails на основе данных из JWT токена.
     *
     * <p>Используется в JwtAuthenticationFilter для создания полноценного
     * UserDetails объекта с ролями из токена.
     *
     * @param username имя пользователя из JWT
     * @param roles    список ролей из JWT
     * @return UserDetails объект с ролями
     */
    public UserDetails createUserDetails(String username, List<String> roles) {
        log.debug("Creating UserDetails for user: {} with roles: {}", username, roles);

        if (username == null || username.isBlank()) {
            log.error("Username is null or empty");
            throw new PlatformException.UnauthorizedException("Неверный токен");
        }

        if (roles == null) {
            log.warn("Roles are null for user: {}", username);
            roles = List.of();
        }

        // Преобразуем роли в формат Spring Security
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> {
                    // Убеждаемся, что роль имеет правильный формат
                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    return new SimpleGrantedAuthority(authority);
                })
                .collect(Collectors.toList());

        log.debug("Created UserDetails for user: {} with authorities: {}", username, authorities);

        return User.builder()
                .username(username)
                .password("")
                .authorities(authorities)
                .build();
    }
}