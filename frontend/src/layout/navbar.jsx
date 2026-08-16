import { Link, NavLink, useNavigate } from 'react-router-dom'
import { Gamepad2 } from 'lucide-react'
import { useAuth } from '@/lib/auth.jsx'
import { btnPrimary } from '@/lib/ui.js'

// Barra do topo, em todas as paginas. NavLink marca sozinho o link atual.
export default function Navbar() {
  const { isAuthenticated, username, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  // O item ativo fica ambar com um traco embaixo. Marcar a pagina atual so com
  // "texto mais claro" e fraco num tema escuro, onde a diferenca entre cinza e
  // branco se perde; a cor quente e o traco resolvem de longe.
  const link = ({ isActive }) =>
    'relative text-sm font-medium transition-colors py-1 ' +
    (isActive
      ? 'text-accent after:absolute after:inset-x-0 after:-bottom-0.5 after:h-px after:bg-accent'
      : 'text-slate hover:text-ink')

  return (
    <nav className="sticky top-0 z-30 bg-canvas/80 backdrop-blur-md border-b border-line">
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link
          to="/"
          className="group flex items-center gap-2.5 font-display font-bold text-xl text-ink tracking-tight"
        >
          <span className="grid place-items-center h-8 w-8 bg-accent text-canvas transition-transform duration-300 group-hover:rotate-[-8deg]">
            <Gamepad2 size={18} />
          </span>
          GameLog
        </Link>

        <div className="flex items-center gap-7">
          <NavLink to="/games" className={link}>Catálogo</NavLink>
          {isAuthenticated ? (
            <>
              {/* So pra quem esta logado: recomendacao depende de saber quem e a pessoa */}
              <NavLink to="/recommendations" className={link}>Recomendados</NavLink>
              <NavLink to={`/users/${username}`} className={link}>{username}</NavLink>
              <button onClick={handleLogout} className="text-sm font-medium text-slate hover:text-ink transition-colors cursor-pointer">
                Sair
              </button>
            </>
          ) : (
            <>
              <NavLink to="/login" className={link}>Entrar</NavLink>
              <Link to="/register" className={`${btnPrimary} !px-4 !py-2 text-sm`}>Criar conta</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  )
}
