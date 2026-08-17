import { Clock, Trophy, Flag, Sparkles } from 'lucide-react'

// A estante em numeros, no topo do perfil.
//
// Sao dados que o app ja tinha e nunca somava. Quem entra no proprio perfil
// via uma grade de capas e nada mais - nenhuma leitura do que aquilo tudo
// significa junto. "428 horas" e uma frase sobre a pessoa; a mesma informacao
// espalhada em vinte cartoes nao e.
//
// Alinhado em tabular-nums pros numeros nao dancarem de largura entre perfis.
function Stat({ icon: Icon, value, label }) {
  return (
    <div className="flex-1 min-w-28 px-4 py-3">
      <div className="flex items-center gap-1.5 text-accent">
        <Icon size={13} />
        <span className="font-display text-xl leading-none tabular-nums">{value}</span>
      </div>
      <p className="eyebrow mt-1.5">{label}</p>
    </div>
  )
}

export default function StatsStrip({ stats }) {
  if (!stats) {
    return null
  }

  const zerados = stats.countByStatus.ZERADO || 0
  const platinas = stats.countByStatus.PLATINADO || 0

  return (
    <div className="flex flex-wrap border border-line bg-mist divide-x divide-line">
      <Stat icon={Clock} value={`${stats.totalHours}h`} label="jogadas" />
      <Stat icon={Flag} value={zerados} label="zerados" />
      <Stat icon={Trophy} value={platinas} label="platinas" />
      {/* O genero so aparece se houver colecao: um campo vazio rotulado
          "gênero favorito" e pior do que a ausencia dele. */}
      {stats.favoriteGenre && (
        <Stat icon={Sparkles} value={stats.favoriteGenre} label="gênero favorito" />
      )}
    </div>
  )
}
