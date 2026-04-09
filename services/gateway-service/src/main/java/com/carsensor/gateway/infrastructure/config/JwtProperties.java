package com.carsensor.gateway.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

/**
 * JWT configuration properties.
 *
 * <p>Usage in application.yml:
 * <pre>
 * app:
 *   jwt:
 *     secret: your-secret-key
 *     access-token-expiration: 900
 *     refresh-token-expiration: 604800
 * </pre>
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        @NotBlank(message = "JWT secret must not be blank")
        String secret,

        long accessTokenExpiration,

        long refreshTokenExpiration
) {

    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            secret = "defaultSecretKeyForDevelopmentOnly";
        }
        if (accessTokenExpiration < 60) {
            accessTokenExpiration = 900;
        }
        if (refreshTokenExpiration < 3600) {
            refreshTokenExpiration = 604800;
        }
    }

    public static JwtProperties defaults() {
        return new JwtProperties(
                "defaultSecretKeyForDevelopmentOnly",
                900,
                604800
        );
    }
}