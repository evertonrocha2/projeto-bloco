import { Link } from 'react-router-dom'
import { Plus } from 'lucide-react'
import StarRating from './StarRating.jsx'

// Card de um jogo. Aparece no catalogo, na previa da landing e na colecao.
// Quando recebe onQuickAdd, mostra um botao "+" no canto pra adicionar o jogo a
// colecao sem sair do catalogo. O botao fica FORA do <Link> (e irmao dele) pra
// nao aninhar um <button> dentro de um <a>, que e invalido.
export default function GameCard({ game, onQuickAdd }) {
  return (
    <div className="relative group">
      <Link
        to={`/games/${game.id}`}
        className="block bg-canvas border border-line rounded-2xl overflow-hidden transition hover:border-accent"
      >
        <div className="relative overflow-hidden">
          <img
            src={game.coverUrl}
            alt={game.title}
            loading="lazy"
            className="w-full aspect-video object-cover transition duration-300 group-hover:scale-105"
          />
          <span className="absolute top-2.5 left-2.5 text-xs font-semibold text-ink bg-canvas/90 backdrop-blur rounded-full px-2.5 py-0.5 border border-line">
            {game.genre?.split(',')[0]}
          </span>
        </div>
        <div className="p-4">
          <h3 className="font-display font-bold text-ink leading-tight truncate" title={game.title}>{game.title}</h3>
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
          className="absolute top-2.5 right-2.5 grid place-items-center h-8 w-8 rounded-full bg-ink/85 text-white opacity-0 group-hover:opacity-100 transition hover:bg-ink cursor-pointer"
        >
          <Plus size={16} />
        </button>
      )}
    </div>
  )
}
