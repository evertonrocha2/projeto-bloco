import { Gamepad2 } from 'lucide-react'

// Letreiro discreto que desliza com frases marcantes de jogos. O loop e
// continuo porque repetimos a lista e animamos ate -50%. Passar o mouse pausa.
const PHRASES = [
  'It’s dangerous to go alone! Take this. — Zelda',
  'War. War never changes. — Fallout',
  'A man chooses, a slave obeys. — BioShock',
  'The cake is a lie. — Portal',
  'Praise the Sun! — Dark Souls',
  'Nothing is true, everything is permitted. — Assassin’s Creed',
  'Stay awhile and listen. — Diablo',
  'Finish him! — Mortal Kombat',
]

export default function Marquee() {
  const items = [...PHRASES, ...PHRASES]

  return (
    // As bordas desvanecem nas pontas (mask lateral): sem isso o texto aparece e
    // some cortado na beirada da tela, o que parece defeito de layout.
    <div
      className="group overflow-hidden whitespace-nowrap bg-mist border-y border-line py-3.5"
      style={{
        maskImage: 'linear-gradient(to right, transparent, #000 8%, #000 92%, transparent)',
        WebkitMaskImage: 'linear-gradient(to right, transparent, #000 8%, #000 92%, transparent)',
      }}
    >
      <div className="inline-flex items-center animate-marquee group-hover:[animation-play-state:paused]">
        {items.map((phrase, i) => (
          <span key={i} className="inline-flex items-center text-sm text-slate">
            {phrase}
            <Gamepad2 size={12} className="text-accent mx-6 shrink-0" />
          </span>
        ))}
      </div>
    </div>
  )
}
