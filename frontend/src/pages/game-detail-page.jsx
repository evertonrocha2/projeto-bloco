import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { ArrowLeft, Plus, Check } from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth.jsx'
import { parseGenres } from '@/lib/genres.js'
import StarRating from '@/ui/star-rating.jsx'
import GameCover from '@/ui/game-cover.jsx'
import Spinner from '@/ui/spinner.jsx'
import Select from '@/ui/select.jsx'
import ReviewItem from '@/features/reviews/review-item.jsx'
import { btnPrimary, btnGhost, field, card } from '@/lib/ui.js'
import { COLLECTION_STATUSES, DEFAULT_STATUS } from '@/lib/collection-status.js'

const wrap = 'max-w-5xl mx-auto px-6 py-12'

// O Select fala em {value, label}; o modulo de status fala em {code, label}.
// A conversao acontece uma vez, fora do componente, e nao a cada renderizacao.
const STATUS_OPTIONS = COLLECTION_STATUSES.map(({ code, label }) => ({ value: code, label }))

export default function GameDetailPage() {
  const { id } = useParams()
  const { isAuthenticated, username } = useAuth()

  const [game, setGame] = useState(null)
  const [error, setError] = useState(null)

  const [rating, setRating] = useState(0)
  const [text, setText] = useState('')
  const [formError, setFormError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const [hours, setHours] = useState(0)
  // codigo do enum, nao rotulo: e o que a API aceita.
  const [status, setStatus] = useState(DEFAULT_STATUS)
  const [collDone, setCollDone] = useState(false)
  const [collMsg, setCollMsg] = useState(null)
  const [celebrating, setCelebrating] = useState(false)

  function loadGame() {
    api.getGame(id).then(setGame).catch((erro) => setError(erro.message))
  }

  useEffect(() => {
    loadGame()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  async function handleReview(event) {
    event.preventDefault()
    setFormError(null)
    if (rating === 0) {
      setFormError('Escolha uma nota de 1 a 5.')
      return
    }
    setSubmitting(true)
    try {
      await api.createReview(id, { rating, text })
      setRating(0)
      setText('')
      loadGame()
    } catch (err) {
      setFormError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  async function handleCollection(event) {
    event.preventDefault()
    setCollMsg(null)
    try {
      await api.addToCollection({ gameId: Number(id), hoursPlayed: Number(hours), status })
      setCollDone(true)
      setCollMsg('Salvo na sua coleção.')

      // A celebracao acontece no MOMENTO de platinar, e nunca ao carregar a
      // pagina. Uma animacao que toca toda vez que a tela abre deixa de ser
      // comemoracao e vira ruido - e some justamente a diferenca entre "voce
      // acabou de conseguir" e "voce conseguiu algum dia".
      if (status === 'PLATINADO') {
        setCelebrating(true)
      }
    } catch (err) {
      setCollMsg(err.message)
    }
  }

  if (error) return <div className={wrap}><p className="text-danger font-medium">{error}</p></div>
  if (!game) return <div className={wrap}><Spinner /></div>

  const genres = parseGenres(game.genre)

  return (
    <div className={wrap}>
      <Link to="/games" className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate hover:text-ink mb-6">
        <ArrowLeft size={16} /> voltar ao catálogo
      </Link>

      {/* cabecalho */}
      <header className="grid md:grid-cols-[300px_1fr] gap-8 items-start">
        {/* A varredura ambar da platina passa por cima da capa. onAnimationEnd
            desarma o estado, entao ela toca UMA vez - e voltar a platinar o
            mesmo jogo dispara de novo, que e o comportamento esperado. */}
        <div
          className={'relative overflow-hidden' + (celebrating ? ' platinum-sweep' : '')}
          onAnimationEnd={() => setCelebrating(false)}
        >
          <GameCover game={game} className="aspect-[3/4] border border-line" />
        </div>
        <div>
          <h1 className="text-4xl text-ink leading-tight">{game.title}</h1>
          <p className="text-slate mt-1">{game.releaseYear || 's/ data'}</p>
          <div className="flex items-center gap-2 mt-3">
            <StarRating value={Math.round(game.averageRating)} size={18} />
            <span className="text-slate">
              {game.reviewCount > 0 ? `${game.averageRating.toFixed(1)} / 5 · ${game.reviewCount} avaliações` : 'ainda sem avaliações'}
            </span>
          </div>
          <div className="flex flex-wrap gap-2 mt-4">
            {genres.map((genre) => (
              <span key={genre} className="text-xs font-semibold text-slate bg-mist border border-line px-3 py-1">{genre}</span>
            ))}
          </div>
          {game.description && <p className="text-slate leading-relaxed mt-5">{game.description}</p>}
        </div>
      </header>

      {/* adicionar a colecao */}
      {isAuthenticated && (
        <form onSubmit={handleCollection} className={`${card} p-5 mt-10 flex flex-wrap items-end gap-4`}>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-semibold text-ink">Horas jogadas</label>
            <input type="number" min="0" value={hours} onChange={(e) => setHours(e.target.value)} className={`${field} w-32`} />
          </div>
          <div className="flex flex-col gap-1.5">
            <label className="text-sm font-semibold text-ink">Status</label>
            <Select
              value={status}
              onChange={setStatus}
              options={STATUS_OPTIONS}
              className="w-48"
            />
          </div>
          <button type="submit" className={btnPrimary}>
            {collDone ? <Check size={18} /> : <Plus size={18} />}
            {collDone ? 'Atualizar coleção' : 'Adicionar à coleção'}
          </button>
          {collMsg && <span className="text-sm font-medium text-positive">{collMsg}</span>}
        </form>
      )}

      {/* avaliacoes */}
      <h2 className="font-display text-2xl text-ink mt-12 mb-5">
        Avaliações {game.reviewCount > 0 && <span className="text-slate font-medium">({game.reviewCount})</span>}
      </h2>

      {isAuthenticated ? (
        <form onSubmit={handleReview} className={`${card} p-5 mb-8 flex flex-col items-start gap-3`}>
          <label className="text-sm font-semibold text-ink">Sua nota</label>
          <StarRating value={rating} onChange={setRating} size={26} />
          <textarea
            placeholder="O que você achou do jogo?"
            value={text}
            onChange={(e) => setText(e.target.value)}
            rows={3}
            className={`${field} resize-y`}
          />
          {formError && <p className="text-danger text-sm font-medium">{formError}</p>}
          <button type="submit" className={btnPrimary} disabled={submitting}>
            {submitting ? 'Enviando...' : 'Publicar avaliação'}
          </button>
        </form>
      ) : (
        <p className="text-slate mb-8">
          <Link to="/login" className="text-accent font-semibold">Entre</Link> pra deixar sua avaliação.
        </p>
      )}

      {game.reviews.length === 0 && <p className="text-slate">Seja o primeiro a avaliar este jogo.</p>}

      <ul className="flex flex-col gap-3">
        {game.reviews.map((review) => (
          <ReviewItem key={review.id} review={review} currentUser={username} />
        ))}
      </ul>
    </div>
  )
}
