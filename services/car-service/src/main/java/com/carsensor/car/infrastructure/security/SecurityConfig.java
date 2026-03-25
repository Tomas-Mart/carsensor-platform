package com.carsensor.car.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Конфигурация безопасности Car Service.
 *
 * <p>Использует JWT для аутентификации и авторизации.
 * Все эндпоинты защищены, кроме публичных (health, swagger).
 *
 * <p><b>Публичные эндпоинты:</b>
 * <ul>
 *   <li>/actuator/health/** - health checks</li>
 *   <li>/swagger-ui/** - Swagger UI</li>
 *   <li>/v3/api-docs/** - OpenAPI документация</li>
 * </ul>
 *
 * <p><b>Порядок фильтров:</b>
 * <pre>
 * Request
 *     ↓
 * JwtExceptionHandlerFilter (первым перехватывает исключения JWT)
 *     ↓
 * JwtAuthenticationFilter (валидирует JWT и устанавливает аутентификацию)
 *     ↓
 * UsernamePasswordAuthenticationFilter
 *     ↓
 * Controller
 * </pre>
 *
 * @see JwtAuthenticationFilter
 * @see JwtExceptionHandlerFilter
 * @see JwtAuthenticationEntryPoint
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health/**",
            "/actuator/info",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtExceptionHandlerFilter jwtExceptionHandlerFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Настраивает AuthenticationManager.
     *
     * @param authConfig конфигурация аутентификации
     * @return AuthenticationManager
     * @throws Exception если ошибка
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        log.debug("Configuring AuthenticationManager for Car Service");
        return authConfig.getAuthenticationManager();
    }

    /**
     * Основная цепочка фильтров безопасности.
     *
     * <p>Конфигурация включает:
     * <ul>
     *   <li>Отключение CSRF и CORS</li>
     *   <li>Stateless сессии</li>
     *   <li>Кастомный обработчик исключений</li>
     *   <li>Настройку публичных эндпоинтов</li>
     *   <li>Порядок фильтров</li>
     * </ul>
     *
     * @param http HTTP security для настройки
     * @return настроенная цепочка фильтров
     * @throws Exception если ошибка конфигурации
     */
    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        log.debug("Configuring SecurityFilterChain for Car Service");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtExceptionHandlerFilter, LogoutFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}