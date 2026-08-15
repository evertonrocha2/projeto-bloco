import { request } from './client.js'

// Perfil publico. Atendido pelo monolito.
export const usersApi = {
  getProfile: (username) => request(`/api/users/${username}`),
}
