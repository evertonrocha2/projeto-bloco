import { request } from './client.js'

// Catalogo e avaliacoes. Atendidos pelo monolito.
//
// createReview mora aqui, e nao num arquivo de reviews: a rota e
// /api/games/{id}/reviews, ou seja, avaliar e uma operacao SOBRE um jogo. Agrupar
// por rota mantem o mapa mental igual ao da API.
export const gamesApi = {
  listGames: () => request('/api/games'),
  getGame: (id) => request(`/api/games/${id}`),
  createReview: (gameId, body) =>
    request(`/api/games/${gameId}/reviews`, { method: 'POST', body: JSON.stringify(body) }),
}
