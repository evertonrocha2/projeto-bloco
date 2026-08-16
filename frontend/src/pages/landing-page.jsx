import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import {
  Star, Library, Users, Search, Clock, Gamepad2,
  ArrowRight, ArrowUpRight, Quote,
} from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth.jsx'
import { collectGenres } from '@/lib/genres.js'
import GameCard from '@/features/catalog/game-card.jsx'
import GameCover from '@/ui/game-cover.jsx'
import Marquee from '@/ui/marquee.jsx'
import StarRating from '@/ui/star-rating.jsx'
import { useReveal } from '@/ui/use-reveal.js'
import { btnPrimary, btnGhost } from '@/lib/ui.js'

const wrap = 'max-w-6xl mx-auto px-6'

const FEATURES = [
  [Star, 'Nota e opinião', 'Zero a cinco estrelas e o que você realmente achou. Fica no seu perfil, pra você e pra quem quiser ler.'],
  [Library, 'Coleção', 'Marque o que é seu, com horas jogadas e em que pé está: quero jogar, jogando, zerado ou largado.'],
  [Users, 'Perfis abertos', 'Entre no perfil de qualquer jogador e veja a estante inteira dele. Sem seguir, sem pedir permissão.'],
  [Search, 'Busca e gênero', 'Ache pelo nome ou filtre por gênero. O catálogo vem de uma base real, com capa, ano e descrição.'],
  [Clock, 'Horas jogadas', 'Some quanto tempo cada jogo tomou da sua vida. Alguns números vão te surpreender.'],
  [Gamepad2, 'Um lugar só', 'Chega de print no celular e lista no bloco de notas. Seu histórico organizado de verdade.'],
]

const TESTIMONIALS = [
  ['Lucas M.', 'Parei de comprar jogo repetido na promoção. Só isso já pagou.'],
  ['Bea R.', 'Uso pra escolher o próximo da pilha. As notas da galera ajudam mais que trailer.'],
  ['Igor S.', 'Entro, marco o que zerei, saio. É exatamente isso que eu queria.'],
]

