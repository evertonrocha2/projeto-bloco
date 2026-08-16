package com.gamelog.integration.service;

import com.gamelog.collection.repository.CollectionRepository;
import com.gamelog.identity.repository.UserRepository;
import com.gamelog.integration.dto.GameActivityResponse;
import com.gamelog.review.dto.RatedGameRow;
import com.gamelog.review.repository.ReviewRepository;
import com.gamelog.shared.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Junta, num unico payload, o que o microsservico de recomendacoes precisa saber
// sobre um usuario.
//
// Por que existe um service so pra isso, em vez de o microsservico chamar os
// endpoints que ja existiam (/api/users/{u} e /api/users/{u}/collection):
//
//  1. Aqueles endpoints nao devolvem o GENERO do jogo - o front nunca precisou
//     dele. Sem genero nao ha como calcular afinidade.
//  2. Seriam duas chamadas de rede em vez de uma, e o microsservico ainda teria
//     que cruzar as duas listas na memoria.
//  3. Mudar o ReviewResponse pra incluir genero afetaria o front e os testes que
//     ja dependem do formato dele. Um consumidor novo com necessidade diferente
//     ganha um endpoint proprio - o formato de cada cliente evolui separado.
@Service
public class GameActivityService {

    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final CollectionRepository collectionRepository;

    public GameActivityService(UserRepository userRepository,
                               ReviewRepository reviewRepository,
                               CollectionRepository collectionRepository) {
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
        this.collectionRepository = collectionRepository;
    }

    @Transactional(readOnly = true)
    public GameActivityResponse getActivity(String username) {
        // Confere que o usuario existe pra poder devolver 404 em vez de um payload
        // vazio. A diferenca importa pra quem chama: "usuario nao existe" e um
        // erro de quem pediu; "usuario sem atividade" e uma resposta legitima.
        if (userRepository.findByUsername(username).isEmpty()) {
            throw new NotFoundException("Usuario nao encontrado");
        }

        // Duas consultas projetadas, uma por lista. Nenhuma entidade e carregada,
        // entao nao ha risco de N+1 resolvendo relacao LAZY.
        List<RatedGameRow> ratedGames = reviewRepository.findRatedGamesByUsername(username);
        List<Long> ownedGameIds = collectionRepository.findOwnedGameIdsByUsername(username);

        return new GameActivityResponse(username, ratedGames, ownedGameIds);
    }
}
