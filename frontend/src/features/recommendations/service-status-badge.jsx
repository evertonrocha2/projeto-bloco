import { serviceStatus } from './recommendation-text.js'

// Mostra se as recomendacoes vieram de um calculo novo ou do ultimo lote salvo.
//
// Este selo e a arquitetura distribuida ficando VISIVEL na tela. Quando o monolito
// esta fora do ar, o microsservico responde com stale=true e serve o que tem
// gravado no banco proprio - a tela continua funcionando, com aviso, em vez de
// mostrar erro.
//
// O ponto luminoso a esquerda faz o trabalho que um icone faria, com menos ruido:
// verde parado quando esta ao vivo, ambar pulsando quando esta degradado. E o
// unico movimento da interface, e existe porque sinaliza algo que muda sozinho.
export default function ServiceStatusBadge({ stale }) {
  const status = serviceStatus(stale)

  return (
    <span
      title={status.detail}
      className={
        'inline-flex items-center gap-2 text-[0.7rem] font-medium tracking-[0.14em] uppercase px-2.5 py-1 border ' +
        (status.live
          ? 'text-positive border-line'
          : 'text-warning border-warning/40')
      }
    >
      <span
        aria-hidden="true"
        className={
          'block h-1.5 w-1.5 rounded-full ' +
          (status.live ? 'bg-positive' : 'bg-warning animate-pulse')
        }
      />
      {status.label}
    </span>
  )
}
