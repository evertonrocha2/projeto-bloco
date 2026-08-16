import { Link } from 'react-router-dom'
import { Gamepad2 } from 'lucide-react'

// Moldura das telas de entrar e criar conta.
//
// Tela de acesso e um momento de foco: a pessoa veio fazer UMA coisa. Por isso
// aqui nao ha rodape, nao ha menu e nao ha nada pra clicar alem do formulario e
// da volta pro inicio.
//
// A arte ocupa a tela inteira e o formulario flutua sobre ela num painel escuro.
// Em telas estreitas a arte continua ao fundo, so que mais apagada - o
// formulario nao pode disputar legibilidade com ela em nenhum tamanho.
export default function AuthLayout({ eyebrow, title, subtitle, children, footer }) {
  return (
    <div className="relative min-h-screen flex flex-col">
      {/* Arte de fundo, cobrindo tudo */}
      <div
        aria-hidden="true"
        className="ambient-in fixed inset-0 -z-20 bg-cover bg-center"
        style={{
          '--ambient-opacity': 1,
          backgroundImage: 'url(/background-3.jpg)',
          filter: 'brightness(0.55) saturate(0.95)',
        }}
      />
      {/* Escurece na direcao do painel pra garantir contraste do texto sobre
          qualquer parte da arte, inclusive a mais clara (o anel em chamas). */}
      <div
        aria-hidden="true"
        className="fixed inset-0 -z-10 bg-gradient-to-br from-canvas via-canvas/70 to-canvas/40"
      />

      {/* Cabecalho minimo: so a marca, levando de volta pro inicio. */}
      <header className="relative px-6 sm:px-10 py-7">
        <Link
          to="/"
          className="group inline-flex items-center gap-2.5 font-display font-bold text-xl text-ink tracking-tight"
        >
          <span className="grid place-items-center h-8 w-8 bg-accent text-canvas transition-transform duration-300 group-hover:rotate-[-8deg]">
            <Gamepad2 size={18} />
          </span>
          GameLog
        </Link>
      </header>

      <main className="relative flex-1 flex items-center justify-center px-6 py-10">
        <div className="hero-stagger w-full max-w-md bg-canvas/85 backdrop-blur-md border border-line p-8 sm:p-10">
          <p className="eyebrow">{eyebrow}</p>
          <h1 className="font-display font-bold text-4xl text-ink leading-[1.05] mt-3">
            {title}
          </h1>
          <p className="text-slate mt-3 leading-relaxed">{subtitle}</p>
          <div className="mt-8">{children}</div>
        </div>
      </main>

      {footer && (
        <div className="relative px-6 pb-10 text-center">{footer}</div>
      )}
    </div>
  )
}
