// Pedacos de classe Tailwind reaproveitados. Visual moderno e clean: botoes em
// pilula, cards brancos com borda fina e sombra suave.

const btnBase =
  'inline-flex items-center justify-center gap-2 font-semibold rounded-full ' +
  'px-6 py-3 transition cursor-pointer disabled:opacity-50'

// primario: quase-preto (bem moderno sobre fundo branco)
export const btnPrimary = `${btnBase} bg-ink text-white hover:bg-ink/90`

// secundario: branco com borda
export const btnGhost = `${btnBase} bg-canvas text-ink border border-line hover:bg-mist`

// acento (indigo), usado em CTAs especiais
export const btnAccent = `${btnBase} bg-accent text-white hover:bg-accent/90`

// cartao base
export const card = 'bg-canvas border border-line rounded-2xl'

// campo de formulario
export const field =
  'w-full bg-canvas border border-line rounded-xl px-3.5 py-2.5 text-sm text-ink ' +
  'outline-none transition placeholder:text-slate/60 ' +
  'focus:border-accent focus:ring-4 focus:ring-accent/10'
