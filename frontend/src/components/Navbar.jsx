import { Link, NavLink, useNavigate } from 'react-router-dom'
import { Gamepad2 } from 'lucide-react'
import { useAuth } from '../auth.jsx'
import { btnPrimary } from '../ui.js'

// Barra do topo, em todas as paginas. NavLink marca sozinho o link atual.
export default function Navbar() {
  const { isAuthenticated, username, logout } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/')
  }

  const link = ({ isActive }) =>
    'text-sm font-semibold transition ' + (isActive ? 'text-ink' : 'text-slate hover:text-ink')

  return (
    <nav className="sticky top-0 z-30 bg-canvas/85 backdrop-blur border-b border-line">
      <div className="max-w-6xl mx-auto px-6 h-16 flex items-center justify-between">
        <Link to="/" className="flex items-center gap-2 font-display font-extrabold text-xl text-ink">
          <span className="grid place-items-center h-8 w-8 rounded-lg bg-ink text-white">
            <Gamepad2 size={18} />
          </span>
          GameLog
        </Link>

        <div className="flex items-center gap-6">
          <NavLink to="/games" className={link}>Catálogo</NavLink>
          {isAuthenticated ? (
            <>
              <NavLink to={`/users/${username}`} className={link}>{username}</NavLink>
              <button onClick={handleLogout} className="text-sm font-semibold text-slate hover:text-ink transition cursor-pointer">
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
