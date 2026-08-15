import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import { btnPrimary, field, card } from '../ui.js'

export default function RegisterPage() {
  const [form, setForm] = useState({ username: '', email: '', password: '', bio: '' })
  const [error, setError] = useState(null)

  const { login } = useAuth()
  const navigate = useNavigate()

  function update(event) {
    setForm({ ...form, [event.target.name]: event.target.value })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    try {
      const data = await api.register(form)
      login(data.token, data.username)
      navigate('/')
    } catch (err) {
      setError(err.message)
    }
  }

  return (
    <div className="max-w-md mx-auto px-6 py-20">
      <div className={`${card} p-8`}>
        <h1 className="font-display font-extrabold text-2xl text-ink">Criar conta</h1>
        <p className="text-slate text-sm mt-1">Leva menos de um minuto.</p>
        <form onSubmit={handleSubmit} className="flex flex-col gap-3.5 mt-6">
          <input name="username" className={field} placeholder="Usuário" value={form.username} onChange={update} />
          <input name="email" type="email" className={field} placeholder="Email" value={form.email} onChange={update} />
          <input name="password" type="password" className={field} placeholder="Senha (mín. 6)" value={form.password} onChange={update} />
          <textarea name="bio" className={`${field} resize-y`} placeholder="Fala um pouco de você (opcional)" value={form.bio} onChange={update} rows={2} />
          {error && <p className="text-red-500 text-sm font-medium">{error}</p>}
          <button type="submit" className={`${btnPrimary} w-full`}>Criar conta</button>
        </form>
        <p className="text-slate text-sm mt-5">
          Já tem conta? <Link to="/login" className="text-accent font-semibold">Entrar</Link>
        </p>
      </div>
    </div>
  )
}
