import { request } from './client.js'

// Notificacoes. Atendidas pelo notification-service (TP4), que as monta a partir
// dos eventos do monolito. Tudo aqui exige token, menos o feed da comunidade.
export const notificationsApi = {
  getNotifications: (username, limit = 20) =>
    request(`/api/notifications/${username}?limit=${limit}`),

  markNotificationRead: (username, id) =>
    request(`/api/notifications/${username}/${id}/read`, { method: 'POST' }),

  markAllNotificationsRead: (username) =>
    request(`/api/notifications/${username}/read-all`, { method: 'POST' }),

  getCommunityFeed: (limit = 10) => request(`/api/notifications/feed?limit=${limit}`),
}