export default function LandingPage() {
  const { isAuthenticated } = useAuth()
  const [games, setGames] = useState([])

  useEffect(() => {
    api.listGames().then(setGames).catch(() => setGames([]))
  }, [])

  // Reobserva quando os jogos chegam: metade das secoes so existe depois disso.
  useReveal([games.length])

  // So os dez primeiros: sao atalhos numa vitrine, nao o filtro completo
  // (esse fica no catalogo).
  const categories = useMemo(() => collectGenres(games).slice(0, 10), [games])

  // O jogo mais bem avaliado, entre os que TEM avaliacao - sem o filtro, jogos
  // empatados em media 0 poluiriam a comparacao.
  const topRated = useMemo(() => {
    const avaliados = games.filter((game) => game.reviewCount > 0)
    const doMaiorParaOMenor = [...avaliados].sort(
      (a, b) => b.averageRating - a.averageRating,
    )
    return doMaiorParaOMenor[0]
  }, [games])

  const totalReviews = games.reduce(
    (total, game) => total + (game.reviewCount || 0),
    0,
  )
  const heroCovers = games.slice(0, 6)
  const preview = games.slice(0, 8)

  return (
    <>
      {/* ==================== HERO ==================== */}
      <section className="relative overflow-hidden border-b border-line min-h-[88vh] flex items-center">
        {/* A paisagem ocupa o hero inteiro e se APAGA subindo.
            Quatro camadas, cada uma com um trabalho:

            1. a imagem, forte na base (a arte e o assunto do site, entao ela
               aparece de verdade em vez de virar textura);
            2. mask-image apagando de baixo pra cima - mascara real, a imagem
               deixa de existir no topo em vez de ganhar um veu;
            3. uma camada DESFOCADA por cima da metade superior: e ela que da a
               sensacao de dissolucao, como se a cena perdesse foco ao subir;
            4. o gradiente da cor de fundo, que garante contraste do texto
               inclusive enquanto a imagem ainda carrega.

            aria-hidden em todas: sao atmosfera, nao informacao. */}
        <div
          aria-hidden="true"
          className="ambient-in pointer-events-none absolute inset-0 -z-20 bg-cover bg-bottom bg-no-repeat"
          style={{
            '--ambient-opacity': 0.85,
            backgroundImage: 'url(/background.jpg)',
            filter: 'brightness(0.62) saturate(0.85)',
            maskImage: 'linear-gradient(to top, #000 0%, #000 38%, rgba(0,0,0,0.35) 68%, transparent 92%)',
            WebkitMaskImage: 'linear-gradient(to top, #000 0%, #000 38%, rgba(0,0,0,0.35) 68%, transparent 92%)',
          }}
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-x-0 top-0 h-[62%] -z-10 backdrop-blur-[6px]"
          style={{
            maskImage: 'linear-gradient(to bottom, #000 30%, transparent 100%)',
            WebkitMaskImage: 'linear-gradient(to bottom, #000 30%, transparent 100%)',
          }}
        />
        <div
          aria-hidden="true"
          className="pointer-events-none absolute inset-0 -z-10 bg-gradient-to-t from-canvas via-canvas/45 to-canvas/90"
        />

        <div className={`${wrap} py-28 sm:py-36 w-full`}>
          <div className="hero-stagger max-w-3xl">
            <p className="eyebrow">catálogo · avaliações · coleção</p>
            <h1 className="font-display font-bold text-5xl sm:text-6xl md:text-7xl text-ink leading-[0.98] mt-6">
              Você já esqueceu<br />metade do que jogou.
            </h1>
            <p className="text-lg text-slate mt-7 max-w-xl leading-relaxed">
              O GameLog guarda o resto. Registre a nota, as horas e o que você achou —
              e tenha, pela primeira vez, a lista completa do que passou pelas suas mãos.
            </p>
            <div className="flex flex-wrap gap-3 mt-10">
              <Link to="/games" className={btnPrimary}>
                Ver o catálogo <ArrowRight size={18} />
              </Link>
              {!isAuthenticated && <Link to="/register" className={btnGhost}>Criar conta</Link>}
            </div>
          </div>
        </div>
      </section>

      {/* ==================== CAPAS ====================
          Saiu de dentro do hero: com a paisagem ao fundo, as duas coisas
          disputavam a mesma area. As capas entram apagadas e em escala de cinza,
          acendendo uma a uma no hover - seis capas coloridas lado a lado viravam
          uma faixa clara que rasgava o tema escuro. */}
      {heroCovers.length >= 3 && (
        <section className="border-b border-line">
          <div className={`${wrap} py-16`} data-reveal>
            <div className="flex items-baseline justify-between mb-6">
              <p className="eyebrow">no catálogo agora</p>
              <Link to="/games" className="eyebrow hover:text-accent transition-colors">
                ver todos
              </Link>
            </div>
            <div className="grid grid-cols-3 sm:grid-cols-6 border-t border-l border-line">
              {heroCovers.map((game) => (
                <Link
                  key={game.id}
                  to={`/games/${game.id}`}
                  className="group relative overflow-hidden border-r border-b border-line"
                  title={game.title}
                >
                  <GameCover
                    game={game}
                    className="aspect-[3/4] opacity-40 grayscale transition-all duration-700 group-hover:opacity-100 group-hover:grayscale-0 group-hover:scale-105"
                  />
                  <span className="absolute inset-x-0 bottom-0 p-3 text-xs font-medium text-ink bg-gradient-to-t from-canvas to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300 truncate">
                    {game.title}
                  </span>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      <Marquee />

      {/* ==================== NÚMEROS ====================
          Cada número fica CENTRADO na sua coluna, e as três colunas dividem a
          largura em partes iguais. Antes o conteúdo era alinhado à esquerda com
          padding solto, então cada célula tinha uma quantidade diferente de ar em
          volta e o conjunto parecia torto. */}
      <section className={`${wrap} py-16`}>
        <div className="grid grid-cols-1 sm:grid-cols-3 border-y border-line" data-reveal>
          {[
            [games.length, 'jogos no catálogo'],
            [totalReviews, 'avaliações publicadas'],
            [categories.length, 'gêneros pra explorar'],
          ].map(([valor, label]) => (
            <div
              key={label}
              className="flex flex-col items-center justify-center text-center py-12 px-4 border-b sm:border-b-0 sm:border-r border-line last:border-r-0 last:border-b-0"
            >
              <div className="font-display font-bold text-5xl sm:text-6xl text-accent tabular-nums leading-none">
                {valor}
              </div>
              <div className="eyebrow mt-3">{label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ==================== O QUE DÁ PRA FAZER ==================== */}
      <section className="bg-mist border-y border-line">
        <div className={`${wrap} py-24`}>
          <div className="max-w-2xl" data-reveal>
            <p className="eyebrow">o que dá pra fazer</p>
            <h2 className="font-display font-bold text-4xl sm:text-5xl text-ink leading-[1.05] mt-4">
              Seis coisas. Bem feitas.
            </h2>
            <p className="text-lg text-slate mt-5 leading-relaxed">
              Sem rede social, sem feed infinito, sem notificação pedindo pra voltar.
              É um diário de jogos — e a lista abaixo é tudo o que ele faz.
            </p>
          </div>
          <div className="grid md:grid-cols-2 lg:grid-cols-3 mt-14 border-t border-l border-line" data-reveal>
            {FEATURES.map(([Icon, title, desc], index) => (
              <div
                key={title}
                className="group p-8 border-r border-b border-line transition-colors hover:bg-canvas"
              >
                <div className="flex items-center justify-between">
                  <Icon size={22} className="text-accent" />
                  <span className="eyebrow tabular-nums">{String(index + 1).padStart(2, '0')}</span>
                </div>
                <h3 className="font-display font-bold text-xl text-ink mt-6">{title}</h3>
                <p className="text-slate text-sm mt-2 leading-relaxed">{desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ==================== EDITORIAL ==================== */}
      <section className={`${wrap} py-24`}>
        <div className="grid lg:grid-cols-[1fr_1.5fr] gap-12" data-reveal>
          <div>
            <p className="eyebrow">por que registrar</p>
            <h2 className="font-display font-bold text-3xl sm:text-4xl text-ink mt-4 leading-[1.08]">
              A gente joga muito e esquece quase tudo.
            </h2>
          </div>
          <div className="space-y-6 text-slate text-lg leading-relaxed">
            <p>
              Quantos jogos você zerou na vida? E quantos consegue mesmo lembrar — a
              história, a nota que daria hoje, se valeu as horas? Pra maioria da gente
              esse histórico evapora. Fica espalhado em print, conversa de grupo e numa
              memória que falha exatamente na hora de decidir o próximo.
            </p>
            <p>
              Registrar muda isso. Quando está anotado, dá pra ver a sua evolução de
              gosto, notar padrões desconfortáveis — <em>"eu juro que odeio souls-like,
              mas dei cinco estrelas pros três"</em> — e escolher o próximo jogo com
              alguma informação. O <em>backlog</em> para de assombrar quando ganha nome
              e ordem.
            </p>
            <p>
              E tem o lado coletivo. Avaliação boa é a que ajuda outra pessoa a decidir.
              Deixando a sua, você entra numa conversa maior: vê onde a galera concorda,
              acha joia escondida e encontra gente com gosto parecido com o seu.
            </p>
          </div>
        </div>
      </section>

      {/* ==================== PARALLAX ====================
          Tratamento diferente das outras duas artes: aqui a imagem fica FIXA
          enquanto a pagina rola por cima (background-attachment: fixed). O efeito
          e de olhar por uma janela em movimento - combina com a cena, que e
          alguem parado diante de um mundo grande.

          Sem mascara desta vez: a arte aparece inteira, contida por reguas em
          cima e embaixo. O que garante a leitura do texto e a camada escura no
          meio, nao um apagamento.

          background-attachment: fixed e ignorado no iOS e custa caro em mobile,
          entao no lugar dele o telefone recebe uma imagem normal, centrada -
          degrada pra algo bom em vez de quebrar. */}
      <section className="relative border-y border-line overflow-hidden">
        <div
          aria-hidden="true"
          className="absolute inset-0 bg-cover bg-center sm:bg-fixed"
          style={{
            backgroundImage: 'url(/background-4.jpg)',
            filter: 'brightness(0.42) saturate(0.8)',
          }}
        />
        <div aria-hidden="true" className="absolute inset-0 bg-canvas/45" />

        <div className={`${wrap} relative py-32 sm:py-40`} data-reveal>
          <div className="max-w-2xl">
            <p className="eyebrow">o catálogo</p>
            <h2 className="font-display font-bold text-4xl sm:text-5xl text-ink leading-[1.05] mt-4">
              Tem mais jogo aí fora do que você consegue jogar.
            </h2>
            <p className="text-lg text-slate mt-6 leading-relaxed">
              O que não falta é opção — falta saber qual vale o seu tempo. Comece
              registrando o que você já jogou: é assim que as indicações passam a
              fazer sentido.
            </p>
            <Link to="/galeria" className={`${btnGhost} mt-9`}>
              Ver a galeria <ArrowRight size={18} />
            </Link>
          </div>
        </div>
      </section>

      {/* ==================== EM DESTAQUE ====================
          Antes era uma grade de duas colunas: o poster (retrato) esticava a
          coluna da esquerda e deixava metade da direita vazia. Agora a capa e o
          FUNDO da faixa, com a informacao por cima - o poster aparece inteiro,
          em escala grande, e nao sobra buraco. */}
      {topRated && (
        <section className="border-y border-line">
          <div className="relative overflow-hidden" data-reveal>
            <div
              aria-hidden="true"
              className="absolute inset-0 bg-cover bg-center"
              style={{
                backgroundImage: topRated.coverUrl ? `url(${topRated.coverUrl})` : undefined,
                filter: 'brightness(0.28) saturate(0.7)',
              }}
            />
            <div
              aria-hidden="true"
              className="absolute inset-0 bg-gradient-to-r from-canvas via-canvas/85 to-canvas/30"
            />

            <div className={`${wrap} relative py-24 sm:py-28`}>
              <div className="grid md:grid-cols-[210px_1fr] gap-10 items-center">
                <GameCover
                  game={topRated}
                  className="aspect-[3/4] border border-line shadow-2xl"
                />
                <div className="max-w-xl">
                  <p className="eyebrow">o mais bem avaliado</p>
                  <h2 className="font-display font-bold text-4xl sm:text-5xl text-ink mt-4 leading-[1.03]">
                    {topRated.title}
                  </h2>
                  <div className="flex flex-wrap items-center gap-x-3 gap-y-2 mt-5">
                    <StarRating value={Math.round(topRated.averageRating)} size={18} />
                    <span className="font-display font-bold text-ink tabular-nums">
                      {topRated.averageRating.toFixed(1)}
                    </span>
                    <span className="text-slate text-sm">
                      · {topRated.reviewCount} {topRated.reviewCount === 1 ? 'avaliação' : 'avaliações'}
                      · {topRated.genre} · {topRated.releaseYear || 's/ data'}
                    </span>
                  </div>
                  {topRated.description && (
                    <p className="text-slate mt-5 line-clamp-3 leading-relaxed">
                      {topRated.description}
                    </p>
                  )}
                  <Link to={`/games/${topRated.id}`} className={`${btnPrimary} mt-8`}>
                    Abrir ficha <ArrowRight size={18} />
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </section>
      )}

      {/* ==================== EM ALTA ==================== */}
      {preview.length > 0 && (
        <section className={`${wrap} py-24`}>
          <div className="flex items-end justify-between mb-10" data-reveal>
            <div>
              <p className="eyebrow">em alta</p>
              <h2 className="font-display font-bold text-3xl sm:text-4xl text-ink mt-3">
                O que está sendo jogado
              </h2>
            </div>
            <Link to="/games" className="hidden sm:inline-flex items-center gap-1.5 eyebrow hover:text-accent transition-colors">
              ver tudo <ArrowRight size={14} />
            </Link>
          </div>
          <div className="grid gap-5 grid-cols-[repeat(auto-fill,minmax(220px,1fr))]" data-reveal>
            {preview.map((game) => <GameCard key={game.id} game={game} />)}
          </div>
        </section>
      )}

      {/* ==================== DEPOIMENTOS ==================== */}
      <section className="bg-mist border-y border-line">
        <div className={`${wrap} py-24`}>
          <div className="max-w-2xl" data-reveal>
            <p className="eyebrow">quem usa</p>
            <h2 className="font-display font-bold text-3xl sm:text-4xl text-ink mt-4 leading-[1.08]">
              Três pessoas que pararam de esquecer.
            </h2>
          </div>
          <div className="grid md:grid-cols-3 mt-12 border-t border-l border-line" data-reveal>
            {TESTIMONIALS.map(([name, quote]) => (
              <figure key={name} className="p-8 border-r border-b border-line">
                <Quote size={20} className="text-accent" />
                <blockquote className="font-display text-lg text-ink mt-5 leading-snug">
                  {quote}
                </blockquote>
                <figcaption className="flex items-center gap-3 mt-7">
                  <span className="grid place-items-center h-8 w-8 bg-accent-soft text-accent font-display font-bold text-sm border border-line">
                    {name.charAt(0)}
                  </span>
                  <span className="eyebrow">{name}</span>
                </figcaption>
              </figure>
            ))}
          </div>
        </div>
      </section>

      {/* ==================== NAVEGUE POR GÊNERO ==================== */}
      {categories.length > 0 && (
        <section className={`${wrap} py-24`}>
          <div data-reveal>
            <p className="eyebrow">navegue por gênero</p>
            <h2 className="font-display font-bold text-3xl sm:text-4xl text-ink mt-4 max-w-xl leading-[1.08]">
              Comece por algo que você já gosta.
            </h2>
            <div className="flex flex-wrap gap-2 mt-9">
              {categories.map((categoria) => (
                <Link
                  key={categoria}
                  to={`/games?genero=${encodeURIComponent(categoria)}`}
                  className="group inline-flex items-center gap-1.5 text-sm font-medium text-ink bg-mist border border-line px-4 py-2.5 transition-colors hover:border-accent hover:text-accent"
                >
                  {categoria}
                  <ArrowUpRight size={14} className="transition-transform group-hover:translate-x-0.5 group-hover:-translate-y-0.5" />
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ==================== CHAMADA FINAL ====================
          Era um retangulo em gradiente indigo com texto branco - a peca mais
          "template" da pagina. Vira uma faixa de largura total com o por do sol
          ao fundo, no mesmo tratamento do hero: imagem forte embaixo, dissolvendo
          pra cima. Fecha a pagina com o mesmo gesto que abriu. */}
      {!isAuthenticated && (
        <section className="relative overflow-hidden border-t border-line">
          <div
            aria-hidden="true"
            className="absolute inset-0 bg-cover bg-center"
            style={{
              backgroundImage: 'url(/background-2.jpg)',
              filter: 'brightness(0.55) saturate(0.9)',
              maskImage: 'linear-gradient(to top, #000 0%, #000 45%, transparent 95%)',
              WebkitMaskImage: 'linear-gradient(to top, #000 0%, #000 45%, transparent 95%)',
            }}
          />
          <div
            aria-hidden="true"
            className="absolute inset-0 bg-gradient-to-t from-canvas/85 via-canvas/40 to-canvas"
          />

          <div className={`${wrap} relative py-32 sm:py-40 text-center`} data-reveal>
            <p className="eyebrow">leva um minuto</p>
            <h2 className="font-display font-bold text-5xl sm:text-6xl md:text-7xl text-ink leading-[0.98] mt-6">
              Comece a<br />registrar hoje.
            </h2>
            <p className="text-lg text-slate mt-7 max-w-lg mx-auto leading-relaxed">
              Crie a conta, marque os cinco últimos jogos que você zerou e veja a
              sua estante começar a existir.
            </p>
            <Link to="/register" className={`${btnPrimary} mt-10 text-base px-8 py-4`}>
              Criar minha conta <ArrowRight size={18} />
            </Link>
          </div>
        </section>
      )}
    </>
  )
}
