import { card } from '@/lib/ui.js'

// O perfil de gosto do usuario em barras: quanto ele gosta de cada genero, na
// escala em que o favorito vale 1.0.
//
// Existe pra tornar a recomendacao AUDITAVEL. Sem isso a lista de jogos e uma
// caixa preta e o usuario nao tem como saber se o sistema entendeu o gosto dele.
// Com o grafico ao lado, "por que esse jogo apareceu?" tem resposta na tela.
//
// Barras com Tailwind puro, sem biblioteca de grafico: sao seis barras
// horizontais, e uma dependencia nova pra isso seria peso sem retorno.
export default function TasteProfileChart({ genres }) {
  if (!genres || genres.length === 0) {
    return (
      <div className={`${card} p-5`}>
        <h2 className="font-display font-bold text-ink">Seu perfil de gosto</h2>
        <p className="text-sm text-slate mt-2">
          Avalie alguns jogos pra gente entender o que você curte. Enquanto isso, as
          indicações vêm da nota da comunidade.
        </p>
      </div>
    )
  }

  // So os generos mais fortes: a lista inteira pode ter muitos com peso baixo, e
  // mostrar todos transformaria o grafico em ruido.
  const principais = genres.slice(0, 6)

  return (
    <div className={`${card} p-5`}>
      <h2 className="font-display font-bold text-ink">Seu perfil de gosto</h2>
      <p className="text-sm text-slate mt-1 mb-4">
        Calculado a partir das suas avaliações e da sua coleção.
      </p>

      <ul className="space-y-2.5">
        {principais.map(({ genre, weight }) => (
          <li key={genre}>
            <div className="flex items-center justify-between text-sm mb-1">
              <span className="font-semibold text-ink">{genre}</span>
              {/* Peso como porcentagem: 1.0 vira 100%, que le mais facil */}
              <span className="text-slate">{Math.round(weight * 100)}%</span>
            </div>
            <div className="h-2 rounded-full bg-mist overflow-hidden">
              <div
                className="h-full rounded-full bg-accent transition-all duration-500"
                style={{ width: `${Math.max(weight * 100, 3)}%` }}
              />
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
