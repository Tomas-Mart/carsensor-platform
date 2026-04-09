package com.carsensor.car.infrastructure.config;

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

import com.carsensor.car.infrastructure.security.JwtAuthenticationEntryPoint;
import com.carsensor.car.infrastructure.security.JwtAuthenticationFilter;
import com.carsensor.car.infrastructure.security.JwtExceptionHandlerFilter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Конфигурация безопасности Car Service.
 *
 * <p>Настройки безопасности для микросервиса автомобилей:
 * <ul>
 *   <li>Использование JWT для аутентификации</li>
 *   <li>Stateless сессии (без сохранения состояния)</li>
 *   <li>Публичные эндпоинты: health, info, swagger</li>
 *   <li>Все остальные эндпоинты требуют аутентификации</li>
 *   <li>Авторизация на основе ролей через @PreAuthorize аннотации</li>
 * </ul>
 *
 * <p><b>Порядок фильтров:</b>
 * <pre>
 * Request
 *     ↓
 * JwtExceptionHandlerFilter (обработка JWT исключений)
 *     ↓
 * JwtAuthenticationFilter (валидация JWT)
 *     ↓
 * UsernamePasswordAuthenticationFilter
 *     ↓
 * Controller
 * </pre>
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /** Публичные эндпоинты, доступные без аутентификации. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/actuator/health/**",
            "/actuator/info",
            "/actuator/prometheus",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html"
    };

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtExceptionHandlerFilter jwtExceptionHandlerFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Настраивает AuthenticationManager для обработки аутентификации.
     *
     * @param authConfig конфигурация аутентификации
     * @return AuthenticationManager
     * @throws Exception если ошибка конфигурации
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        log.debug("Configuring AuthenticationManager for Car Service");
        return authConfig.getAuthenticationManager();
    }

    /**
     * Настраивает цепочку фильтров безопасности.
     *
     * <p>Конфигурация включает:
     * <ul>
     *   <li>Отключение CSRF (stateless API)</li>
     *   <li>Stateless сессии</li>
     *   <li>Кастомный обработчик исключений</li>
     *   <li>Публичные эндпоинты</li>
     *   <li>Фильтры JWT</li>
     * </ul>
     *
     * @param http HTTP security конфигурация
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
