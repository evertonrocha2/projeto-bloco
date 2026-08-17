import { useRef, useState } from 'react'
import { Check, Upload, X } from 'lucide-react'
import { api } from '@/lib/api'
import { field } from '@/lib/ui.js'
import { resolveImageUrl } from '@/lib/image-url.js'

// As artes que ja moram em frontend/public. Servidas da raiz, entao o caminho
// relativo comecando com / e exatamente o que o servidor aceita.
const GALERIA = [
  { url: '/background.jpg', label: 'Abertura' },
  { url: '/background-2.jpg', label: 'Recomendações' },
  { url: '/background-3.jpg', label: 'Acesso' },
  { url: '/background-4.jpg', label: 'Paralaxe' },
]

// Escolher uma imagem: subir a sua, pegar uma da galeria, ou colar um endereco.
//
// Os tres caminhos terminam no MESMO lugar - uma string de URL no campo abaixo. E
// o que faz o upload nao ter exigido mudanca em mais nada: perfil e capa de lista
// continuam guardando uma URL, e o servidor valida a string do mesmo jeito venha
// ela de onde vier.
//
// A ordem na tela e deliberada: subir primeiro, porque personalizar de verdade e
// usar a propria imagem. A galeria vem em seguida como atalho pra quem nao tem
// nenhuma em maos, e o campo de endereco fica por ultimo, pra quem sabe o que quer.
export default function ImagePicker({ value, onChange, label, aspect = 'aspect-[3/1]' }) {
  const inputRef = useRef(null)
  const [enviando, setEnviando] = useState(false)
  const [erro, setErro] = useState(null)

  async function subir(event) {
    const file = event.target.files?.[0]
    if (!file) return

    setEnviando(true)
    setErro(null)
    try {
      onChange(await api.uploadImage(file))
    } catch (err) {
      setErro(err.message)
    } finally {
      setEnviando(false)
      // Limpa o input pra escolher O MESMO arquivo de novo disparar o evento. Sem
      // isso, corrigir um envio que falhou exigiria escolher outro arquivo antes.
      event.target.value = ''
    }
  }

  const enviada = value && value.startsWith('/api/uploads/')

  return (
    <div className="flex flex-col gap-2">
      <label className="text-xs font-semibold text-slate">{label}</label>

      <div className="flex items-center gap-2">
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={enviando}
          className="inline-flex items-center gap-1.5 border border-line text-ink text-xs px-3 py-2 transition-colors hover:border-accent hover:text-accent disabled:opacity-40 disabled:cursor-not-allowed cursor-pointer"
        >
          <Upload size={13} />
          {enviando ? 'Enviando...' : 'Subir imagem'}
        </button>

        <span className="text-xs text-slate/70">JPEG, PNG ou WebP, até 5 MB.</span>

        <input
          ref={inputRef}
          type="file"
          // accept apenas FILTRA o seletor de arquivos, e nao valida nada: da pra
          // trocar o filtro pra "todos" e escolher qualquer coisa. Quem valida e o
          // servidor, olhando os bytes.
          accept="image/jpeg,image/png,image/webp"
          onChange={subir}
          className="hidden"
        />
      </div>

      {/* Previa da imagem enviada, com como tirar. A previa importa porque o
          resultado do upload e uma URL de UUID - sem ver a imagem, o campo abaixo
          e uma sequencia sem sentido nenhum. */}
      {enviada && (
        <div className="flex items-center gap-2">
          <span className={`block ${aspect} w-24 overflow-hidden border border-accent`}>
            <img src={resolveImageUrl(value)} alt="" className="h-full w-full object-cover" />
          </span>
          <button
            type="button"
            onClick={() => onChange('')}
            className="inline-flex items-center gap-1 text-xs text-slate hover:text-danger cursor-pointer"
          >
            <X size={12} /> tirar
          </button>
        </div>
      )}

      {erro && <p className="text-danger text-xs">{erro}</p>}

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
