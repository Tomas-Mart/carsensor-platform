package com.carsensor.auth.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import com.carsensor.auth.domain.service.PasswordEncoder;

/**
 * Реализация кодировщика паролей на основе BCrypt.
 *
 * <p>Адаптер между Spring Security BCrypt и доменным интерфейсом.
 *
 * @author CarSensor Platform Team
 * @since 1.0
 */
@Component
public class BCryptPasswordEncoderAdapter implements PasswordEncoder {

    private final BCryptPasswordEncoder delegate = new BCryptPasswordEncoder(12);

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }
}