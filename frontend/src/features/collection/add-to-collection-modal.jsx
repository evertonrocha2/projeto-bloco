import { useEffect, useMemo, useState } from 'react'
import { X, Search, Check } from 'lucide-react'
import { api } from '@/lib/api'
import { btnPrimary, field } from '@/lib/ui.js'
import GameCover from '@/ui/game-cover.jsx'

const STATUS_OPTIONS = ['Quero jogar', 'Jogando', 'Zerado', 'Largado']

// Popup pra adicionar um jogo na colecao. O fluxo: a pessoa busca/filtra pelo
// nome, escolhe um jogo da lista, define horas e status, e confirma. Nada e
// adicionado automaticamente - so quando ela clica em "Adicionar".
export default function AddToCollectionModal({ onClose, onAdded, initialGame = null }) {
  const [games, setGames] = useState([])
  const [search, setSearch] = useState('')
  // se veio um jogo pre-selecionado (ex: clicou no "+" de um card), ja comeca nele
  const [selected, setSelected] = useState(initialGame)
  const [hours, setHours] = useState(0)
  const [status, setStatus] = useState(STATUS_OPTIONS[0])
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api.listGames().then(setGames).catch(() => setGames([]))
  }, [])

  // filtra pelo texto digitado
  const filtered = useMemo(
    () => games.filter((game) => game.title.toLowerCase().includes(search.toLowerCase())),
    [games, search],
  )

  async function handleAdd() {
    if (!selected) return
    setSaving(true)
    setError(null)
    try {
      await api.addToCollection({ gameId: selected.id, hoursPlayed: Number(hours), status })
      onAdded()
      onClose()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    // fundo escurecido; clicar fora fecha
    <div className="fixed inset-0 z-50 bg-canvas/85 flex items-center justify-center p-4" onClick={onClose}>
      <div
        className="bg-canvas border border-line w-full max-w-lg max-h-[85vh] flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-5 py-4 border-b border-line">
          <h3 className="font-display font-bold text-lg text-ink">Adicionar jogo à coleção</h3>
          <button onClick={onClose} className="text-slate hover:text-ink cursor-pointer"><X size={20} /></button>
        </div>

        {/* busca */}
        <div className="p-4 border-b border-line">
          <div className="relative">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate/60" />
            <input
              autoFocus
              className="w-full bg-canvas border border-line pl-9 pr-3 py-2 text-sm text-ink outline-none focus:border-accent focus:ring-4 focus:ring-accent/10"
              placeholder="Buscar jogo pelo nome..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
          </div>
        </div>

        {/* lista de jogos */}
        <div className="overflow-y-auto p-2 flex-1">
          {filtered.length === 0 && <p className="text-sm text-slate p-3">Nenhum jogo encontrado.</p>}
          {filtered.map((game) => {
            const active = selected?.id === game.id
            return (
              <button
                key={game.id}
                onClick={() => setSelected(game)}
                className={
                  'w-full flex items-center gap-3 p-2 text-left transition cursor-pointer ' +
                  (active ? 'bg-accent-soft' : 'hover:bg-mist')
                }
              >
                <GameCover game={game} className="h-10 w-16 border border-line shrink-0" />
                <span className="flex-1 min-w-0">
                  <span className="block text-sm font-semibold text-ink truncate">{game.title}</span>
                  <span className="block text-xs text-slate truncate">{game.genre}</span>
                </span>
                {active && <Check size={16} className="text-accent shrink-0" />}
              </button>
            )
          })}
        </div>

        {/* rodape: so aparece quando um jogo esta selecionado */}
        {selected && (
          <div className="border-t border-line p-4 flex flex-wrap items-end gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-semibold text-slate">Horas</label>
              <input type="number" min="0" value={hours} onChange={(e) => setHours(e.target.value)} className={`${field} w-24`} />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-semibold text-slate">Status</label>
              <select value={status} onChange={(e) => setStatus(e.target.value)} className={`${field} w-40`}>
                {STATUS_OPTIONS.map((opcao) => <option key={opcao} value={opcao}>{opcao}</option>)}
              </select>
            </div>
            <button onClick={handleAdd} disabled={saving} className={`${btnPrimary} !py-2.5`}>
              {saving ? 'Adicionando...' : 'Adicionar'}
            </button>
            {error && <p className="text-danger text-sm font-medium w-full">{error}</p>}
          </div>
        )}
      </div>
    </div>
  )
}
