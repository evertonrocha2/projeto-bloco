// Regras de apresentacao das notificacoes, separadas do componente pra serem
// testadas sem renderizar nada.

// Pra onde o clique leva: a pagina do jogo, quando o aviso fala de um.
export function notificationLink(notification) {
  return notification?.gameId ? `/games/${notification.gameId}` : null
}

// "agora", "ha 5 min", "ha 3 h", "ha 2 d". Precisao de relogio de parede nao
// ajuda aqui; a pessoa quer saber se e novo.
export function relativeTime(isoDate, now = new Date()) {
  const seconds = Math.max(0, Math.floor((now - new Date(isoDate)) / 1000))
  if (seconds < 60) return 'agora'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `há ${minutes} min`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `há ${hours} h`
  return `há ${Math.floor(hours / 24)} d`
}

// O numero no sino. Acima de 9 vira "9+" pra nao alargar o icone.
export function unreadBadge(unread) {
  if (!unread || unread <= 0) return null
  return unread > 9 ? '9+' : String(unread)
}
