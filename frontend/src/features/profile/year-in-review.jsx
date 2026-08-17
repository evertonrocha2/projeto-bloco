import { CalendarDays } from 'lucide-react'

// "Seu 2026": o que a pessoa terminou no ano.
//
// Some inteiro quando nao ha nada terminado. Um bloco chamado "Seu 2026" com
// tres tracinhos dentro nao e retrospectiva nenhuma - e um lembrete de que o ano
// nao rendeu, no lugar da tela que deveria dar prazer de visitar.
export default function YearInReview({ stats }) {
  if (!stats || stats.finishedThisYear === 0) {
    return null
  }

  const linha = (rotulo, valor) =>
    valor && (
      <div className="min-w-0">
        <p className="eyebrow">{rotulo}</p>
        <p className="text-sm text-ink truncate mt-0.5" title={valor}>{valor}</p>
      </div>
    )

  return (
    <section className="border border-line bg-mist p-5">
      <h2 className="inline-flex items-center gap-2 font-display text-lg text-ink">
        <CalendarDays size={16} className="text-accent" />
        Seu {stats.year}
      </h2>

      <div className="grid gap-4 sm:grid-cols-3 mt-4">
        <div>
          <p className="eyebrow">terminados</p>
          <p className="font-display text-2xl text-accent tabular-nums leading-none mt-1">
            {stats.finishedThisYear}
          </p>
        </div>
        {linha('mais jogado', stats.mostPlayedThisYear)}
        {linha('melhor nota sua', stats.bestRatedThisYear)}
      </div>
    </section>
  )
}
