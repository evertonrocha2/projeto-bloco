package com.gamelog.identity.dto;

import com.gamelog.review.dto.ReviewResponse;

import java.time.Instant;
import java.util.List;

// Pagina de perfil: dados publicos do usuario + todas as reviews que ele fez.
// Nunca incluimos email ou senha aqui, porque perfil e publico.
public record UserProfileResponse(
        Long id,
        String username,
        String bio,
        Instant createdAt,
        List<ReviewResponse> reviews
) {
}
