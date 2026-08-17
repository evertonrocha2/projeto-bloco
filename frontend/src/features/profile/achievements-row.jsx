import { Award } from 'lucide-react'
import { describeAchievements } from '@/lib/achievements.js'

// As conquistas ganhas.
//
// So as GANHAS aparecem. Mostrar as bloqueadas em cinza transformaria o perfil
// numa lista de tarefas - e a ideia era o contrario: entrar e ver o que ja foi
// feito. Quem quiser caçar as que faltam ainda descobre pelo que os outros tem.
export default function AchievementsRow({ codes }) {
  const conquistas = describeAchievements(codes)

  if (conquistas.length === 0) {
    return null
  }

  return (
    <div className="flex flex-wrap gap-2">
      {conquistas.map((conquista) => (
        <span
          key={conquista.code}
          title={conquista.description || undefined}
          className="inline-flex items-center gap-1.5 text-xs font-medium text-accent bg-accent-soft border border-accent/25 px-2.5 py-1"
        >
          <Award size={12} />
          {conquista.label}
        </span>
      ))}
    </div>
  )
}
