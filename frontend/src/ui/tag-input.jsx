import { useState } from 'react'
import { X } from 'lucide-react'

// Entrada de tags: digita, aperta Enter, vira etiqueta.
//
// O teto de 5 e a normalizacao pra minusculas repetem regras que o servidor ja
// aplica. Nao e desconfianca do back-end - e que descobrir o limite ao receber um
// 400, depois de escrever tudo e clicar em salvar, e pior do que o campo
// simplesmente parar de aceitar. O servidor continua sendo quem decide.
export default function TagInput({ tags, onChange, max = 5 }) {
  const [draft, setDraft] = useState('')

  const cheio = tags.length >= max

  function adicionar() {
    const nova = draft.trim().toLowerCase()

    // Vazia e repetida saem em silencio: nao ha nada pra avisar, e um erro
    // vermelho por digitar a mesma tag duas vezes seria barulho.
    if (!nova || tags.includes(nova) || cheio) {
      setDraft('')
      return
    }

    onChange([...tags, nova.slice(0, 24)])
    setDraft('')
  }

  function handleKeyDown(event) {
    // Enter e virgula confirmam. Virgula porque e como as pessoas escrevem lista
    // sem pensar, e engolir a tecla e mais gentil do que aceitar uma tag chamada
    // "terror,".
    if (event.key === 'Enter' || event.key === ',') {
      event.preventDefault()
      adicionar()
      return
    }

    // Backspace no campo vazio apaga a ultima etiqueta - o gesto que todo mundo
    // ja tenta por reflexo.
    if (event.key === 'Backspace' && draft === '' && tags.length > 0) {
      onChange(tags.slice(0, -1))
    }
  }

  return (
    <div>
      <div className="flex flex-wrap items-center gap-1.5 bg-canvas border border-line px-2 py-2 focus-within:border-accent transition-colors">
        {tags.map((tag) => (
          <span
            key={tag}
            className="inline-flex items-center gap-1 text-[0.7rem] font-medium text-accent bg-accent-soft border border-accent/25 px-2 py-0.5"
          >
            {tag}
            <button
              type="button"
              onClick={() => onChange(tags.filter((atual) => atual !== tag))}
              className="text-accent/70 hover:text-accent cursor-pointer"
              title={`Remover ${tag}`}
            >
              <X size={11} />
            </button>
          </span>
        ))}

        <input
          value={draft}
          onChange={(e) => setDraft(e.target.value)}
          onKeyDown={handleKeyDown}
          // Confirma ao sair do campo: sem isto, quem digita a ultima tag e clica
          // direto em "Salvar" perde o que escreveu, sem nenhum aviso.
          onBlur={adicionar}
          disabled={cheio}
          maxLength={24}
          placeholder={cheio ? '' : tags.length === 0 ? 'indie, terror, pra jogar no sofá...' : ''}
          className="flex-1 min-w-24 bg-transparent text-sm text-ink outline-none placeholder:text-slate/60 disabled:cursor-not-allowed"
        />
      </div>

      <p className="text-xs text-slate/70 mt-1">
        {cheio ? `Máximo de ${max} tags.` : `Enter pra confirmar cada tag. ${tags.length}/${max}.`}
      </p>
    </div>
  )
}
