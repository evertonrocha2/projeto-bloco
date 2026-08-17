import { request } from './client.js'

// Listas tematicas. Atendidas pelo monolito.
//
// As rotas de item carregam o id da lista alem do id do item. Nao e redundancia:
// e o que permite ao servidor conferir que o item pertence aquela lista.
export const listsApi = {
  getUserLists: (username) => request(`/api/users/${username}/lists`),

  getListsByTag: (tag) => request(`/api/lists?tag=${encodeURIComponent(tag)}`),

  getList: (listId) => request(`/api/lists/${listId}`),

  createList: (body) => request('/api/lists', { method: 'POST', body: JSON.stringify(body) }),

  updateList: (listId, body) =>
    request(`/api/lists/${listId}`, { method: 'PUT', body: JSON.stringify(body) }),

  deleteList: (listId) => request(`/api/lists/${listId}`, { method: 'DELETE' }),

  addListItem: (listId, body) =>
    request(`/api/lists/${listId}/items`, { method: 'POST', body: JSON.stringify(body) }),

  updateListItemNote: (listId, itemId, note) =>
    request(`/api/lists/${listId}/items/${itemId}`, {
      method: 'PUT',
      body: JSON.stringify({ note }),
    }),

  removeListItem: (listId, itemId) =>
    request(`/api/lists/${listId}/items/${itemId}`, { method: 'DELETE' }),
}
