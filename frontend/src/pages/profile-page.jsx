import { useEffect, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { Clock, Plus, Pencil } from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth.jsx'
import StarRating from '@/ui/star-rating.jsx'
import GameCover from '@/ui/game-cover.jsx'
import GameCard from '@/ui/game-card.jsx'
import { useReveal } from '@/ui/use-reveal.js'
import Spinner from '@/ui/spinner.jsx'
import AddToCollectionModal from '@/features/collection/add-to-collection-modal.jsx'
import ListEditor from '@/features/lists/list-editor.jsx'
import ListCard from '@/features/lists/list-card.jsx'
import StatsStrip from '@/features/profile/stats-strip.jsx'
import AchievementsRow from '@/features/profile/achievements-row.jsx'
import YearInReview from '@/features/profile/year-in-review.jsx'
import EditProfileModal from '@/features/profile/edit-profile-modal.jsx'
import { COLLECTION_STATUSES } from '@/lib/collection-status.js'
import { card, btnPrimary, btnGhost } from '@/lib/ui.js'
import { resolveImageUrl } from '@/lib/image-url.js'

const wrap = 'max-w-5xl mx-auto px-6 py-12'

// "todos" nao e um status do enum: e a ausencia de filtro. Fica junto das abas
// porque na tela ele ocupa o mesmo lugar que elas.
const TODOS = 'TODOS'

export default function ProfilePage() {
  const { username } = useParams()
  const { username: loggedUser } = useAuth()

  const [profile, setProfile] = useState(null)
  const [collection, setCollection] = useState([])
  const [lists, setLists] = useState([])
  const [stats, setStats] = useState(null)
  const [tab, setTab] = useState('reviews')
  const [statusTab, setStatusTab] = useState(TODOS)
  const [showAdd, setShowAdd] = useState(false)
  const [showNewList, setShowNewList] = useState(false)
  const [showEdit, setShowEdit] = useState(false)
  const [error, setError] = useState(null)
  // Imagens que nao carregaram. Reiniciadas ao trocar de perfil e ao salvar uma
  // imagem nova - senao um endereco corrigido continuaria escondido.
  const [avatarBroken, setAvatarBroken] = useState(false)
  const [bannerBroken, setBannerBroken] = useState(false)

  // e o proprio perfil de quem esta logado?
  const isOwnProfile = loggedUser && loggedUser === username

  function loadCollection() {
    api.getCollection(username).then(setCollection).catch(() => setCollection([]))
    // As contagens das abas vem daqui, entao os numeros tem que ser recarregados
    // junto da colecao - senao a aba diz 3 e a grade mostra 4.
    api.getUserStats(username).then(setStats).catch(() => setStats(null))
  }

  function loadLists() {
    api.getUserLists(username).then(setLists).catch(() => setLists([]))
  }

  useEffect(() => {
    setProfile(null)
    setError(null)
    setTab('reviews')
    setStatusTab(TODOS)
    setAvatarBroken(false)
    setBannerBroken(false)
    api.getProfile(username).then(setProfile).catch((erro) => setError(erro.message))
    loadCollection()
    loadLists()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [username])

  // Reobserva ao trocar de aba: avaliações, coleção e listas são conteúdos
  // diferentes, e cada um entra animado quando aparece.
  useReveal([profile?.username, tab, statusTab, collection.length, lists.length])

  if (error) return <div className={wrap}><p className="text-danger font-medium">{error}</p></div>
  if (!profile) return <div className={wrap}><Spinner /></div>

  const memberSince = new Date(profile.createdAt).toLocaleDateString('pt-BR')

  // Filtra no cliente: a coleção inteira já veio numa requisição, e uma ida ao
  // servidor por clique de aba seria recortar dado que o navegador já tem.
  const visibleCollection = statusTab === TODOS
    ? collection
    : collection.filter((entry) => entry.status === statusTab)

  const tabBtn = (key, label, count) => (
    <button
      key={key}
      onClick={() => setTab(key)}
      className={
        'text-sm font-semibold pb-3 -mb-px border-b-2 transition cursor-pointer ' +
        (tab === key ? 'text-ink border-ink' : 'text-slate border-transparent hover:text-ink')
      }
    >
      {label} <span className="text-slate/70">{count}</span>
    </button>
  )

  // As abas de status. Os totais vêm do servidor (stats), a mesma fonte que a
  // faixa de números — dois cálculos diferentes lado a lado poderiam discordar.
  const statusBtn = (code, label, count) => (
    <button
      key={code}
      onClick={() => setStatusTab(code)}
      className={
        'text-xs font-medium px-3 py-1.5 border transition cursor-pointer ' +
        (statusTab === code
          ? 'border-accent text-accent bg-accent-soft'
          : 'border-line text-slate hover:text-ink hover:border-slate/50')
      }
    >
      {label} <span className="tabular-nums opacity-70">{count}</span>
    </button>
  )

  return (
    <div className={wrap}>
      {/* Capa do perfil. Sangra até a borda da coluna e o texto vem por baixo,
          não por cima: sobreposto, o nome dependeria do brilho da imagem que a
          pessoa escolheu pra continuar legível.

          onError some com o bloco inteiro. A URL é colada por quem edita o
          perfil, e endereço que responde 404 é caso comum — um retângulo com
          ícone de imagem partida no topo da página faz o perfil parecer
          quebrado, quando o certo é ele apenas não ter capa. */}
      {profile.bannerUrl && !bannerBroken && (
        <div className="relative aspect-[5/1] overflow-hidden border border-line mb-6">
          <img
            src={resolveImageUrl(profile.bannerUrl)}
            alt=""
            onError={() => setBannerBroken(true)}
            className="h-full w-full object-cover"
            style={{ filter: 'brightness(0.5) saturate(0.9)' }}
          />
        </div>
      )}

      <header className="flex flex-wrap items-start gap-5 mb-8">
        {/* Avatar quebrado cai na inicial em fundo âmbar, que é o que o perfil
            já desenhava antes de existir avatar. */}
        {profile.avatarUrl && !avatarBroken ? (
          <img
            src={resolveImageUrl(profile.avatarUrl)}
            alt=""
            onError={() => setAvatarBroken(true)}
            className="h-16 w-16 shrink-0 object-cover border border-line"
          />
        ) : (
          <span className="grid place-items-center h-16 w-16 shrink-0 bg-accent text-canvas font-display text-2xl">
            {profile.username.charAt(0).toUpperCase()}
          </span>
        )}

        <div className="min-w-0 flex-1">
          <div className="flex items-start justify-between gap-4 flex-wrap">
            <div className="min-w-0">
              <h1 className="text-2xl text-ink">@{profile.username}</h1>
              {profile.bio && <p className="text-slate text-sm mt-0.5">{profile.bio}</p>}
              <p className="text-xs text-slate/80 mt-1">Membro desde {memberSince}</p>
            </div>

            {isOwnProfile && (
              <button onClick={() => setShowEdit(true)} className={`${btnGhost} !px-4 !py-2 text-sm`}>
                <Pencil size={14} /> Editar perfil
              </button>
            )}
          </div>

          {stats && stats.achievements.length > 0 && (
            <div className="mt-3">
              <AchievementsRow codes={stats.achievements} />
            </div>
          )}
        </div>
      </header>

      <div className="flex flex-col gap-5 mb-10">
        <StatsStrip stats={stats} />
        <YearInReview stats={stats} />
      </div>

      <div className="flex gap-6 border-b border-line mb-8">
        {tabBtn('reviews', 'Avaliações', profile.reviews.length)}
        {tabBtn('collection', 'Coleção', collection.length)}
        {tabBtn('lists', 'Listas', lists.length)}
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

                  {/* O placar da avaliação aqui é só leitura: votar acontece na
                      página do jogo, onde a conversa inteira está visível. */}
                  {review.social && (review.social.positiveVotes > 0 || review.social.negativeVotes > 0) && (
                    <p className="text-xs text-slate/70 mt-1.5 tabular-nums">
                      {review.social.positiveVotes} acharam útil
                      {review.social.negativeVotes > 0 && ` · ${review.social.negativeVotes} não`}
                    </p>
                  )}
                </div>
              </li>
            ))}
          </ul>
        </>
      )}

      {/* COLECAO */}
      {tab === 'collection' && (
        <>
          <div className="flex flex-wrap items-center gap-2 mb-6">
            {statusBtn(TODOS, 'Todos', collection.length)}
            {COLLECTION_STATUSES.map(({ code, label }) =>
              statusBtn(code, label, stats?.countByStatus?.[code] ?? 0))}

            {isOwnProfile && (
              <button onClick={() => setShowAdd(true)} className={`${btnPrimary} !px-4 !py-1.5 text-xs ml-auto`}>
                <Plus size={14} /> Adicionar jogo
              </button>
            )}
          </div>

          {visibleCollection.length === 0 ? (
            <p className="text-slate text-sm">
              {statusTab === TODOS
                ? 'A coleção está vazia.'
                : 'Nenhum jogo com esse status ainda.'}
            </p>
          ) : (
            <div className="grid gap-6 grid-cols-[repeat(auto-fill,minmax(200px,1fr))]" data-reveal-group>
              {visibleCollection.map((entry) => (
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

      {/* LISTAS */}
      {tab === 'lists' && (
        <>
          {isOwnProfile && (
            <div className="mb-6">
              <button onClick={() => setShowNewList(true)} className={`${btnPrimary} !py-2.5 text-sm`}>
                <Plus size={16} /> Nova lista
              </button>
            </div>
          )}

          {lists.length === 0 ? (
            <p className="text-slate text-sm">
              {isOwnProfile
                ? 'Você ainda não montou nenhuma lista.'
                : 'Nenhuma lista pública por aqui.'}
            </p>
          ) : (
            <div className="grid gap-5 grid-cols-[repeat(auto-fill,minmax(280px,1fr))]" data-reveal-group>
              {lists.map((list) => <ListCard key={list.id} list={list} />)}
            </div>
          )}
        </>
      )}

      {showAdd && (
        <AddToCollectionModal onClose={() => setShowAdd(false)} onAdded={loadCollection} />
      )}

      {showNewList && (
        <ListEditor onClose={() => setShowNewList(false)} onSaved={loadLists} />
      )}

      {showEdit && (
        <EditProfileModal
          profile={profile}
          onClose={() => setShowEdit(false)}
          onSaved={(atualizado) => {
            // Zera as marcas de "quebrada": a pessoa acabou de trocar as
            // imagens, e manter a marca antiga esconderia um endereco novo que
            // funciona.
            setAvatarBroken(false)
            setBannerBroken(false)
            setProfile({ ...profile, ...atualizado })
          }}
        />
      )}
    </div>
  )
}
