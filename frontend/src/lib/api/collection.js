import { request } from './client.js'

// Colecao pessoal de jogos. Atendida pelo monolito.
export const collectionApi = {
  getCollection: (username) => request(`/api/users/${username}/collection`),
  addToCollection: (body) =>
    request('/api/collection', { method: 'POST', body: JSON.stringify(body) }),
}
