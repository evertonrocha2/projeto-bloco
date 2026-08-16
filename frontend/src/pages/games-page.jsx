import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Search, Plus } from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth.jsx'
import { collectGenres, parseGenres } from '@/lib/genres.js'
import GameCard from '@/features/catalog/game-card.jsx'
import Spinner from '@/ui/spinner.jsx'
import AddToCollectionModal from '@/features/collection/add-to-collection-modal.jsx'
import { btnPrimary } from '@/lib/ui.js'

const wrap = 'max-w-6xl mx-auto px-6 py-12'

export default function GamesPage() {
  const { isAuthenticated } = useAuth()
  const [games, setGames] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [search, setSearch] = useState('')

  // popup de adicionar a colecao; addGame guarda o jogo pre-selecionado (ou null)
  const [showModal, setShowModal] = useState(false)
  const [addGame, setAddGame] = useState(null)

  function openAdd(game) {
    setAddGame(game || null)
    setShowModal(true)
  }

  const [searchParams] = useSearchParams()
  const [activeGenre, setActiveGenre] = useState(searchParams.get('genero') || 'Todos')

  useEffect(() => {
    api.listGames()
      .then(setGames)
      .catch((erro) => setError(erro.message))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    setActiveGenre(searchParams.get('genero') || 'Todos')
  }, [searchParams])

  // Os filtros disponiveis saem do proprio catalogo: nao existe lista fixa de
  // generos em lugar nenhum, ela e descoberta a partir dos jogos carregados.
  const genres = useMemo(
    () => ['Todos', ...collectGenres(games).sort()],
    [games],
  )

  const matchesSearch = (game) =>
    game.title.toLowerCase().includes(search.toLowerCase())

  const matchesGenre = (game) =>
    activeGenre === 'Todos' || parseGenres(game.genre).includes(activeGenre)

  const filtered = games.filter((game) => matchesSearch(game) && matchesGenre(game))

  if (loading) return <div className={wrap}><Spinner /></div>
  if (error) return <div className={wrap}><p className="text-danger font-medium">{error}</p></div>

  return (
    <div className={wrap}>
      <div className="flex flex-wrap items-end justify-between gap-4 pb-8 mb-8 border-b border-line">
        <div>
          <p className="eyebrow">{games.length} títulos</p>
          <h1 className="font-display font-extrabold text-4xl sm:text-5xl text-ink leading-[1.02] mt-3">
            Catálogo
          </h1>
          <p className="text-slate mt-3 max-w-md leading-relaxed">
            Abra um jogo pra ler o que a galera achou, deixar sua nota ou jogar ele
            na sua coleção.
          </p>
        </div>
        {isAuthenticated && (
          <button onClick={() => openAdd(null)} className={`${btnPrimary} !py-2.5 text-sm`}>
            <Plus size={16} /> Adicionar à coleção
          </button>
        )}
      </div>

      <div className="relative max-w-md mb-6">
        <Search size={18} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-slate/60" />
        <input
          className="w-full bg-canvas border border-line pl-10 pr-4 py-2.5 text-sm text-ink outline-none transition placeholder:text-slate/60 focus:border-accent focus:ring-4 focus:ring-accent/10"
          placeholder="Buscar por nome..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
      </div>

      <div className="flex flex-wrap gap-2 mb-9">
        {genres.map((genre) => {
          const active = genre === activeGenre
          return (
            <button
              key={genre}
              onClick={() => setActiveGenre(genre)}
              className={
                'text-sm font-medium px-4 py-2 border transition-colors cursor-pointer ' +
                (active
                  ? 'bg-accent text-canvas border-accent'
                  : 'bg-mist text-slate border-line hover:border-accent hover:text-accent')
              }
            >
              {genre}
            </button>
          )
        })}
      </div>

      {filtered.length === 0 ? (
        <p className="text-slate">Nenhum jogo encontrado com esse filtro.</p>
      ) : (
        <div className="grid gap-6 grid-cols-[repeat(auto-fill,minmax(220px,1fr))]">
          {filtered.map((game) => (
            <GameCard key={game.id} game={game} onQuickAdd={isAuthenticated ? openAdd : undefined} />
          ))}
        </div>
      )}

      {showModal && (
        <AddToCollectionModal
          initialGame={addGame}
          onClose={() => setShowModal(false)}
          onAdded={() => {}}
        />
      )}
    </div>
  )
}
