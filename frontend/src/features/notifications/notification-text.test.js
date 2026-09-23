import { describe, it, expect } from 'vitest'
import { notificationLink, relativeTime, unreadBadge } from './notification-text.js'

describe('notificationLink', () => {
  it('leva pra pagina do jogo citado', () => {
    expect(notificationLink({ gameId: 42 })).toBe('/games/42')
  })

  it('boas-vindas nao tem destino', () => {
    expect(notificationLink({ gameId: null })).toBeNull()
  })
})

describe('relativeTime', () => {
  const now = new Date('2026-09-20T12:00:00Z')

  it('menos de um minuto e agora', () => {
    expect(relativeTime('2026-09-20T11:59:30Z', now)).toBe('agora')
  })

  it('minutos, horas e dias', () => {
    expect(relativeTime('2026-09-20T11:55:00Z', now)).toBe('há 5 min')
    expect(relativeTime('2026-09-20T09:00:00Z', now)).toBe('há 3 h')
    expect(relativeTime('2026-09-18T12:00:00Z', now)).toBe('há 2 d')
  })

  it('relogio adiantado no servidor nao vira tempo negativo', () => {
    expect(relativeTime('2026-09-20T12:00:10Z', now)).toBe('agora')
  })
})

describe('unreadBadge', () => {
  it('some quando nao ha nada novo', () => {
    expect(unreadBadge(0)).toBeNull()
    expect(unreadBadge(undefined)).toBeNull()
  })

  it('limita a 9+', () => {
    expect(unreadBadge(3)).toBe('3')
    expect(unreadBadge(12)).toBe('9+')
  })
})
