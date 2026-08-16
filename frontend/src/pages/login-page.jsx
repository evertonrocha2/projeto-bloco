import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth.jsx'
import AuthLayout from '@/layout/auth-layout.jsx'
import { btnPrimary, fieldLarge } from '@/lib/ui.js'

export default function LoginPage() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [entrando, setEntrando] = useState(false)

  const { login } = useAuth()
  const navigate = useNavigate()

  async function handleSubmit(event) {
    event.preventDefault()
    setError(null)
    setEntrando(true)
    try {
      const data = await api.login({ username, password })
      login(data.token, data.username)
      navigate('/')
    } catch (erro) {
      setError(erro.message)
    } finally {
      setEntrando(false)
    }
  }

  return (
    <AuthLayout
      eyebrow="bem-vindo de volta"
      title="Entrar"
      subtitle="Sua estante está onde você deixou."
      footer={
        <p className="text-sm text-slate">
          Não tem conta?{' '}
          <Link to="/register" className="text-accent font-medium hover:underline">
            Criar uma agora
          </Link>
        </p>
      }
    >
      <form onSubmit={handleSubmit} className="flex flex-col gap-5">
        <label className="flex flex-col gap-2">
          <span className="eyebrow">usuário</span>
          <input
            className={fieldLarge}
            placeholder="seu nome de usuário"
            autoComplete="username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
          />
        </label>

        <label className="flex flex-col gap-2">
          <span className="eyebrow">senha</span>
          <input
            className={fieldLarge}
            type="password"
            placeholder="••••••••"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>

        {/* role="alert" pra que leitor de tela anuncie o erro assim que ele
            aparece, em vez de a pessoa so descobrir tabulando de volta. */}
        {error && (
          <p role="alert" className="text-danger text-sm font-medium border-l-2 border-danger pl-3">
            {error}
          </p>
        )}

        <button type="submit" disabled={entrando} className={`${btnPrimary} w-full mt-2 py-4`}>
          {entrando ? 'Entrando...' : <>Entrar <ArrowRight size={18} /></>}
        </button>
      </form>

      {/* A conta de teste existe pra quem esta avaliando o projeto entrar sem
          precisar criar cadastro. */}
      <div className="mt-7 pt-6 border-t border-line">
        <p className="eyebrow">conta de demonstração</p>
        <p className="text-sm text-slate mt-2">
          <strong className="text-ink font-medium">demo</strong>
          <span className="mx-2 text-line">/</span>
          <strong className="text-ink font-medium">demo123</strong>
        </p>
      </div>
    </AuthLayout>
  )
}
