import { Wifi, WifiOff } from 'lucide-react'
import { serviceStatus } from '../recommendationText.js'

// Mostra se as recomendacoes vieram de um calculo novo ou do ultimo lote salvo.
//
// Este selo e a arquitetura distribuida ficando VISIVEL na tela. Quando o monolito
// esta fora do ar, o microsservico responde com stale=true e serve o que tem
// gravado no banco proprio - a tela continua funcionando, com aviso, em vez de
// mostrar erro.
//
// E o componente que torna a demonstracao concreta: derruba-se o monolito, a
// pagina continua listando jogos, e o selo muda de "ao vivo" pra "modo degradado".
export default function ServiceStatusBadge({ stale }) {
  const status = serviceStatus(stale)
  const Icon = status.live ? Wifi : WifiOff

  return (
    <span
      title={status.detail}
      className={
        'inline-flex items-center gap-1.5 text-xs font-semibold rounded-full px-3 py-1 border ' +
        (status.live
          ? 'text-emerald-700 bg-emerald-50 border-emerald-200'
          : 'text-amber-700 bg-amber-50 border-amber-200')
      }
    >
      <Icon size={13} />
      {status.label}
    </span>
  )
}
