package com.gamelog.identity.service;

import com.gamelog.identity.domain.User;
import com.gamelog.identity.dto.UserProfileResponse;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.review.dto.ReviewResponse;
import com.gamelog.review.service.ReviewService;
import com.gamelog.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Monta a pagina de perfil de um usuario: dados publicos dele + as reviews
// que ele escreveu. Pra pegar as reviews, reaproveita o ReviewService.
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ReviewService reviewService;

    public UserService(UserRepository userRepository, ReviewService reviewService) {
        this.userRepository = userRepository;
        this.reviewService = reviewService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado"));

        List<ReviewResponse> reviews = reviewService.findByUser(user.getId());

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getBio(),
                user.getCreatedAt(),
                reviews
        );
    }
}
