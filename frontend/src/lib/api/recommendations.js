import { request } from './client.js'

// Recomendacoes. Atendidas pelo MICROSSERVICO (TP3), nao pelo monolito.
//
// Este arquivo existir separado espelha, no front, a divisao que existe no
// back-end: sao outro processo e outro banco. Do ponto de vista do codigo daqui a
// chamada e igual as outras - e isso e exatamente o que o gateway resolve, ja que
// o front continua conhecendo um unico endereco.
export const recommendationsApi = {
  getRecommendations: (username) => request(`/api/recommendations/${username}`),

  // Recalcula do zero. Precisa de token (o gateway barra sem ele).
  refreshRecommendations: (username) =>
    request(`/api/recommendations/${username}/refresh`, { method: 'POST' }),

  // verdict: 'LIKED' ou 'DISMISSED'.
  sendRecommendationFeedback: (username, body) =>
    request(`/api/recommendations/${username}/feedback`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),

  // O perfil de gosto calculado - e o que deixa a recomendacao explicavel.
  getTasteProfile: (username) => request(`/api/recommendations/${username}/taste-profile`),
}
