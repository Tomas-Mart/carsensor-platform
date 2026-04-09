package com.carsensor.car.infrastructure.security;

import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import com.carsensor.platform.exception.PlatformException;
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
 * <p><b>Порядок работы:</b>
 * <ol>
 *   <li>Извлекает JWT токен из заголовка Authorization (Bearer token)</li>
 *   <li>Валидирует токен (подпись, срок действия, формат)</li>
 *   <li>Извлекает username из токена</li>
 *   <li>Извлекает роли из токена</li>
 *   <li>Создает UserDetails на основе данных из токена</li>
 *   <li>Создает Authentication объект и устанавливает его в SecurityContextHolder</li>
 * </ol>
 *
 * <p><b>Важно:</b> В car-service UserDetailsService не обращается к базе данных,
 * а создает UserDetails на основе данных из JWT токена. Это позволяет избежать
 * дополнительных запросов к БД и делает сервис stateless.
 *
 * @author CarSensor Platform Team
 * @see JwtTokenProvider
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Выполняет фильтрацию запроса.
     *
     * @param request     HTTP запрос
     * @param response    HTTP ответ
     * @param filterChain цепочка фильтров
     * @throws ServletException если ошибка сервлета
     * @throws IOException      если ошибка ввода-вывода
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String jwt = parseJwt(request);

        try {
            if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
                String username = tokenProvider.extractUsername(jwt);
                List<String> roles = tokenProvider.extractRoles(jwt);
                boolean isActive = tokenProvider.isUserActive(jwt);

                log.debug("Authenticating user: {} with roles: {}, isActive: {}", username, roles, isActive);

                // Проверка активности пользователя (Car Service доверяет Auth Service)
                if (!isActive) {
                    log.warn("User is not active: {}", username);
                    throw new PlatformException.UserBlockedException(username);
                }

                // Создаем UserDetails из данных токена (без обращения к БД)
                UserDetails userDetails = createUserDetails(username, roles);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userDetails, null, userDetails.getAuthorities()
                        );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("User authenticated successfully: {}", username);
            }
        } catch (PlatformException e) {
            log.debug("JWT validation error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in JWT authentication: {}", e.getMessage(), e);
            throw new PlatformException.UnauthorizedException("Ошибка аутентификации", e);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Создает UserDetails на основе username и ролей из JWT токена.
     *
     * <p>Этот метод не обращается к базе данных, а использует данные из токена,
     * что делает сервис полностью stateless.
     *
     * @param username имя пользователя
     * @param roles    список прав из токена (CREATE_CARS, EDIT_CARS, DELETE_CARS, VIEW_CARS)
     * @return UserDetails объект для Spring Security
     */
    private UserDetails createUserDetails(String username, List<String> roles) {
        log.debug("Creating UserDetails for user: {} with authorities: {}", username, roles);

        return User.builder()
                .username(username)
                .password("") // Пароль не требуется для аутентификации по JWT
                .authorities(roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    /**
     * Извлекает JWT токен из заголовка Authorization.
     *
     * <p>Ожидается заголовок в формате: {@code Authorization: Bearer <token>}
     *
     * @param request HTTP запрос
     * @return JWT токен или null, если заголовок отсутствует или имеет неверный формат
     */
    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith(BEARER_PREFIX)) {
            return headerAuth.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}