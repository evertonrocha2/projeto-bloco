import { Link } from 'react-router-dom'
import { ThumbsUp, X, Sparkles } from 'lucide-react'
import { reasonText } from '../recommendationText.js'

// Card de um jogo recomendado.
//
// Parece o GameCard do catalogo, mas mostra coisas que so a recomendacao tem: a
// pontuacao que o algoritmo deu e o POR QUE aquele jogo apareceu. E tem os dois
// botoes de feedback, que sao a unica parte do sistema que grava dado exclusivo do
// microsservico - o monolito nunca fica sabendo desse veredito.
//
// Os botoes ficam FORA do <Link> (irmaos dele) pelo mesmo motivo do GameCard: um
// <button> dentro de um <a> e HTML invalido.
export default function RecommendationCard({ item, onFeedback, busy }) {
  return (
    <div className="relative group">
      <Link
        to={`/games/${item.gameId}`}
        className="block bg-canvas border border-line rounded-2xl overflow-hidden transition hover:border-accent"
      >
        <div className="relative overflow-hidden">
          <img
            src={item.gameCoverUrl}
            alt={item.gameTitle}
            loading="lazy"
            className="w-full aspect-video object-cover transition duration-300 group-hover:scale-105"
          />
          {/* Pontuacao do algoritmo. Max teorico 5.0 (3.0 de genero + 2.0 de comunidade) */}
          <span className="absolute top-2.5 left-2.5 inline-flex items-center gap-1 text-xs font-bold text-white bg-accent rounded-full px-2.5 py-0.5">
            <Sparkles size={12} />
            {item.score.toFixed(2)}
          </span>
        </div>

        <div className="p-4">
          <h3 className="font-display font-bold text-ink leading-tight truncate" title={item.gameTitle}>
            {item.gameTitle}
          </h3>
          {/* A frase e montada no front a partir da lista de generos que a API manda */}
          <p className="text-sm text-slate mt-1.5 line-clamp-2">{reasonText(item.reasonGenres)}</p>
        </div>
      </Link>

      <div className="absolute top-2.5 right-2.5 flex gap-1.5 opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition">
        <button
          onClick={() => onFeedback(item.gameId, 'LIKED')}
          disabled={busy}
          title="Gostei — indique mais desse tipo"
          className="grid place-items-center h-8 w-8 rounded-full bg-ink/85 text-white transition hover:bg-emerald-600 disabled:opacity-50 cursor-pointer"
        >
          <ThumbsUp size={15} />
        </button>
        <button
          onClick={() => onFeedback(item.gameId, 'DISMISSED')}
          disabled={busy}
          title="Não me interessa — não mostre de novo"
          className="grid place-items-center h-8 w-8 rounded-full bg-ink/85 text-white transition hover:bg-red-600 disabled:opacity-50 cursor-pointer"
        >
          <X size={15} />
        </button>
      </div>
    </div>
  )
}
