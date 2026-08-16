// Pedacos de classe Tailwind reaproveitados.
//
// Tema escuro editorial: cantos RETOS em tudo. A estrutura da interface vem de
// reguas de 1px e de espaco em branco, nao de curva e sombra - foi a decisao que
// mais aproximou o resultado da referencia visual.

const btnBase =
  'inline-flex items-center justify-center gap-2 font-medium ' +
  'px-6 py-3 transition-colors duration-200 cursor-pointer disabled:opacity-40 ' +
  'disabled:cursor-not-allowed'

// primario: ambar solido sobre quase-preto. O texto e o PROPRIO fundo da pagina,
// e nao branco - e o que faz o botao parecer recortado da tela.
export const btnPrimary = `${btnBase} bg-accent text-canvas hover:bg-accent/85`

// secundario: so contorno. Some no fundo ate voce passar o mouse.
export const btnGhost = `${btnBase} border border-line text-ink hover:border-accent hover:text-accent`

// acento: usado quando ja existe um primario por perto e este precisa ceder.
export const btnAccent = `${btnBase} bg-mist text-ink border border-line hover:bg-line`

// cartao: painel um degrau acima do fundo, delimitado por regua fina.
export const card = 'bg-mist border border-line'

// campo de formulario
export const field =
  'w-full bg-canvas border border-line px-3.5 py-2.5 text-sm text-ink ' +
  'outline-none transition-colors placeholder:text-slate/60 ' +
  'focus:border-accent'

// etiqueta retangular (genero, status). Era pilula; virou retangulo.
export const tag =
  'inline-flex items-center text-[0.7rem] font-medium tracking-wide ' +
  'px-2.5 py-1 bg-canvas/85 text-ink border border-line backdrop-blur'
