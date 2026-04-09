// infrastructure/config/SecurityConfig.java (обновленный)
package com.carsensor.auth.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import com.carsensor.auth.domain.service.PasswordEncoder;
import com.carsensor.auth.infrastructure.security.JwtAuthenticationEntryPoint;
import com.carsensor.auth.infrastructure.security.JwtAuthenticationFilter;
import com.carsensor.auth.infrastructure.security.JwtExceptionHandlerFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Конфигурация безопасности с поддержкой JWT.
 *
 * <p>Использует современные подходы Spring Security 6.x:
 * <ul>
 *   <li>Lambda DSL для конфигурации</li>
 *   <li>Stateless сессии</li>
 *   <li>Кастомные фильтры для JWT</li>
 * </ul>
 *
 * @since 1.0
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/refresh",
            "/api/v1/auth/validate",
            "/actuator/health/**",
            "/actuator/info",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-ui.html"
    };

    private final PasswordEncoder passwordEncoder;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtExceptionHandlerFilter jwtExceptionHandlerFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * Настраивает провайдер аутентификации.
     *
     * @return AuthenticationProvider
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        log.debug("Настройка DaoAuthenticationProvider");

        var provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        // Используем адаптер, реализующий PasswordEncoder из домена
        provider.setPasswordEncoder(new org.springframework.security.crypto.password.PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return passwordEncoder.encode(rawPassword.toString());
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return passwordEncoder.matches(rawPassword.toString(), encodedPassword);
            }
        });
        provider.setHideUserNotFoundExceptions(false);

        return provider;
    }

    /**
     * Настраивает AuthenticationManager.
     *
     * @param authConfig конфигурация аутентификации
     * @return AuthenticationManager
     * @throws Exception если ошибка
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig
    ) throws Exception {
        log.debug("Настройка AuthenticationManager");
        return authConfig.getAuthenticationManager();
    }

    /**
     * Основная цепочка фильтров безопасности.
     *
     * @param http HTTP security
     * @return SecurityFilterChain
     * @throws Exception если ошибка
     */
    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.debug("Настройка SecurityFilterChain");

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .authenticationProvider(authenticationProvider())
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