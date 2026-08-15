// Camada que fala com o back-end. Toda chamada HTTP do front passa por aqui,
// entao a regra de "como montar a requisicao" fica num lugar so.

// Aponta pro API GATEWAY (porta 8090), e nao mais direto pro back-end na 8080.
//
// Mudou no TP3, e essa unica linha e o que o front precisou saber sobre a
// arquitetura ter virado distribuida. O gateway olha o caminho da URL e decide o
// destino: /api/recommendations/** vai pro microsservico de recomendacoes, todo o
// resto vai pro monolito.
//
// Por que passar tudo pelo gateway em vez de o front conhecer os dois enderecos:
//   - um endereco so aqui; servico novo no futuro nao mexe no front;
//   - CORS resolvido num lugar so (no gateway), nao em cada servico;
//   - as portas internas dos servicos nao ficam expostas ao navegador.
//
// Da pra apontar pra outro lugar com VITE_API_URL - inclusive direto pra 8080, se
// alguem quiser rodar so o monolito, sem a stack distribuida.
const BASE_URL = import.meta.env.VITE_API_URL || 'http://localhost:8090'

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

  // --- Microsservico de recomendacoes (TP3) ---
  // Estas quatro chamadas saem do mesmo request() das de cima e, do ponto de vista
  // do front, sao iguais as outras. So que o gateway as encaminha pra OUTRO
  // processo, com OUTRO banco. Ter conseguido acrescentar um servico ao sistema sem
  // mudar a forma de chamar a API e exatamente o resultado que o gateway entrega.
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
