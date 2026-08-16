import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth.jsx'
import AuthLayout from '@/layout/auth-layout.jsx'
import { btnPrimary, fieldLarge } from '@/lib/ui.js'

export default function RegisterPage() {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [bio, setBio] = useState('')
  const [error, setError] = useState(null)
  const [criando, setCriando] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setCriando(true)
    try {
      const data = await api.register({ username, email, password, bio })
      login(data.token, data.username)
      navigate('/')
    } catch (erro) {
      setError(erro.message)
    } finally {
      setCriando(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="leva um minuto"
      title="Criar conta"
      subtitle="Depois é só marcar os jogos que você já zerou."
      footer={
        <p className="text-sm text-slate">
          Já tem conta?{' '}
          <Link to="/login" className="text-accent font-medium hover:underline">
            Entrar
          </Link>
        </p>
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
        {/* Usuario e email lado a lado: sao curtos, e empilhar tudo num painel
            largo deixaria o formulario comprido a toa. */}
        <div className="grid sm:grid-cols-2 gap-5">
          <label className="flex flex-col gap-2">
            <span className="eyebrow">usuário</span>
            <input
              className={fieldLarge}
              autoComplete="username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </label>

          <label className="flex flex-col gap-2">
            <span className="eyebrow">email</span>
            <input
              className={fieldLarge}
              type="email"
              autoComplete="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
            />
          </label>
        </div>

        <label className="flex flex-col gap-2">
          <span className="eyebrow">senha</span>
          <input
            className={fieldLarge}
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>

        <label className="flex flex-col gap-2">
          <span className="eyebrow">sobre você · opcional</span>
          <textarea
            className={`${fieldLarge} resize-y min-h-20`}
            value={bio}
            onChange={(e) => setBio(e.target.value)}
          />
        </label>

        {error && (
          <p role="alert" className="text-danger text-sm font-medium border-l-2 border-danger pl-3">
            {error}
          </p>
        )}

        <button type="submit" disabled={criando} className={`${btnPrimary} w-full mt-2 py-4`}>
          {criando ? 'Criando...' : <>Criar conta <ArrowRight size={18} /></>}
        </button>
      </form>
    </AuthLayout>
  )
}
