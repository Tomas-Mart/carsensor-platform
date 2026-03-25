package com.carsensor.auth.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        log.warn("Unauthorized access attempt to: {} from IP: {} - Error: {}",
                request.getRequestURI(),
                getClientIp(request),
                authException != null ? authException.getMessage() : "No authentication details");

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        var errorResponse = new HashMap<String, Object>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("error_code", "UNAUTHORIZED");
        errorResponse.put("message", "Необходима аутентификация для доступа к ресурсу");
        errorResponse.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    /**
     * Получает IP адрес клиента с учетом прокси.
     *
     * @param request HTTP запрос
     * @return IP адрес клиента
     */
    private String getClientIp(HttpServletRequest request) {
        var xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}