package com.carsensor.car.infrastructure.security;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.carsensor.platform.exception.PlatformException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Фильтр для перехвата исключений JWT и преобразования их в JSON ответы.
 *
 * <p>Этот фильтр ДОЛЖЕН быть добавлен ПЕРЕД JwtAuthenticationFilter,
 * чтобы перехватывать исключения, выбрасываемые в фильтре аутентификации.
 *
 * <p>Возвращает специфичные error_code:
 * <ul>
 *   <li>MISSING_TOKEN - отсутствует токен в заголовке</li>
 *   <li>INVALID_TOKEN_FORMAT - неверный формат токена или подпись</li>
 *   <li>TOKEN_EXPIRED - срок действия токена истек</li>
 *   <li>INVALID_TOKEN - неподдерживаемый тип токена</li>
 *   <li>UNAUTHORIZED - общая ошибка аутентификации</li>
 * </ul>
 *
 * @see JwtAuthenticationFilter
 * @see PlatformException
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtExceptionHandlerFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (PlatformException e) {
            handlePlatformException(response, request, e);
        } catch (Exception e) {
            handleGenericException(response, request, e);
        }
    }

    /**
     * Обрабатывает специфичные PlatformException.
     *
     * @param response HTTP ответ
     * @param request  HTTP запрос
     * @param e        исключение
     * @throws IOException при ошибке записи ответа
     */
    private void handlePlatformException(HttpServletResponse response,
                                         HttpServletRequest request,
                                         PlatformException e) throws IOException {
        log.warn("JWT error: {} - {}", e.getErrorCode(), e.getUserMessage());

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        var errorResponse = new HashMap<String, Object>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("error_code", e.getErrorCode());
        errorResponse.put("message", e.getUserMessage());
        errorResponse.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    /**
     * Обрабатывает непредвиденные исключения.
     *
     * @param response HTTP ответ
     * @param request  HTTP запрос
     * @param e        исключение
     * @throws IOException при ошибке записи ответа
     */
    private void handleGenericException(HttpServletResponse response,
                                        HttpServletRequest request,
                                        Exception e) throws IOException {
        log.error("Unexpected error in JWT filter", e);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        var errorResponse = new HashMap<String, Object>();
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("status", HttpServletResponse.SC_UNAUTHORIZED);
        errorResponse.put("error", "Unauthorized");
        errorResponse.put("error_code", "UNAUTHORIZED");
        errorResponse.put("message", "Ошибка аутентификации");
        errorResponse.put("path", request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}