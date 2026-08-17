import { request } from './client.js'

// A conversa em volta de uma avaliacao: votos e respostas. Atendida pelo monolito.
export const socialApi = {
  // PUT e nao POST porque da tela e UM gesto - clicar no polegar. O servidor
  // decide entre criar, trocar de lado e desfazer, e devolve o placar ja
  // recalculado: a tela pinta o resultado sem uma segunda chamada.
  voteReview: (reviewId, type) =>
    request(`/api/reviews/${reviewId}/vote`, {
      method: 'PUT',
      body: JSON.stringify({ type }),
    }),

  replyToReview: (reviewId, body) =>
    request(`/api/reviews/${reviewId}/replies`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  deleteReply: (replyId) => request(`/api/replies/${replyId}`, { method: 'DELETE' }),
}
