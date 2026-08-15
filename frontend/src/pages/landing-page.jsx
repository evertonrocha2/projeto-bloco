import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Star, Library, Users, Search, Clock, Gamepad2,
  ArrowRight, ArrowUpRight, Quote, Sparkles,
} from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth.jsx'
import GameCard from '@/features/catalog/game-card.jsx'
import Marquee from '@/ui/marquee.jsx'
import StarRating from '@/ui/star-rating.jsx'
import { btnPrimary, btnGhost, btnAccent, card } from '@/lib/ui.js'

const wrap = 'max-w-6xl mx-auto px-6'

const FEATURES = [
  [Star, 'Avaliações honestas', 'Dê uma nota de 0 a 5 e escreva o que realmente achou de cada jogo. Sua opinião fica registrada no seu perfil.'],
  [Library, 'Sua coleção', 'Marque os jogos como seus, anote as horas jogadas e o status — de "quero jogar" a "zerado".'],
  [Users, 'Perfis públicos', 'Visite o perfil de qualquer jogador e veja as avaliações e a coleção que ele montou.'],
  [Search, 'Catálogo gigante', 'Busque por nome e filtre por gênero. Os títulos vêm de uma base de dados real e atualizada.'],
  [Clock, 'Horas jogadas', 'Acompanhe quanto tempo cada jogo tomou da sua vida — pra o bem ou pro mal.'],
  [Gamepad2, 'Tudo num lugar', 'Pare de espalhar suas notas em prints e blocos de papel. Seu histórico organizado de verdade.'],
]

const TESTIMONIALS = [
  ['Lucas M.', 'Finalmente parei de esquecer o que eu já tinha jogado. Minha coleção tá toda aqui.'],
  ['Bea R.', 'Uso pra decidir o próximo jogo da pilha. As notas da galera ajudam demais.'],
  ['Igor S.', 'Simples e direto. Entro, registro o que zerei e pronto. É isso que eu queria.'],
]

