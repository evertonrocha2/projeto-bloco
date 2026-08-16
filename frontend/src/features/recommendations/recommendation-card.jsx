import { Link } from 'react-router-dom'
import { ThumbsUp, X } from 'lucide-react'
import GameCover from '@/ui/game-cover.jsx'
import { reasonText } from './recommendation-text.js'

// Card de um jogo recomendado.
//
// Parece o GameCard do catalogo, mas mostra o que so a recomendacao tem: a
// PONTUACAO do algoritmo e o porque daquele jogo ter aparecido.
//
// A pontuacao e o elemento de destaque da tela, e por isso ganha tratamento
// tipografico proprio - numero grande na fonte de display, sobreposto a capa, com
// a escala (/5) em corpo miudo ao lado. E o resultado do microsservico virando o
// centro visual da interface, em vez de um detalhe num canto.
//
// Os botoes ficam FORA do <Link> (irmaos dele) pelo mesmo motivo do GameCard: um
// <button> dentro de um <a> e HTML invalido.
export default function RecommendationCard({ item, onFeedback, busy }) {
  return (
    <div className="relative group">
      <Link
        to={`/games/${item.gameId}`}
        className="block border border-line bg-mist transition-colors hover:border-accent"
      >
        <div className="relative overflow-hidden">
          <GameCover
            game={{ title: item.gameTitle, coverUrl: item.gameCoverUrl }}
            className="aspect-[3/4] transition-transform duration-700 group-hover:scale-105"
          />

          {/* Pontuacao: maximo teorico 5.0 (3.0 de genero + 2.0 de comunidade) */}
          <div className="absolute bottom-0 left-0 flex items-baseline gap-1 px-2.5 py-1 bg-canvas/90 border-t border-r border-line backdrop-blur">
            <span className="font-display font-bold text-lg leading-none text-accent">
              {item.score.toFixed(2)}
            </span>
            <span className="text-[0.7rem] text-slate leading-none">/5</span>
          </div>
        </div>

        <div className="p-4 border-t border-line">
          <h3 className="font-display font-semibold text-ink leading-tight truncate" title={item.gameTitle}>
            {item.gameTitle}
          </h3>
          {/* A frase e montada no front a partir da lista de generos que a API manda */}
          <p className="text-sm text-slate mt-1.5 line-clamp-2">{reasonText(item.reasonGenres)}</p>
        </div>
      </Link>

      <div className="absolute top-0 right-0 flex opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition">
        <button
          onClick={() => onFeedback(item.gameId, 'LIKED')}
          disabled={busy}
          title="Gostei — indique mais desse tipo"
          className="grid place-items-center h-8 w-8 bg-canvas/90 text-ink border-l border-b border-line transition-colors hover:bg-positive hover:text-canvas disabled:opacity-40 cursor-pointer"
        >
          <ThumbsUp size={15} />
        </button>
        <button
          onClick={() => onFeedback(item.gameId, 'DISMISSED')}
          disabled={busy}
          title="Não me interessa — não mostre de novo"
          className="grid place-items-center h-8 w-8 bg-canvas/90 text-ink border-l border-b border-line transition-colors hover:bg-danger hover:text-canvas disabled:opacity-40 cursor-pointer"
        >
          <X size={15} />
        </button>
      </div>
    </div>
  )
}
