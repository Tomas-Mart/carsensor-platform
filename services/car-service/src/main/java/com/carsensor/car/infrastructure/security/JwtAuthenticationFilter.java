package com.carsensor.car.infrastructure.security;

import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
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
 *   <li>Загружает UserDetails через UserDetailsService</li>
 *   <li>Создает Authentication объект и устанавливает его в SecurityContextHolder</li>
 * </ol>
 *
 * <p><b>Важно:</b> В car-service используется UserDetailsService, который:
 * <ul>
 *   <li>Не обращается к базе данных (нет своей БД пользователей)</li>
 *   <li>Создает UserDetails на основе данных из JWT токена</li>
 *   <li>Роли пользователя извлекаются из JWT токена</li>
 * </ul>
 *
 * <p><b>Обработка ошибок:</b>
 * <ul>
 *   <li>PlatformException пробрасываются в JwtExceptionHandlerFilter</li>
 *   <li>Все остальные исключения оборачиваются в PlatformException.UnauthorizedException</li>
 * </ul>
 *
 * @see JwtTokenProvider
 * @see UserDetailsServiceImpl
 * @see JwtExceptionHandlerFilter
 * @since 1.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            String jwt = parseJwt(request);

            if (StringUtils.hasText(jwt)) {
                // validateToken выбрасывает PlatformException при ошибках
                if (tokenProvider.validateToken(jwt)) {
                    String username = tokenProvider.extractUsername(jwt);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities()
                            );
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("User authenticated: {}", username);
                }
            }
        } catch (PlatformException e) {
            // Пробрасываем PlatformException для обработки в JwtExceptionHandlerFilter
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error in JWT authentication: {}", e.getMessage(), e);
            throw new PlatformException.UnauthorizedException("Ошибка аутентификации", e);
        }

        filterChain.doFilter(request, response);
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
        String headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}