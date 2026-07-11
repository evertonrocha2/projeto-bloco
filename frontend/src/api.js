// Camada que fala com o back-end. Toda chamada HTTP do front passa por aqui,
// entao a regra de "como montar a requisicao" fica num lugar so.

// Por padrao aponta pro back-end local na porta 8080. Da pra trocar criando um
// arquivo .env com VITE_API_URL=... (util se o back-end subir em outra porta).
const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080'

// O token JWT fica no localStorage pra sobreviver a um F5 na pagina.
export function getToken() {
  return localStorage.getItem('gamelog_token')
}

// Funcao central: monta os headers, anexa o token quando existe, e ja trata
// o erro traduzindo o JSON {error: "..."} do back-end numa Error legivel.
async function request(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) }

  const token = getToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }

  const response = await fetch(`${BASE_URL}${path}`, { ...options, headers })

  // 204/sem corpo: nao tenta fazer parse de JSON vazio.
  const text = await response.text()
  const data = text ? JSON.parse(text) : null

  if (!response.ok) {
    const message = data && data.error ? data.error : 'Algo deu errado'
    throw new Error(message)
  }

  return data
}

// Cada funcao abaixo corresponde a um endpoint da API. As paginas chamam estas,
// e nao o fetch direto, pra manter o codigo das telas limpo.
export const api = {
  register: (body) => request('/api/auth/register', { method: 'POST', body: JSON.stringify(body) }),
  login: (body) => request('/api/auth/login', { method: 'POST', body: JSON.stringify(body) }),
  listGames: () => request('/api/games'),
  getGame: (id) => request(`/api/games/${id}`),
  createReview: (gameId, body) =>
    request(`/api/games/${gameId}/reviews`, { method: 'POST', body: JSON.stringify(body) }),
  getProfile: (username) => request(`/api/users/${username}`),
  getCollection: (username) => request(`/api/users/${username}/collection`),
  addToCollection: (body) =>
    request('/api/collection', { method: 'POST', body: JSON.stringify(body) }),
}
