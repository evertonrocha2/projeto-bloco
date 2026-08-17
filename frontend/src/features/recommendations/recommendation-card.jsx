import { Link } from 'react-router-dom'
import { ThumbsUp, X } from 'lucide-react'
import GameCover from '@/ui/game-cover.jsx'

// Card de um jogo recomendado.
//
// Mostra o que so a recomendacao tem: a PONTUACAO do algoritmo e o genero que
// fez aquele jogo aparecer. O genero e exibido como etiqueta, e nao como frase
// solta, pra deixar visivel a ligacao com o grafico de perfil ao lado - o mesmo
// rotulo aparece nos dois lugares.
//
// Os botoes ficam FORA do <Link> (irmaos dele): um <button> dentro de um <a> e
// HTML invalido.
export default function RecommendationCard({ item, onFeedback, busy }) {
  const generos = item.reasonGenres || []

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
          <div className="absolute bottom-0 left-0 flex items-baseline gap-1 px-2 py-0.5 bg-canvas/90 border-t border-r border-line backdrop-blur">
            <span className="font-display text-sm leading-none text-accent tabular-nums">
              {item.score.toFixed(2)}
            </span>
            <span className="text-[0.65rem] text-slate leading-none">/5</span>
          </div>
        </div>

        <div className="p-3 border-t border-line">
          <h3
            className="font-display text-sm text-ink leading-tight truncate"
            title={item.gameTitle}
          >
            {item.gameTitle}
          </h3>

          {/* O motivo, em etiqueta. Sem afinidade, a indicacao veio da nota da
              comunidade - e a tela diz isso, em vez de deixar o card sem
              explicacao nenhuma. */}
          <div className="flex flex-wrap gap-1 mt-2">
            {generos.length > 0 ? (
              generos.map((genero) => (
                <span
                  key={genero}
                  className="text-[0.65rem] font-medium text-accent bg-accent-soft border border-accent/25 px-1.5 py-0.5"
                >
                  {genero}
                </span>
              ))
            ) : (
              <span className="text-[0.65rem] text-slate">bem avaliado pela comunidade</span>
            )}
          </div>
        </div>
      </Link>

      <div className="absolute top-0 right-0 flex opacity-0 group-hover:opacity-100 focus-within:opacity-100 transition">
        <button
          onClick={() => onFeedback(item.gameId, 'LIKED')}
          disabled={busy}
          title="Gostei — indique mais desse tipo"
          className="grid place-items-center h-7 w-7 bg-canvas/90 text-ink border-l border-b border-line transition-colors hover:bg-positive hover:text-canvas disabled:opacity-40 cursor-pointer"
        >
          <ThumbsUp size={13} />
        </button>
        <button
          onClick={() => onFeedback(item.gameId, 'DISMISSED')}
          disabled={busy}
          title="Não me interessa — não mostre de novo"
          className="grid place-items-center h-7 w-7 bg-canvas/90 text-ink border-l border-b border-line transition-colors hover:bg-danger hover:text-canvas disabled:opacity-40 cursor-pointer"
        >
          <X size={13} />
        </button>
      </div>
    </div>
  )
}
