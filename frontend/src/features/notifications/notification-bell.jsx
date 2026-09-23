import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Bell } from 'lucide-react'
import { api } from '@/lib/api/index.js'
import { notificationLink, relativeTime, unreadBadge } from './notification-text.js'

// Sino de notificacoes no topo (TP4).
//
// As notificacoes nascem de eventos: alguem responde sua avaliacao no monolito, o
// evento review.replied sai pelo RabbitMQ e o notification-service grava o aviso.
// A tela so consulta. Consulta a cada 30s em vez de manter conexao aberta - pra
// avisos de rede social, meio minuto de atraso nao muda nada, e poupa um
// WebSocket atravessando o gateway.
const POLL_MS = 30_000

export default function NotificationBell({ username }) {
  const [data, setData] = useState(null)
  const [open, setOpen] = useState(false)
  const navigate = useNavigate()
  const boxRef = useRef(null)

  const load = useCallback(() => {
    api.getNotifications(username).then(setData).catch(() => {
      // Servico de notificacoes fora do ar nao pode quebrar a barra do topo:
      // o sino so fica sem numero.
    })
  }, [username])

  useEffect(() => {
    load()
    const timer = setInterval(load, POLL_MS)
    return () => clearInterval(timer)
  }, [load])

  useEffect(() => {
    if (!open) return undefined
    function onClickOutside(event) {
      if (boxRef.current && !boxRef.current.contains(event.target)) setOpen(false)
    }
    document.addEventListener('mousedown', onClickOutside)
    return () => document.removeEventListener('mousedown', onClickOutside)
  }, [open])

  async function handleSelect(notification) {
    setOpen(false)
    if (!notification.read) {
      await api.markNotificationRead(username, notification.id).catch(() => {})
      load()
    }
    const link = notificationLink(notification)
    if (link) navigate(link)
  }

  async function handleReadAll() {
    await api.markAllNotificationsRead(username).catch(() => {})
    load()
  }

  const badge = unreadBadge(data?.unread)
  const items = data?.items ?? []

  return (
    <div className="relative" ref={boxRef}>
      <button
        onClick={() => setOpen((value) => !value)}
        className="relative text-slate hover:text-ink transition-colors cursor-pointer py-1"
        aria-label={badge ? `Notificações (${badge} novas)` : 'Notificações'}
        aria-expanded={open}
      >
        <Bell size={18} />
        {badge && (
          <span className="absolute -top-1 -right-2 min-w-4 h-4 px-1 grid place-items-center bg-accent text-canvas text-[0.6rem] font-semibold">
            {badge}
          </span>
        )}
      </button>

      {open && (
        <div className="absolute right-0 mt-3 w-80 bg-mist border border-line shadow-lg z-40">
          <div className="flex items-center justify-between px-4 py-3 border-b border-line">
            <p className="eyebrow">notificações</p>
            {badge && (
              <button onClick={handleReadAll} className="text-xs text-slate hover:text-accent cursor-pointer">
                marcar todas como lidas
              </button>
            )}
          </div>

          {items.length === 0 ? (
            <p className="px-4 py-6 text-sm text-slate">Nada por aqui ainda.</p>
          ) : (
            <ul className="max-h-96 overflow-y-auto">
              {items.map((notification) => (
                <li key={notification.id}>
                  <button
                    onClick={() => handleSelect(notification)}
                    className={
                      'w-full text-left px-4 py-3 border-b border-line last:border-b-0 hover:bg-canvas/60 transition-colors cursor-pointer ' +
                      (notification.read ? 'text-slate' : 'text-ink')
                    }
                  >
                    <span className="flex items-start gap-2">
                      {!notification.read && (
                        <span aria-hidden="true" className="mt-1.5 block h-1.5 w-1.5 shrink-0 rounded-full bg-accent" />
                      )}
                      <span className="text-sm leading-snug">{notification.message}</span>
                    </span>
                    <span className="block text-[0.7rem] text-slate mt-1">
                      {relativeTime(notification.createdAt)}
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}
