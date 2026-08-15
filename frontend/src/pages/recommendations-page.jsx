import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { RefreshCw, Sparkles } from 'lucide-react'
import { api } from '../api.js'
import { useAuth } from '../auth.jsx'
import RecommendationCard from '../components/RecommendationCard.jsx'
import TasteProfileChart from '../components/TasteProfileChart.jsx'
import ServiceStatusBadge from '../components/ServiceStatusBadge.jsx'
import Spinner from '../components/Spinner.jsx'
import { btnPrimary, btnGhost, card } from '../ui.js'

const wrap = 'max-w-6xl mx-auto px-6 py-12'

// A tela do microsservico de recomendacoes.
//
// Tudo aqui passa pelo gateway (8090), que encaminha /api/recommendations/** pro
// microsservico. Do ponto de vista deste componente e uma chamada de API como
// qualquer outra - a divisao em dois processos fica invisivel, que e justamente o
// que o gateway resolve.
export default function RecommendationsPage() {
  const { isAuthenticated, username } = useAuth()

  const [data, setData] = useState(null)
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [feedbackBusy, setFeedbackBusy] = useState(false)
  const [error, setError] = useState(null)

  // As duas chamadas vao juntas porque a tela mostra as duas coisas lado a lado: a
  // lista e o perfil que a explica. Promise.all pra nao esperar uma depois da
  // outra.
  const load = useCallback(async () => {
    if (!username) return

    try {
      const [recommendations, tasteProfile] = await Promise.all([
        api.getRecommendations(username),
        api.getTasteProfile(username),
      ])
      setData(recommendations)
      setProfile(tasteProfile)
      setError(null)
    } catch (e) {
      setError(e.message)
    } finally {
      setLoading(false)
    }
  }, [username])

  useEffect(() => {
    load()
  }, [load])

  async function handleRefresh() {
    setRefreshing(true)
    try {
      const recommendations = await api.refreshRecommendations(username)
      setData(recommendations)
      // O perfil pode ter mudado junto (novas avaliacoes, feedback novo).
      setProfile(await api.getTasteProfile(username))
      setError(null)
    } catch (e) {
      setError(e.message)
    } finally {
      setRefreshing(false)
    }
  }

  async function handleFeedback(gameId, verdict) {
    setFeedbackBusy(true)

    // Tira o card da tela na hora, sem esperar a resposta. O servidor faz o mesmo
    // (remove a recomendacao do lote), entao a tela nao esta mentindo - so nao
    // esta esperando pra contar. Se der erro, o recarregamento abaixo devolve o
    // estado real.
    setData((atual) => ({
      ...atual,
      items: atual.items.filter((item) => item.gameId !== gameId),
    }))

    try {
      await api.sendRecommendationFeedback(username, { gameId, verdict })
      // Recarrega o perfil: um "gostei" reforca o genero daquele jogo, e o grafico
      // deve refletir isso.
      setProfile(await api.getTasteProfile(username))
    } catch (e) {
      setError(e.message)
      await load()
    } finally {
      setFeedbackBusy(false)
    }
  }

  // Recomendacao e pessoal: sem saber quem e a pessoa, nao ha o que mostrar.
  if (!isAuthenticated) {
    return (
      <div className={wrap}>
        <div className={`${card} p-8 text-center max-w-lg mx-auto`}>
          <span className="grid place-items-center h-12 w-12 rounded-xl bg-accent/10 text-accent mx-auto">
            <Sparkles size={22} />
          </span>
          <h1 className="font-display font-extrabold text-2xl text-ink mt-4">
            Recomendações são pessoais
          </h1>
          <p className="text-slate mt-2">
            Entre na sua conta pra ver jogos escolhidos a partir do que você avaliou e
            tem na coleção.
          </p>
          <Link to="/login" className={`${btnPrimary} mt-6`}>Entrar</Link>
        </div>
      </div>
    )
  }

  if (loading) return <div className={wrap}><Spinner /></div>

  return (
    <div className={wrap}>
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <h1 className="font-display font-extrabold text-3xl text-ink">Recomendados pra você</h1>
            {/* Deixa visivel se os dados vieram de um calculo novo ou do lote salvo */}
            {data && <ServiceStatusBadge stale={data.stale} />}
          </div>
          <p className="text-slate mt-1.5">
            Escolhidos pelo microsserviço de recomendações a partir dos gêneros que você
            costuma gostar.
          </p>
        </div>

        <button onClick={handleRefresh} disabled={refreshing} className={`${btnGhost} !py-2.5 text-sm`}>
          <RefreshCw size={16} className={refreshing ? 'animate-spin' : ''} />
          {refreshing ? 'Recalculando...' : 'Recalcular'}
        </button>
      </div>

      {error && <p className="text-red-500 font-medium mt-6">{error}</p>}

      {data?.stale && (
        <div className="mt-6 rounded-2xl border border-amber-200 bg-amber-50 px-5 py-4">
          <p className="text-sm text-amber-800">
            <strong className="font-semibold">Modo degradado.</strong> O serviço de
            catálogo não respondeu, então estas indicações vêm do último cálculo salvo
            no banco do microsserviço. A tela continua funcionando — o que você vê pode
            estar desatualizado.
          </p>
        </div>
      )}

      <div className="grid lg:grid-cols-[1fr_320px] gap-8 mt-8">
        <div>
          {data?.items?.length === 0 ? (
            <div className={`${card} p-8`}>
              <h2 className="font-display font-bold text-ink">Nada pra indicar agora</h2>
              <p className="text-slate mt-2">
                {data.stale
                  ? 'Não foi possível falar com o catálogo e não há um cálculo anterior salvo. Tente recalcular em instantes.'
                  : 'Você já conhece tudo o que está no catálogo, ou descartou o resto. Avalie mais jogos pra gente encontrar coisas novas.'}
              </p>
              <Link to="/games" className={`${btnGhost} mt-5 !py-2.5 text-sm`}>Ver catálogo</Link>
            </div>
          ) : (
            <div className="grid gap-6 grid-cols-[repeat(auto-fill,minmax(220px,1fr))]">
              {data?.items?.map((item) => (
                <RecommendationCard
                  key={item.gameId}
                  item={item}
                  onFeedback={handleFeedback}
                  busy={feedbackBusy}
                />
              ))}
            </div>
          )}
        </div>

        <aside className="lg:sticky lg:top-24 h-fit">
          <TasteProfileChart genres={profile?.genres} />

          <p className="text-xs text-slate mt-4 leading-relaxed">
            Cada jogo recebe uma pontuação de até 5,0: parte vem da afinidade com os
            seus gêneros, parte da nota média da comunidade. Jogos que você já avaliou,
            já tem na coleção ou descartou não aparecem.
          </p>
        </aside>
      </div>
    </div>
  )
}
