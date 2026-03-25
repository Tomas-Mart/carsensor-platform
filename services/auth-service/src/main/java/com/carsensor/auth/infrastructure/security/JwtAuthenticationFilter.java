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
 * <p>При ошибках валидации выбрасывает PlatformException с соответствующим error_code,
 * которые перехватываются JwtExceptionHandlerFilter.
 *
 * @see JwtTokenProvider
 * @see JwtExceptionHandlerFilter
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
            var jwt = parseJwt(request);

            if (StringUtils.hasText(jwt)) {
                // validateToken выбрасывает PlatformException при ошибках
                if (tokenProvider.validateToken(jwt)) {
                    var username = tokenProvider.extractUsername(jwt);
                    var userDetails = userDetailsService.loadUserByUsername(username);

                    // Java 21 pattern matching for instanceof
                    if (userDetails instanceof User user && tokenProvider.isTokenValid(jwt, user)) {
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
     * @param request HTTP запрос
     * @return JWT токен или null
     */
    private String parseJwt(HttpServletRequest request) {
        var headerAuth = request.getHeader("Authorization");

        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }

        return null;
    }
}