import { request } from './client.js'

// Perfil publico e os numeros dele. Atendidos pelo monolito.
export const usersApi = {
  getProfile: (username) => request(`/api/users/${username}`),

  // Horas, jogos por status, genero favorito, conquistas e retrospectiva do ano -
  // tudo numa chamada. Sao cinco blocos da tela que leem as mesmas duas tabelas.
  getUserStats: (username) => request(`/api/users/${username}/stats`),

  updateMyProfile: (body) =>
    request('/api/users/me', { method: 'PUT', body: JSON.stringify(body) }),
}
