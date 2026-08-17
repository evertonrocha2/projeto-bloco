import { useEffect, useMemo, useState } from 'react'
import { Search, Check } from 'lucide-react'
import { api } from '@/lib/api'
import GameCover from '@/ui/game-cover.jsx'

// Escolher um jogo do catalogo: campo de busca + lista clicavel.
//
// Nasceu dentro do modal de adicionar a colecao. Saiu de la quando a tela de
// lista tematica precisou da mesma coisa - a alternativa era o mesmo filtro
// escrito duas vezes, que e exatamente como os tres cards de jogo divergiram.
//
// Nao decide o que fazer com a escolha: avisa quem chamou por onSelect. O modal
// de colecao pede horas e status depois; a lista pede uma nota. A busca nao
// precisa saber de nenhum dos dois.
export default function GamePicker({ selected, onSelect, exclude = [] }) {
  const [games, setGames] = useState([])
  const [search, setSearch] = useState('')

  useEffect(() => {
    api.listGames().then(setGames).catch(() => setGames([]))
  }, [])

  const filtered = useMemo(() => {
    const termo = search.toLowerCase()

    return games
      // exclude tira o que ja esta na lista: oferecer um jogo que so vai voltar
      // como "esse jogo ja esta na lista" e fazer a pessoa descobrir o erro
      // depois de clicar.
      .filter((game) => !exclude.includes(game.id))
      .filter((game) => game.title.toLowerCase().includes(termo))
  }, [games, search, exclude])

  return (
    <>
      <div className="p-4 border-b border-line">
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate/60" />
          <input
            autoFocus
            className="w-full bg-canvas border border-line pl-9 pr-3 py-2 text-sm text-ink outline-none focus:border-accent"
            placeholder="Buscar jogo pelo nome..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      <div className="overflow-y-auto p-2 flex-1">
        {filtered.length === 0 && <p className="text-sm text-slate p-3">Nenhum jogo encontrado.</p>}

        {filtered.map((game) => {
          const active = selected?.id === game.id

          return (
            <button
              key={game.id}
              type="button"
              onClick={() => onSelect(game)}
              className={
                'w-full flex items-center gap-3 p-2 text-left transition cursor-pointer ' +
                (active ? 'bg-accent-soft' : 'hover:bg-mist')
              }
            >
              {/* Conteiner de tamanho fixo com a capa preenchendo por dentro. O
                  GameCover traz w-full na base, entao passar w-10 direto nele
                  deixava as duas larguras competindo e a capa esticava. */}
              <span className="block h-14 w-10 shrink-0 overflow-hidden border border-line">
                <GameCover game={game} className="h-full" />
              </span>
              <span className="flex-1 min-w-0">
                <span className="block text-sm font-semibold text-ink truncate">{game.title}</span>
                <span className="block text-xs text-slate truncate">{game.genre}</span>
              </span>
              {active && <Check size={16} className="text-accent shrink-0" />}
            </button>
          )
        })}
      </div>
    </>
  )
}
