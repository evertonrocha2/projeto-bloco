import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { Clock, Plus } from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth.jsx'
import StarRating from '@/ui/star-rating.jsx'
import GameCover from '@/ui/game-cover.jsx'
import GameCard from '@/ui/game-card.jsx'
import { useReveal } from '@/ui/use-reveal.js'
import Spinner from '@/ui/spinner.jsx'
import AddToCollectionModal from '@/features/collection/add-to-collection-modal.jsx'
import { card, btnPrimary } from '@/lib/ui.js'

const wrap = 'max-w-5xl mx-auto px-6 py-12'

export default function ProfilePage() {
  const { username } = useParams()
  const { username: loggedUser } = useAuth()
  const [profile, setProfile] = useState(null)
  const [collection, setCollection] = useState([])
  const [tab, setTab] = useState('reviews')
  const [showModal, setShowModal] = useState(false)
  const [error, setError] = useState(null)

  // e o proprio perfil de quem esta logado?
  const isOwnProfile = loggedUser && loggedUser === username

  function loadCollection() {
    api.getCollection(username).then(setCollection).catch(() => setCollection([]))
  }

  useEffect(() => {
    setProfile(null)
    setError(null)
    setTab('reviews')
    api.getProfile(username).then(setProfile).catch((erro) => setError(erro.message))
    loadCollection()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [username])

  // Reobserva ao trocar de aba: as listas de avaliações e de coleção são
  // conteúdos diferentes, e cada uma entra animada quando aparece.
  useReveal([profile?.username, tab, collection.length])

  if (error) return <div className={wrap}><p className="text-danger font-medium">{error}</p></div>
  if (!profile) return <div className={wrap}><Spinner /></div>

  const memberSince = new Date(profile.createdAt).toLocaleDateString('pt-BR')

  const tabBtn = (key, label, count) => (
    <button
      onClick={() => setTab(key)}
      className={
        'text-sm font-semibold pb-3 -mb-px border-b-2 transition cursor-pointer ' +
        (tab === key ? 'text-ink border-ink' : 'text-slate border-transparent hover:text-ink')
      }
    >
      {label} <span className="text-slate/70">{count}</span>
    </button>
  )

  return (
    <div className={wrap}>
      <header className="flex flex-wrap items-center gap-5 mb-10">
        <span className="grid place-items-center h-16 w-16 shrink-0 bg-accent text-canvas font-display text-2xl">
          {profile.username.charAt(0).toUpperCase()}
        </span>
        <div>
          <h1 className="text-2xl text-ink">@{profile.username}</h1>
          {profile.bio && <p className="text-slate text-sm mt-0.5">{profile.bio}</p>}
          <p className="text-xs text-slate/80 mt-1">Membro desde {memberSince}</p>
        </div>
      </header>

      <div className="flex gap-6 border-b border-line mb-8">
        {tabBtn('reviews', 'Avaliações', profile.reviews.length)}
        {tabBtn('collection', 'Coleção', collection.length)}
      </div>

      {/* AVALIACOES */}
      {tab === 'reviews' && (
        <>
          {profile.reviews.length === 0 && <p className="text-slate text-sm">Ainda não avaliou nenhum jogo.</p>}
          <ul className="flex flex-col gap-3" data-reveal-group>
            {profile.reviews.map((review) => (
              <li key={review.id} className={`${card} p-4 flex gap-4`}>
                <Link to={`/games/${review.gameId}`} className="shrink-0">
                  <span className="block h-20 w-14 shrink-0 overflow-hidden border border-line">
                    <GameCover game={{ title: review.gameTitle, coverUrl: review.gameCoverUrl }} className="h-full" />
                  </span>
                </Link>
                <div className="min-w-0 flex-1">
                  <div className="flex items-center justify-between gap-3 mb-1">
                    <Link to={`/games/${review.gameId}`} className="font-semibold text-sm text-ink hover:text-accent truncate">{review.gameTitle}</Link>
                    <StarRating value={review.rating} size={14} />
                  </div>
                  <p className="text-slate text-sm leading-relaxed">{review.text}</p>
                </div>
              </li>
            ))}
          </ul>
        </>
      )}

      {/* COLECAO */}
      {tab === 'collection' && (
        <>
          {isOwnProfile && (
            <div className="mb-6">
              <button onClick={() => setShowModal(true)} className={`${btnPrimary} !py-2.5 text-sm`}>
                <Plus size={16} /> Adicionar jogo
              </button>
            </div>
          )}

          {collection.length === 0 ? (
            <p className="text-slate text-sm">A coleção está vazia.</p>
          ) : (
            <div className="grid gap-6 grid-cols-[repeat(auto-fill,minmax(200px,1fr))]" data-reveal-group>
              {collection.map((entry) => (
                <GameCard
                  key={entry.id}
                  to={`/games/${entry.gameId}`}
                  game={{ title: entry.gameTitle, coverUrl: entry.gameCoverUrl }}
                  footer={
                    <div className="flex items-center justify-between">
                      {/* statusLabel vem pronto da API; entry.status e o codigo
                          do enum e mostraria "QUERO_JOGAR" cru na tela. */}
                      <span className="text-xs font-semibold text-accent bg-accent-soft px-2.5 py-0.5">{entry.statusLabel}</span>
                      <span className="inline-flex items-center gap-1 text-xs text-slate">
                        <Clock size={12} /> {entry.hoursPlayed}h
                      </span>
                    </div>
                  }
                />
              ))}
            </div>
          )}
        </>
      )}

      {showModal && (
        <AddToCollectionModal
          onClose={() => setShowModal(false)}
          onAdded={loadCollection}
        />
      )}
    </div>
  )
}
