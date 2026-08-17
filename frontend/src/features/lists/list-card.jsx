import { Link } from 'react-router-dom'
import { Lock, Layers } from 'lucide-react'
import { resolveImageUrl } from '@/lib/image-url.js'

// Uma lista no perfil ou na busca por tag.
//
// Nao usa a casca de GameCard: aquela e retrato 3/4 porque o assunto dela e UMA
// capa de jogo. Uma lista e um conjunto, entao o formato e paisagem e o peso fica
// no titulo e na descricao - a capa e ambiente.
export default function ListCard({ list }) {
  const privada = list.visibility === 'PRIVATE'

  return (
    <Link
      to={`/lists/${list.id}`}
      className="group block border border-line bg-mist transition-colors hover:border-accent overflow-hidden"
    >
      {list.coverUrl && (
        <div className="relative aspect-[3/1] overflow-hidden border-b border-line">
          <img
            src={resolveImageUrl(list.coverUrl)}
            alt=""
            className="h-full w-full object-cover transition-transform duration-700 group-hover:scale-105"
            style={{ filter: 'brightness(0.55) saturate(0.9)' }}
          />
        </div>
      )}

      <div className="p-4">
        <div className="flex items-start justify-between gap-3">
          <h3 className="font-display text-ink leading-tight">{list.title}</h3>
          {/* O cadeado so aparece pro dono, porque so ele recebe listas privadas
              da API - ninguem mais chega a ver este cartao. */}
          {privada && <Lock size={13} className="shrink-0 mt-0.5 text-slate" title="Lista privada" />}
        </div>

        {list.description && (
          <p className="text-sm text-slate leading-relaxed mt-1.5 line-clamp-2">{list.description}</p>
        )}

        <div className="flex items-center justify-between gap-3 mt-3">
          <div className="flex flex-wrap gap-1">
            {list.tags.map((tag) => (
              <span
                key={tag}
                className="text-[0.65rem] font-medium text-accent bg-accent-soft border border-accent/25 px-1.5 py-0.5"
              >
                {tag}
              </span>
            ))}
          </div>

          <span className="inline-flex items-center gap-1 shrink-0 text-xs text-slate tabular-nums">
            <Layers size={12} /> {list.gameCount}
          </span>
        </div>
      </div>
    </Link>
  )
}
