import { Star } from 'lucide-react'

// Estrelas usando icone (lucide). Sem onChange so exibe; com onChange deixa
// clicar pra escolher a nota.
export default function StarRating({ value = 0, onChange, size = 16 }) {
  const interactive = typeof onChange === 'function'
  const stars = [1, 2, 3, 4, 5]

  return (
    <span className="inline-flex items-center gap-0.5">
      {stars.map((star) => {
        const active = star <= value
        return (
          <Star
            key={star}
            size={size}
            onClick={interactive ? () => onChange(star) : undefined}
            className={
              (active ? 'fill-amber-400 text-amber-400' : 'fill-none text-slate/40') +
              (interactive ? ' cursor-pointer transition hover:scale-110' : '')
            }
          />
        )
      })}
    </span>
  )
}
