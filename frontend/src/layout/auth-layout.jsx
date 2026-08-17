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
          className="group inline-flex items-center gap-2.5 font-display text-xl text-ink tracking-tight"
        >
          <span className="grid place-items-center h-7 w-7 bg-accent text-canvas transition-transform duration-300 group-hover:rotate-[-8deg]">
            <Gamepad2 size={16} />
          </span>
          GameLog
        </Link>
      </header>

      <main className="relative flex-1 flex items-center justify-center px-6 py-10">
        {/* Painel largo e translucido: o desfoque forte e o que deixa a arte
            atravessar sem prejudicar a leitura. Com fundo quase opaco a imagem
            sumia atras de um retangulo preto, e o painel estreito fazia os campos
            parecerem espremidos - num formulario que e a unica coisa da tela, isso
            passa sensacao de aperto. */}
        <div className="hero-stagger w-full max-w-2xl bg-canvas/55 backdrop-blur-2xl border border-line/80 p-8 sm:p-12">
          <p className="eyebrow">{eyebrow}</p>
          <h1 className="text-4xl sm:text-5xl text-ink leading-[1.03] mt-3">
            {title}
          </h1>
          <p className="text-slate mt-3 leading-relaxed">{subtitle}</p>
          <div className="mt-9">{children}</div>
        </div>
      </main>

      {footer && (
        <div className="relative px-6 pb-10 text-center">{footer}</div>
      )}
    </div>
  )
}
