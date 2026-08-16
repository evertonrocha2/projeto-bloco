import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { ArrowRight } from 'lucide-react'
import { api } from '@/lib/api'
import { collectGenres } from '@/lib/genres.js'
import GameCover from '@/ui/game-cover.jsx'
import Spinner from '@/ui/spinner.jsx'
import { useReveal } from '@/ui/use-reveal.js'
import { btnGhost } from '@/lib/ui.js'

const wrap = 'max-w-6xl mx-auto px-6'

// Mosaico de arte do catalogo.
//
// O catalogo lista jogos pra voce ler nota e descricao; esta pagina existe pra
// voce OLHAR. Sem card, sem estrela, sem texto por cima: so a arte, em blocos de
// tamanhos diferentes, com a informacao aparecendo apenas no hover.
//
// O ritmo dos tamanhos nao e aleatorio - seria bagunca. A cada seis pecas, a
// primeira ocupa quatro celulas e a quarta ocupa duas na horizontal. O padrao se
// repete, entao a grade tem cadencia mesmo com 12 ou com 200 jogos.
function spanDoIndice(indice) {
  const posicao = indice % 6
  if (posicao === 0) return 'col-span-2 row-span-2'
  if (posicao === 3) return 'col-span-2'
  return ''
}

export default function GalleryPage() {
  const [games, setGames] = useState([])
  const [loading, setLoading] = useState(true)
  const [generoAtivo, setGeneroAtivo] = useState('Todos')

  useEffect(() => {
    api.listGames()
      .then(setGames)
      .catch(() => setGames([]))
      .finally(() => setLoading(false))
  }, [])

  const generos = useMemo(
    () => ['Todos', ...collectGenres(games).sort()],
    [games],
  )

  const visiveis = useMemo(() => {
    if (generoAtivo === 'Todos') return games
    return games.filter((game) => (game.genre || '').includes(generoAtivo))
  }, [games, generoAtivo])

  useReveal([visiveis.length, generoAtivo])

  if (loading) return <div className={`${wrap} py-20`}><Spinner /></div>

  return (
    <>
      <section className="relative overflow-hidden border-b border-line">
        <div
          aria-hidden="true"
          className="absolute inset-0 bg-cover bg-center"
          style={{
            backgroundImage: 'url(/background.jpg)',
            filter: 'brightness(0.4) saturate(0.8)',
            maskImage: 'linear-gradient(to top, #000 0%, #000 35%, transparent 90%)',
            WebkitMaskImage: 'linear-gradient(to top, #000 0%, #000 35%, transparent 90%)',
          }}
        />
        <div aria-hidden="true" className="absolute inset-0 bg-gradient-to-t from-canvas/85 via-canvas/50 to-canvas" />

        <div className={`${wrap} relative py-20 sm:py-24`}>
          <p className="eyebrow">galeria</p>
          <h1 className="font-display font-bold text-4xl sm:text-6xl text-ink leading-[1.02] mt-4">
            Só a arte
          </h1>
          <p className="text-slate mt-4 max-w-lg leading-relaxed">
            {games.length} capas do catálogo, sem nota e sem texto atrapalhando.
            Passe o mouse pra ver o que é, clique pra abrir a ficha.
          </p>
        </div>
      </section>

      <div className={`${wrap} py-12`}>
        {/* Filtro por gênero: a galeria vira uma forma de navegar por estilo de
            arte, que costuma acompanhar o gênero. */}
        <div className="flex flex-wrap gap-2 mb-10">
          {generos.map((genero) => {
            const ativo = genero === generoAtivo
            return (
              <button
                key={genero}
                onClick={() => setGeneroAtivo(genero)}
                className={
                  'text-sm font-medium px-4 py-2 border transition-colors cursor-pointer ' +
                  (ativo
                    ? 'bg-accent text-canvas border-accent'
                    : 'bg-mist text-slate border-line hover:border-accent hover:text-accent')
                }
              >
                {genero}
              </button>
            )
          })}
        </div>

        {visiveis.length === 0 ? (
          <p className="text-slate">Nenhum jogo com esse gênero.</p>
        ) : (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-2 auto-rows-[minmax(0,1fr)]">
            {visiveis.map((game, indice) => (
              <Link
                key={game.id}
                to={`/games/${game.id}`}
                data-reveal
                style={{ transitionDelay: `${Math.min(indice, 12) * 45}ms` }}
                className={`group relative overflow-hidden border border-line ${spanDoIndice(indice)}`}
              >
                <GameCover
                  game={game}
                  className="h-full aspect-[3/4] transition-transform duration-700 group-hover:scale-[1.04]"
                />

                {/* A informacao so aparece no hover: em repouso a pagina e uma
                    parede de arte, sem rotulo competindo com a imagem. */}
                <div className="absolute inset-0 flex flex-col justify-end p-4 bg-gradient-to-t from-canvas via-canvas/40 to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                  <p className="eyebrow">{(game.genre || '').split(',')[0]}</p>
                  <h2 className="font-display font-semibold text-ink leading-tight mt-1.5">
                    {game.title}
                  </h2>
                </div>

                {/* Traço âmbar que cresce da esquerda no hover: o mesmo gesto do
                    item ativo no menu, repetido aqui. */}
                <span
                  aria-hidden="true"
                  className="absolute bottom-0 left-0 h-0.5 w-0 bg-accent transition-all duration-500 group-hover:w-full"
                />
              </Link>
            ))}
          </div>
        )}

        <div className="flex justify-center mt-14">
          <Link to="/games" className={btnGhost}>
            Ver com nota e descrição <ArrowRight size={18} />
          </Link>
        </div>
      </div>
    </>
  )
}
