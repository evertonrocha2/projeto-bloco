import { useEffect, useId, useLayoutEffect, useRef, useState } from 'react'
import { createPortal } from 'react-dom'
import { Check, ChevronDown } from 'lucide-react'
import { nextHighlight, findByPrefix } from './listbox-keys.js'

// Altura maxima do painel, em pixels. Vive aqui, e nao numa classe do Tailwind,
// porque a conta de "cabe embaixo?" precisa do numero.
const ALTURA_MAX = 256

// Respiro entre o campo e o painel.
const FOLGA = 4

// Select proprio, no lugar do <select> nativo.
//
// O nativo nao aceita estilo no painel: o navegador desenha aquela lista cinza de
// sistema, que no tema escuro aparece como um retangulo claro de outro software
// colado na tela. Nao havia como resolver por CSS - so trocando o componente.
//
// O painel abre ANCORADO no campo, e nao centralizado na tela: sao cinco valores
// curtos, e um modal com fundo escurecido pra trocar entre cinco palavras e
// cerimonia demais pro que a acao vale.
//
// O painel e desenhado num PORTAL, com posicao fixa calculada a partir do campo.
// Isso nao e sofisticacao: o primeiro lugar onde este componente e usado e o
// rodape do modal de colecao, que tem overflow-hidden. Um painel absoluto ali
// seria recortado pela borda do modal e o que sobrasse cairia fora da tela - e a
// falha e silenciosa, do tipo que so aparece quando alguem abre o modal e ve meia
// lista. Sair do fluxo resolve a classe inteira do problema, em qualquer
// contexto de rolagem em que o campo venha a ser usado.
//
// Acessibilidade: o foco NAO se move pras opcoes. Ele fica no gatilho, e o
// destaque viaja por aria-activedescendant. E o padrao de listbox do ARIA, e
// evita o problema classico de devolver o foco pro lugar certo ao fechar.
export default function Select({ value, onChange, options, className = '', id }) {
  const [open, setOpen] = useState(false)
  // -1 e "nada destacado" - o mesmo contrato de listbox-keys.js.
  const [highlight, setHighlight] = useState(-1)
  const [posicao, setPosicao] = useState(null)

  const triggerRef = useRef(null)
  const listRef = useRef(null)

  const autoId = useId()
  const listId = `${id || autoId}-listbox`
  const optionId = (index) => `${listId}-opt-${index}`

  const selectedIndex = options.findIndex((option) => option.value === value)
  const selected = options[selectedIndex]

  // Onde desenhar o painel. Medido antes da pintura (useLayoutEffect) pra ele
  // nunca aparecer um quadro no canto errado da tela.
  useLayoutEffect(() => {
    if (!open || !triggerRef.current) {
      return
    }

    const rect = triggerRef.current.getBoundingClientRect()
    const abaixo = window.innerHeight - rect.bottom - FOLGA
    const acima = rect.top - FOLGA

    // Abre pra cima quando nao cabe embaixo E ha mais espaco em cima. As duas
    // condicoes: sem a segunda, um campo perto do topo numa janela baixa
    // inverteria pra um espaco ainda menor.
    const praCima = abaixo < Math.min(ALTURA_MAX, acima) && acima > abaixo

    setPosicao({
      left: rect.left,
      width: rect.width,
      praCima,
      top: praCima ? undefined : rect.bottom + FOLGA,
      bottom: praCima ? window.innerHeight - rect.top + FOLGA : undefined,
      // Nunca ultrapassa o espaco disponivel: melhor uma lista rolavel do que
      // uma lista que passa da borda da janela.
      maxHeight: Math.min(ALTURA_MAX, praCima ? acima : abaixo),
    })
  }, [open])

  // Clicar fora fecha. Rolar ou redimensionar tambem: com posicao fixa o painel
  // nao acompanha a pagina, entao mante-lo aberto o deixaria flutuando longe do
  // campo. Fechar e mais honesto do que recalcular a cada quadro de rolagem.
  useEffect(() => {
    if (!open) {
      return
    }

    function fecharSeForaDoCampoEDoPainel(event) {
      const dentroDoCampo = triggerRef.current?.contains(event.target)
      const dentroDoPainel = listRef.current?.contains(event.target)

      if (!dentroDoCampo && !dentroDoPainel) {
        setOpen(false)
      }
    }

    const fechar = () => setOpen(false)

    document.addEventListener('mousedown', fecharSeForaDoCampoEDoPainel)
    // true na fase de captura: pega rolagem de QUALQUER contenedor, inclusive a
    // lista interna do modal, e nao so a da janela.
    window.addEventListener('scroll', fechar, true)
    window.addEventListener('resize', fechar)

    return () => {
      document.removeEventListener('mousedown', fecharSeForaDoCampoEDoPainel)
      window.removeEventListener('scroll', fechar, true)
      window.removeEventListener('resize', fechar)
    }
  }, [open])

  // Mantem a opcao destacada visivel. Sem isso, navegar por seta numa lista
  // longa move um destaque que a pessoa nao consegue ver.
  useEffect(() => {
    if (!open || highlight < 0 || !listRef.current) {
      return
    }

    const node = listRef.current.children[highlight]
    node?.scrollIntoView({ block: 'nearest' })
  }, [open, highlight])

  function abrir() {
    // Abre ja destacando o valor atual, pra seta continuar de onde a pessoa esta.
    setHighlight(selectedIndex)
    setOpen(true)
  }

  function escolher(index) {
    onChange(options[index].value)
    setOpen(false)
    triggerRef.current?.focus()
  }

  function handleKeyDown(event) {
    // Fechado, o teclado so abre.
    if (!open) {
      if (event.key === 'Enter' || event.key === ' ' || event.key === 'ArrowDown') {
        event.preventDefault()
        abrir()
      }
      return
    }

    if (event.key === 'Escape') {
      event.preventDefault()
      setOpen(false)
      return
    }

    // Tab fecha e SEGUE. Sem preventDefault: prender o foco dentro de um select
    // e das formas mais rapidas de tornar uma tela innavegavel por teclado.
    if (event.key === 'Tab') {
      setOpen(false)
      return
    }

    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      if (highlight >= 0) {
        escolher(highlight)
      }
      return
    }

    const movido = nextHighlight(event.key, highlight, options.length)
    if (movido !== null) {
      event.preventDefault()
      setHighlight(movido)
      return
    }

    // Digitar uma letra salta pra opcao que comeca com ela. So letra sozinha:
    // com Ctrl, Alt ou Meta a tecla pertence a um atalho do navegador.
    if (event.key.length === 1 && !event.ctrlKey && !event.altKey && !event.metaKey) {
      const achado = findByPrefix(
        options.map((option) => option.label),
        event.key,
        highlight,
      )

      if (achado >= 0) {
        event.preventDefault()
        setHighlight(achado)
      }
    }
  }

  return (
    <div className={`relative ${className}`}>
      <button
        ref={triggerRef}
        type="button"
        role="combobox"
        aria-expanded={open}
        aria-haspopup="listbox"
        aria-controls={open ? listId : undefined}
        aria-activedescendant={open && highlight >= 0 ? optionId(highlight) : undefined}
        onClick={() => (open ? setOpen(false) : abrir())}
        onKeyDown={handleKeyDown}
        className={
          'w-full flex items-center justify-between gap-3 bg-canvas border px-3.5 py-2.5 ' +
          'text-sm text-left text-ink cursor-pointer transition-colors ' +
          (open ? 'border-accent' : 'border-line hover:border-slate/50')
        }
      >
        <span className="truncate">{selected ? selected.label : '—'}</span>
        {/* A seta gira ao abrir: e o sinal mais barato de "isto esta aberto", e
            sobrevive a quem nao percebe a mudanca de cor da borda. */}
        <ChevronDown
          size={15}
          className={'shrink-0 text-slate transition-transform duration-200 ' + (open ? 'rotate-180' : '')}
        />
      </button>

      {open && posicao && createPortal(
        <ul
          ref={listRef}
          id={listId}
          role="listbox"
          style={{
            position: 'fixed',
            left: posicao.left,
            width: posicao.width,
            top: posicao.top,
            bottom: posicao.bottom,
            maxHeight: posicao.maxHeight,
          }}
          className={
            'select-panel z-60 overflow-y-auto bg-canvas border border-accent ' +
            (posicao.praCima ? 'select-panel-up' : '')
          }
        >
          {options.map((option, index) => {
            const escolhida = option.value === value
            const destacada = index === highlight

            return (
              // Nao e <button>: role="option" dentro de role="listbox" ja e o
              // contrato certo, e um botao faria o leitor de tela anunciar
              // "botao" no lugar de "opcao 3 de 5".
              <li
                key={option.value}
                id={optionId(index)}
                role="option"
                aria-selected={escolhida}
                onMouseEnter={() => setHighlight(index)}
                onClick={() => escolher(index)}
                className={
                  'flex items-center justify-between gap-2 px-3.5 py-2 text-sm cursor-pointer ' +
                  'transition-colors ' +
                  (destacada ? 'bg-accent-soft text-accent' : escolhida ? 'text-accent' : 'text-ink')
                }
              >
                <span className="truncate">{option.label}</span>
                {escolhida && <Check size={14} className="shrink-0 text-accent" />}
              </li>
            )
          })}
        </ul>,
        document.body,
      )}
    </div>
  )
}
