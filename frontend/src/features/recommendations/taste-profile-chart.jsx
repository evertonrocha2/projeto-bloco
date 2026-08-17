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
        <p className="eyebrow">seu perfil</p>
        <h2 className="font-display text-ink mt-2">Ainda sem leitura</h2>
        <p className="text-sm text-slate mt-2 leading-relaxed">
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
      <p className="eyebrow">seu perfil</p>
      <h2 className="font-display text-ink mt-2">Gêneros que te definem</h2>
      <p className="text-sm text-slate mt-1 mb-5 leading-relaxed">
        Calculado a partir das suas avaliações e da sua coleção.
      </p>

      <ul className="flex flex-col">
        {principais.map(({ genre, weight }) => (
          <li key={genre} className="py-2.5 border-t border-line first:border-t-0">
            <div className="flex items-center justify-between text-sm mb-2">
              <span className="font-medium text-ink">{genre}</span>
              {/* Peso como porcentagem: 1.0 vira 100%, que le mais facil */}
              <span className="text-slate tabular-nums">{Math.round(weight * 100)}%</span>
            </div>
            <div className="h-px bg-line">
              <div
                className="h-px bg-accent transition-all duration-700"
                style={{ width: `${Math.max(weight * 100, 4)}%` }}
              />
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