export default function LandingPage() {
  const { isAuthenticated } = useAuth()
  const [games, setGames] = useState([])

  useEffect(() => {
    api.listGames().then(setGames).catch(() => setGames([]))
  }, [])

  const categories = useMemo(() => {
    const set = new Set()
    games.forEach((g) => (g.genre || '').split(',').forEach((p) => {
      const n = p.trim()
      if (n) set.add(n)
    }))
    return Array.from(set).slice(0, 10)
  }, [games])

  const topRated = useMemo(
    () => games.filter((g) => g.reviewCount > 0).sort((a, b) => b.averageRating - a.averageRating)[0],
    [games],
  )
  const totalReviews = games.reduce((s, g) => s + (g.reviewCount || 0), 0)
  const heroCovers = games.slice(0, 6)
  const preview = games.slice(0, 8)

  return (
    <>
      {/* HERO */}
      <section className="relative overflow-hidden border-b border-line">
        <div className="absolute inset-0 -z-10 bg-gradient-to-b from-accent-soft/70 via-canvas to-canvas" />
        <div className={`${wrap} py-24 sm:py-32`}>
          <div className="max-w-3xl">
            <span className="inline-flex items-center gap-2 text-sm font-semibold text-accent bg-accent-soft rounded-full px-3 py-1">
              <Sparkles size={15} /> Sua estante de jogos, organizada
            </span>
            <h1 className="font-display font-extrabold text-4xl sm:text-5xl md:text-6xl text-ink leading-[1.05] mt-6">
              Tudo o que você jogou, num só lugar.
            </h1>
            <p className="text-base sm:text-lg text-slate mt-5 max-w-2xl">
              O GameLog é onde você registra, avalia e organiza seus jogos. Monte sua
              coleção, acompanhe as horas, leia o que outros jogadores acharam e
              descubra o próximo título da sua lista.
            </p>
            <div className="flex flex-wrap gap-3 mt-9">
              <Link to="/games" className={btnPrimary}>
                Explorar catálogo <ArrowRight size={18} />
              </Link>
              {!isAuthenticated && <Link to="/register" className={btnGhost}>Criar conta grátis</Link>}
            </div>
          </div>

          {/* visual: colagem de capas reais do catalogo */}
          {heroCovers.length >= 3 && (
            <div className="mt-16 grid grid-cols-3 sm:grid-cols-6 gap-3">
              {heroCovers.map((g, i) => (
                <Link
                  key={g.id}
                  to={`/games/${g.id}`}
                  className={`rounded-xl overflow-hidden border border-line transition hover:border-accent ${i % 2 ? 'sm:translate-y-4' : ''}`}
                >
                  <img src={g.coverUrl} alt={g.title} className="w-full aspect-[3/4] object-cover" />
                </Link>
              ))}
            </div>
          )}
        </div>
      </section>

      <Marquee />

      {/* STATS */}
      <section className={`${wrap} py-16`}>
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 text-center">
          {[
            [games.length, 'jogos no catálogo'],
            [totalReviews, 'avaliações publicadas'],
            [categories.length, 'gêneros pra explorar'],
          ].map(([n, label]) => (
            <div key={label}>
              <div className="font-display font-extrabold text-4xl text-ink">{n}</div>
              <div className="text-slate mt-1">{label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* FEATURES */}
      <section className="bg-mist border-y border-line">
        <div className={`${wrap} py-24`}>
          <div className="max-w-2xl">
            <h2 className="font-display font-extrabold text-3xl sm:text-4xl text-ink leading-[1.15]">
              Tudo o que você precisa. Nada que você não precisa.
            </h2>
            <p className="text-lg text-slate mt-4">
              O GameLog foi pensado pra ser o seu diário de jogos: rápido de usar,
              fácil de consultar e organizado do jeito que a sua memória nunca foi.
            </p>
          </div>
          <div className="grid gap-5 md:grid-cols-2 lg:grid-cols-3 mt-12">
            {FEATURES.map(([Icon, title, desc]) => (
              <div key={title} className={`${card} p-6`}>
                <span className="grid place-items-center h-11 w-11 rounded-xl bg-accent-soft text-accent">
                  <Icon size={20} />
                </span>
                <h3 className="font-display font-bold text-lg text-ink mt-4">{title}</h3>
                <p className="text-slate text-sm mt-1.5 leading-relaxed">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* EDITORIAL / ARTIGO */}
      <section className={`${wrap} py-24`}>
        <div className="grid lg:grid-cols-[1fr_1.4fr] gap-12">
          <div>
            <span className="text-sm font-semibold text-accent">POR QUE REGISTRAR</span>
            <h2 className="font-display font-extrabold text-3xl text-ink mt-3 leading-tight">
              A gente joga muito e esquece quase tudo.
            </h2>
          </div>
          <div className="space-y-5 text-slate text-lg leading-relaxed">
            <p>
              Quantos jogos você já zerou na vida? E quantos você realmente lembra — a
              história, a nota que daria, o quanto curtiu? Pra maioria da gente, esse
              histórico simplesmente evapora. Fica espalhado em prints, conversas de
              grupo e naquela memória que insiste em falhar bem na hora.
            </p>
            <p>
              Manter um registro muda isso. Quando você anota o que jogou, vira dono da
              sua própria trajetória: dá pra ver sua evolução de gosto, perceber padrões
              ("eu juro que odeio souls-like, mas dei 5 estrelas pros três"), e tomar
              decisões melhores sobre o que jogar a seguir. Aquela pilha de jogos não
              jogados — o famoso <em>backlog</em> — finalmente para de te assombrar,
              porque agora ela tem nome, ordem e prioridade.
            </p>
            <p>
              E tem o lado social. Avaliação boa é aquela que ajuda outra pessoa a
              decidir. Ao deixar sua nota e seu comentário, você entra numa conversa
              maior: vê no que a galera concorda, descobre joias escondidas e acha
              gente com um gosto parecido com o seu pra seguir. É disso que o GameLog
              é feito — não de algoritmo, mas de gente registrando o que jogou.
            </p>
          </div>
        </div>
      </section>

      {/* CATEGORIAS */}
      {categories.length > 0 && (
        <section className="bg-mist border-y border-line">
          <div className={`${wrap} py-20`}>
            <h2 className="font-display font-extrabold text-2xl sm:text-3xl text-ink">Navegue por gênero</h2>
            <p className="text-slate text-lg mt-3 mb-8 max-w-2xl">
              Cada jogo entra com seus gêneros. Comece por um tema que você curte e veja
              o que o catálogo tem a oferecer.
            </p>
            <div className="flex flex-wrap gap-3">
              {categories.map((cat) => (
                <Link
                  key={cat}
                  to={`/games?genero=${encodeURIComponent(cat)}`}
                  className="inline-flex items-center gap-1.5 text-sm font-semibold text-ink bg-canvas border border-line rounded-full px-4 py-2 transition hover:border-accent hover:text-accent"
                >
                  {cat} <ArrowUpRight size={15} />
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* MAIS BEM AVALIADO */}
      {topRated && (
        <section className={`${wrap} py-24`}>
          <span className="text-sm font-semibold text-accent">EM DESTAQUE</span>
          <h2 className="font-display font-extrabold text-3xl text-ink mt-3 mb-10">O mais bem avaliado do momento</h2>
          <div className={`${card} overflow-hidden grid md:grid-cols-2`}>
            <img src={topRated.coverUrl} alt={topRated.title} className="w-full h-full object-cover aspect-video md:aspect-auto" />
            <div className="p-8 sm:p-10">
              <div className="flex items-center gap-2">
                <StarRating value={Math.round(topRated.averageRating)} size={18} />
                <span className="text-slate font-medium">{topRated.averageRating.toFixed(1)} / 5 · {topRated.reviewCount} avaliações</span>
              </div>
              <h3 className="font-display font-extrabold text-2xl text-ink mt-4">{topRated.title}</h3>
              <p className="text-slate mt-2">{topRated.genre} · {topRated.releaseYear || 's/ data'}</p>
              {topRated.description && <p className="text-slate mt-4 line-clamp-4 leading-relaxed">{topRated.description}</p>}
              <Link to={`/games/${topRated.id}`} className={`${btnPrimary} mt-7`}>
                Ver detalhes <ArrowRight size={18} />
              </Link>
            </div>
          </div>
        </section>
      )}

      {/* EM ALTA */}
      {preview.length > 0 && (
        <section className="bg-mist border-y border-line">
          <div className={`${wrap} py-20`}>
            <div className="flex items-end justify-between mb-8">
              <div>
                <h2 className="font-display font-extrabold text-2xl sm:text-3xl text-ink">Em alta agora</h2>
                <p className="text-slate text-lg mt-2">Os títulos mais populares do catálogo neste momento.</p>
              </div>
              <Link to="/games" className="hidden sm:inline-flex items-center gap-1 text-sm font-semibold text-accent">
                ver tudo <ArrowRight size={16} />
              </Link>
            </div>
            <div className="grid gap-6 grid-cols-[repeat(auto-fill,minmax(220px,1fr))]">
              {preview.map((game) => <GameCard key={game.id} game={game} />)}
            </div>
          </div>
        </section>
      )}

      {/* DEPOIMENTOS */}
      <section className={`${wrap} py-24`}>
        <div className="text-center max-w-2xl mx-auto">
          <h2 className="font-display font-extrabold text-3xl sm:text-4xl text-ink leading-[1.15]">Quem usa, recomenda</h2>
          <p className="text-lg text-slate mt-4">
            Jogadores de verdade usando o GameLog pra finalmente colocar a vida gamer em ordem.
          </p>
        </div>
        <div className="grid gap-5 md:grid-cols-3 mt-12">
          {TESTIMONIALS.map(([name, quote]) => (
            <div key={name} className={`${card} p-7`}>
              <Quote size={24} className="text-accent" />
              <p className="text-ink mt-4 leading-relaxed">“{quote}”</p>
              <div className="flex items-center gap-3 mt-6">
                <span className="grid place-items-center h-9 w-9 rounded-full bg-accent-soft text-accent font-bold text-sm">
                  {name.charAt(0)}
                </span>
                <span className="font-semibold text-ink text-sm">{name}</span>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* CTA FINAL */}
      {!isAuthenticated && (
        <section className={`${wrap} pb-24`}>
          <div className="rounded-3xl bg-gradient-to-br from-accent to-indigo-600 px-8 sm:px-16 py-16 text-center overflow-hidden">
            <h2 className="font-display font-extrabold text-3xl sm:text-4xl text-white leading-[1.15]">Comece a registrar hoje</h2>
            <p className="text-white/80 text-lg mt-4 max-w-xl mx-auto">
              Crie sua conta gratuita, monte sua coleção e nunca mais esqueça aquele
              jogo que você jurou que ia terminar.
            </p>
            <Link to="/register" className={`${btnGhost} mt-8 !bg-white !border-white`}>
              Criar minha conta <ArrowRight size={18} />
            </Link>
          </div>
        </section>
      )}
    </>
  )
}
