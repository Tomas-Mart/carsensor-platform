package com.carsensor.platform.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Ответ с JWT токенами
 */
@Builder
public record AuthResponse(
        @JsonProperty("access_token")
        String accessToken,

        @JsonProperty("refresh_token")
        String refreshToken,

        @JsonProperty("token_type")
        String tokenType,

        @JsonProperty("expires_in")
        long expiresIn,

        @JsonProperty("username")
        String username,

        @JsonProperty("roles")
        String[] roles
) {
    public static final String TOKEN_TYPE = "Bearer";

    public AuthResponse {
        tokenType = TOKEN_TYPE;
    }
}