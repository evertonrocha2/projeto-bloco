import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth.jsx'
import { btnPrimary, field, card } from '@/lib/ui.js'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)

  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    try {
      const data = await api.login({ username, password })
      login(data.token, data.username)
      navigate('/')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="max-w-md mx-auto px-6 py-20">
      <div className={`${card} p-8`}>
        <h1 className="font-display font-bold text-2xl text-ink">Entrar</h1>
        <p className="text-slate text-sm mt-1">Bom te ver de novo.</p>
        <form onSubmit={handleSubmit} className="flex flex-col gap-4 mt-6">
          <input className={field} placeholder="Usuário" value={username} onChange={(e) => setUsername(e.target.value)} />
          <input className={field} type="password" placeholder="Senha" value={password} onChange={(e) => setPassword(e.target.value)} />
          {error && <p className="text-danger text-sm font-medium">{error}</p>}
          <button type="submit" className={`${btnPrimary} w-full`}>Entrar</button>
        </form>
        <p className="text-slate text-sm mt-5">
          Não tem conta? <Link to="/register" className="text-accent font-semibold">Criar uma</Link>
        </p>
        <p className="text-sm text-slate mt-4 bg-mist border border-line px-4 py-2.5">
          Conta de teste: <strong className="text-ink">demo</strong> / <strong className="text-ink">demo123</strong>
        </p>
      </div>
    </div>
  )
}
