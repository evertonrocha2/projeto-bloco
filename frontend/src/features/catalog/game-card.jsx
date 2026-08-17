import { Plus } from 'lucide-react'
import StarRating from '@/ui/star-rating.jsx'
import GameCard, { cardActionButton } from '@/ui/game-card.jsx'
import { primaryGenre } from '@/lib/genres.js'

// O card do catalogo, da previa da landing e da busca.
//
// O desenho vem todo da casca (ui/game-card.jsx). O que sobra aqui e o que so
// este card tem: o selo de genero, a nota da comunidade e o "+" de adicionar a
// colecao sem sair da tela.
export default function CatalogGameCard({ game, onQuickAdd }) {
  return (
    <GameCard
      to={`/games/${game.id}`}
      game={game}
      overlayTopLeft={primaryGenre(game)}
      footer={
        <div className="flex items-center gap-2">
          <StarRating value={Math.round(game.averageRating)} />
          <span className="text-xs text-slate">
            {game.reviewCount > 0 ? game.averageRating.toFixed(1) : 'sem nota'}
          </span>
        </div>
      }
      actions={
        onQuickAdd && (
          <button
            onClick={() => onQuickAdd(game)}
            title="Adicionar à coleção"
            className={`${cardActionButton} hover:bg-accent hover:text-canvas`}
          >
            <Plus size={16} />
          </button>
        )
      }
    />
  )
}
