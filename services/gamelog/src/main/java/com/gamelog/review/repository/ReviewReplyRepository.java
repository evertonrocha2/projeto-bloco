package com.gamelog.review.repository;

import com.gamelog.review.domain.ReviewReply;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewReplyRepository extends JpaRepository<ReviewReply, Long> {

    // Todas as respostas das avaliacoes de uma pagina, de uma vez, em ordem
    // cronologica.
    //
    // Plana de proposito: a arvore e montada em memoria pelo service. Buscar
    // filho por filho seria uma consulta por no, e uma discussao de trinta
    // respostas viraria trinta idas ao banco. A ordem cronologica e o que faz os
    // irmaos sairem na ordem certa sem precisar reordenar depois.
    List<ReviewReply> findByReviewIdInOrderByCreatedAtAsc(List<Long> reviewIds);

    // Se alguem ja respondeu esta resposta. Decide entre apagar de verdade e
    // virar lapide.
    boolean existsByParentId(Long parentId);
}
