import { ThumbsUp, X } from 'lucide-react'
import GameCard, { cardActionButton } from '@/ui/game-card.jsx'

// O card de um jogo recomendado.
//
// Mesma casca dos outros; o que muda e o conteudo dos espacos. Mostra o que so a
// recomendacao tem: a PONTUACAO do algoritmo e o genero que fez aquele jogo
// aparecer. O genero vai como etiqueta, e nao como frase solta, pra deixar
// visivel a ligacao com o grafico de perfil ao lado - o mesmo rotulo aparece nos
// dois lugares.
export default function RecommendationCard({ item, onFeedback, busy }) {
  const generos = item.reasonGenres || []

  return (
    <GameCard
      to={`/games/${item.gameId}`}
      game={{ title: item.gameTitle, coverUrl: item.gameCoverUrl }}
      // Pontuacao: maximo teorico 5.0 (3.0 de genero + 2.0 de comunidade)
      overlayBottom={
        <span className="flex items-baseline gap-1">
          <span className="font-display text-sm leading-none text-accent tabular-nums">
            {item.score.toFixed(2)}
          </span>
          <span className="text-[0.65rem] text-slate leading-none">/5</span>
        </span>
      }
      // O motivo, em etiqueta. Sem afinidade, a indicacao veio da nota da
      // comunidade - e a tela diz isso, em vez de deixar o card sem explicacao.
      footer={
        <div className="flex flex-wrap gap-1">
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
      }
      actions={
        <>
          <button
            onClick={() => onFeedback(item.gameId, 'LIKED')}
            disabled={busy}
            title="Gostei — indique mais desse tipo"
            className={`${cardActionButton} hover:bg-positive hover:text-canvas`}
          >
            <ThumbsUp size={14} />
          </button>
          <button
            onClick={() => onFeedback(item.gameId, 'DISMISSED')}
            disabled={busy}
            title="Não me interessa — não mostre de novo"
            className={`${cardActionButton} hover:bg-danger hover:text-canvas`}
          >
            <X size={14} />
          </button>
        </>
      }
    />
  )
}
