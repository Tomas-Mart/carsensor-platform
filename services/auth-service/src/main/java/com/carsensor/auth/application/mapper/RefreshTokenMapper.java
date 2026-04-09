package com.carsensor.auth.application.mapper;

import java.util.List;
import org.springframework.stereotype.Component;
import com.carsensor.auth.domain.entity.RefreshToken;
import com.carsensor.platform.dto.RefreshTokenDto;

@Component
public class RefreshTokenMapper {

    public RefreshTokenDto toDto(RefreshToken token) {
        if (token == null) {
            return null;
        }

        return new RefreshTokenDto(
                token.getId(),
                token.getToken(),
                token.getUser().getId(),
                token.getUser().getUsername(),
                token.getExpiresAt(),
                token.getCreatedAt(),
                !token.isExpired()
        );
    }

    public List<RefreshTokenDto> toDtoList(List<RefreshToken> tokens) {
        if (tokens == null) {
            return List.of();
        }
        return tokens.stream()
                .map(this::toDto)
                .toList();
    }
}