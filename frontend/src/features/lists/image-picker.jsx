import { Check } from 'lucide-react'
import { field } from '@/lib/ui.js'

// As artes que ja moram em frontend/public. Servidas da raiz, entao o caminho
// relativo comecando com / e exatamente o que o servidor aceita.
const GALERIA = [
  { url: '/background.jpg', label: 'Abertura' },
  { url: '/background-2.jpg', label: 'Recomendações' },
  { url: '/background-3.jpg', label: 'Acesso' },
  { url: '/background-4.jpg', label: 'Paralaxe' },
]

// Escolher uma imagem: galeria pronta ou endereco colado.
//
// A galeria vem primeiro de proposito. Quem abre a tela de editar perfil raramente
// tem uma URL de imagem na mao, e um campo de texto vazio como unica opcao e o
// caminho mais curto pra pessoa desistir e ficar com o perfil sem capa.
//
// Usado pelo avatar, pela capa do perfil e pela capa das listas.
export default function ImagePicker({ value, onChange, label, aspect = 'aspect-[3/1]' }) {
  return (
    <div className="flex flex-col gap-2">
      <label className="text-xs font-semibold text-slate">{label}</label>

      <div className="grid grid-cols-4 gap-2">
        {GALERIA.map((arte) => {
          const escolhida = value === arte.url

          return (
            <button
              key={arte.url}
              type="button"
              onClick={() => onChange(escolhida ? '' : arte.url)}
              title={arte.label}
              className={
                `relative ${aspect} overflow-hidden border transition-colors cursor-pointer ` +
                (escolhida ? 'border-accent' : 'border-line hover:border-slate/50')
              }
            >
              <img src={arte.url} alt={arte.label} className="h-full w-full object-cover" />
              {escolhida && (
                <span className="absolute inset-0 grid place-items-center bg-canvas/70">
                  <Check size={16} className="text-accent" />
                </span>
              )}
            </button>
          )
        })}
      </div>

      <input
        value={value || ''}
        onChange={(e) => onChange(e.target.value)}
        placeholder="ou cole um endereço https://..."
        className={`${field} text-xs`}
      />
    </div>
  )
}
