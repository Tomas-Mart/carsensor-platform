package com.carsensor.auth.infrastructure.security;

import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.carsensor.auth.domain.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Фильтр для JWT аутентификации.
 *
 * <p>Перехватывает каждый запрос, извлекает JWT токен из заголовка Authorization,
 * валидирует его и устанавливает аутентификацию в SecurityContext.
 *
 * <p><b>Особенности работы:</b>
 * <ul>
 *   <li>Для публичных эндпоинтов аутентификация не выполняется</li>
 *   <li>Для эндпоинта /logout при отсутствии или невалидном токене возвращается
 *       error_code = INVALID_TOKEN_FORMAT (специфичное требование)</li>
 *   <li>При успешной валидации токена аутентификация устанавливается в SecurityContext</li>
 *   <li>Заблокированные пользователи не проходят аутентификацию</li>
 * </ul>
 *
 * @see JwtTokenProvider
 * @see JwtExceptionHandlerFilter
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    /**
     * Обрабатывает входящий запрос, выполняя JWT аутентификацию.
     *
     * @param request     HTTP запрос
     * @param response    HTTP ответ
     * @param filterChain цепочка фильтров
     * @throws ServletException если произошла ошибка сервлета
     * @throws IOException      если произошла ошибка ввода/вывода
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        var jwt = parseJwt(request);
        var requestURI = request.getRequestURI();

        // Пропускаем публичные эндпоинты (аутентификация не требуется)
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Специальная обработка для эндпоинта /logout:
        // при отсутствии или невалидном токене возвращаем INVALID_TOKEN_FORMAT
        if (requestURI.equals("/api/v1/auth/logout")) {
            if (jwt == null) {
                sendErrorResponse(response, "Токен отсутствует");
                return;
            }

            if (!tokenProvider.validateToken(jwt)) {
                sendErrorResponse(response, "Неверный формат токена");
                return;
            }
        }

        // Основная логика аутентификации
        try {
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                var username = tokenProvider.extractUsername(jwt);
                var userDetails = userDetailsService.loadUserByUsername(username);

                // Проверка что пользователь активен и токен валиден
                if (userDetails instanceof User user && tokenProvider.isTokenValid(jwt, user)) {
                    if (!user.isActive()) {
                        log.warn("Попытка аутентификации заблокированного пользователя: {}", username);
                        filterChain.doFilter(request, response);
                        return;
                    }

                    var authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities()
                    );
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("User authenticated: {}", username);
                }
            }
        } catch (Exception e) {
            log.error("Unexpected error in JWT authentication: {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Отправляет JSON ответ с ошибкой для эндпоинта /logout.
     *
     * @param response HTTP ответ
     * @param message  сообщение об ошибке
     * @throws IOException если произошла ошибка записи
     */
    private void sendErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error_code\":\"INVALID_TOKEN_FORMAT\",\"message\":\"" + message + "\"}");
    }

    /**
     * Извлекает JWT токен из заголовка Authorization.
     *
     * <p>Ожидается формат заголовка: "Bearer &lt;token&gt;"
     *
     * @param request HTTP запрос
     * @return JWT токен или null, если заголовок отсутствует или имеет неверный формат
     */
    private String parseJwt(HttpServletRequest request) {
        var headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }

    /**
     * Проверяет, является ли эндпоинт публичным (не требует аутентификации).
     *
     * <p>Публичные эндпоинты:
     * <ul>
     *   <li>/api/v1/auth/login - вход в систему</li>
     *   <li>/api/v1/auth/register - регистрация нового пользователя</li>
     *   <li>/api/v1/auth/refresh - обновление токена</li>
     *   <li>/api/v1/auth/validate - проверка валидности токена</li>
     *   <li>/actuator/health/** - проверка здоровья сервиса</li>
     *   <li>/actuator/info - информация о приложении</li>
     *   <li>/swagger-ui/** - Swagger UI документация</li>
     *   <li>/v3/api-docs/** - OpenAPI документация</li>
     * </ul>
     *
     * @param requestURI URI запроса
     * @return true если эндпоинт публичный, false в противном случае
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.equals("/api/v1/auth/login") ||
               requestURI.equals("/api/v1/auth/register") ||
               requestURI.equals("/api/v1/auth/refresh") ||
               requestURI.equals("/api/v1/auth/validate") ||
               requestURI.startsWith("/actuator/health") ||
               requestURI.startsWith("/actuator/info") ||
               requestURI.startsWith("/swagger-ui") ||
               requestURI.startsWith("/v3/api-docs");
    }
}