import { request, BASE_URL, getToken } from './client.js'

// Perfil publico e os numeros dele. Atendidos pelo monolito.
export const usersApi = {
  getProfile: (username) => request(`/api/users/${username}`),

  // Horas, jogos por status, genero favorito, conquistas e retrospectiva do ano -
  // tudo numa chamada. Sao cinco blocos da tela que leem as mesmas duas tabelas.
  getUserStats: (username) => request(`/api/users/${username}/stats`),

  updateMyProfile: (body) =>
    request('/api/users/me', { method: 'PUT', body: JSON.stringify(body) }),

  // Envio de imagem. Nao passa pelo request() de propósito: aquele fixa
  // Content-Type: application/json, e multipart precisa que o NAVEGADOR monte o
  // cabeçalho — ele inclui o boundary, que é gerado na hora e que o servidor usa
  // pra separar as partes. Escrever o Content-Type à mão aqui quebraria o parse
  // no servidor com uma mensagem que não aponta pra causa.
  uploadImage: async (file) => {
    const form = new FormData()
    form.append('file', file)

    const response = await fetch(`${BASE_URL}/api/uploads`, {
      method: 'POST',
      headers: getToken() ? { Authorization: `Bearer ${getToken()}` } : {},
      body: form,
    })

    const text = await response.text()
    const data = text ? JSON.parse(text) : null

    if (!response.ok) {
      throw new Error(data && data.error ? data.error : 'Não foi possível enviar a imagem')
    }

    return data.url
  },
}
