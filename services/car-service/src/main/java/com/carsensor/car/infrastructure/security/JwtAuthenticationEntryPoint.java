package com.carsensor.car.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Обработчик неавторизованного доступа (401 Unauthorized).
 *
 * <p>Этот entry point используется как fallback, когда JwtExceptionHandlerFilter
 * не перехватил исключение. Возвращает общий error_code UNAUTHORIZED.
 *
 * <p>Срабатывает когда:
 * <ul>
 *   <li>Пользователь не аутентифицирован</li>
 *   <li>Spring Security не может найти аутентификацию</li>
 *   <li>JwtExceptionHandlerFilter не обработал исключение</li>
 * </ul>
 *
 * @see JwtExceptionHandlerFilter
 * @see com.carsensor.platform.exception.PlatformException
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {

        log.error("Unauthorized error: {}", authException != null ? authException.getMessage() : "No authentication details");

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        body.put("error", "Unauthorized");
        body.put("error_code", "UNAUTHORIZED");
        body.put("message", "Необходима аутентификация для доступа к ресурсу");
        body.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}