import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import StarRating from '@/ui/star-rating.jsx'
import GameCover from '@/ui/game-cover.jsx'
import { primaryGenre } from '@/lib/genres.js'

// Card de um jogo. Aparece no catalogo, na previa da landing e na colecao.
// Quando recebe onQuickAdd, mostra um botao "+" no canto pra adicionar o jogo a
// colecao sem sair do catalogo. O botao fica FORA do <Link> (e irmao dele) pra
// nao aninhar um <button> dentro de um <a>, que e invalido.
export default function GameCard({ game, onQuickAdd }) {
  return (
    <div className="relative group">
      <Link
        to={`/games/${game.id}`}
        className="block border border-line bg-mist transition-colors hover:border-accent"
      >
        <div className="relative overflow-hidden">
          <GameCover
            game={game}
            className="aspect-[3/4] transition-transform duration-700 group-hover:scale-105"
          />
          <span className="absolute top-0 left-0 text-[0.7rem] font-medium tracking-wide px-2.5 py-1 bg-canvas/90 text-ink border-r border-b border-line backdrop-blur">
            {primaryGenre(game)}
          </span>
        </div>
        <div className="p-4 border-t border-line">
          <h3 className="font-display font-semibold text-ink leading-tight truncate" title={game.title}>
            {game.title}
          </h3>
          <div className="flex items-center gap-2 mt-2">
            <StarRating value={Math.round(game.averageRating)} />
            <span className="text-sm text-slate">
              {game.reviewCount > 0 ? game.averageRating.toFixed(1) : 'sem nota'}
            </span>
          </div>
        </div>
      </Link>

      {onQuickAdd && (
        <button
          onClick={() => onQuickAdd(game)}
          title="Adicionar à coleção"
          className="absolute top-0 right-0 grid place-items-center h-8 w-8 bg-canvas/90 text-ink border-l border-b border-line opacity-0 group-hover:opacity-100 focus-visible:opacity-100 transition hover:bg-accent hover:text-canvas cursor-pointer"
        >
          <Plus size={16} />
        </button>
      )}
    </div>
  )
}
