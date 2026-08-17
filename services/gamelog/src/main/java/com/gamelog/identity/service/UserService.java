package com.gamelog.identity.service;

import com.gamelog.identity.domain.User;
import com.gamelog.identity.dto.UpdateProfileRequest;
import com.gamelog.identity.dto.UserProfileResponse;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.review.dto.ReviewResponse;
import com.gamelog.review.dto.ReviewSocial;
import com.gamelog.review.service.ReviewService;
import com.gamelog.review.service.ReviewSocialService;
import com.gamelog.shared.ImageUrl;
import com.gamelog.shared.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

// Monta a pagina de perfil de um usuario: dados publicos dele + as reviews
// que ele escreveu. Pra pegar as reviews, reaproveita o ReviewService.
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ReviewService reviewService;
    private final ReviewSocialService reviewSocialService;

    public UserService(UserRepository userRepository,
                       ReviewService reviewService,
                       ReviewSocialService reviewSocialService) {
        this.userRepository = userRepository;
        this.reviewService = reviewService;
        this.reviewSocialService = reviewSocialService;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username) {
        return getProfile(username, null);
    }

    // viewerUsername e quem esta OLHANDO o perfil, que nem sempre e o dono. E o
    // que decide se os polegares das avaliacoes aparecem marcados. Nulo pra
    // visitante deslogado - o perfil e publico.
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(String username, String viewerUsername) {
        User user = findUser(username);

        List<ReviewResponse> reviews = reviewService.findByUser(user.getId());

        // Em lote, uma vez pra pagina inteira - mesma razao da pagina de um jogo.
        Map<Long, ReviewSocial> social = reviewSocialService.loadFor(
                reviews.stream().map(ReviewResponse::id).toList(), viewerUsername);

        List<ReviewResponse> comSocial = reviews.stream()
                .map(review -> review.withSocial(
                        social.getOrDefault(review.id(), ReviewSocial.empty())))
                .toList();

        return toResponse(user, comSocial);
    }

    // Editar o proprio perfil. Nao ha rota pra editar o de outra pessoa: o
    // username sai do Principal, nunca do corpo.
    @Transactional
    public UserProfileResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = findUser(username);

        // As URLs passam pelo ImageUrl: o valor vai parar num <img src>, e um
        // perfil publico e visto por gente que nao escolheu aquela imagem.
        user.updateProfile(
                request.bio(),
                ImageUrl.sanitize(request.avatarUrl()),
                ImageUrl.sanitize(request.bannerUrl())
        );

        userRepository.save(user);
        return toResponse(user, List.of());
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Usuario nao encontrado"));
    }

    private UserProfileResponse toResponse(User user, List<ReviewResponse> reviews) {
        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getBio(),
                user.getAvatarUrl(),
                user.getBannerUrl(),
                user.getCreatedAt(),
                reviews
        );
    }
}
