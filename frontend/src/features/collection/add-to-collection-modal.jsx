import { useState } from 'react'
import { X } from 'lucide-react'
import { api } from '@/lib/api'
import { btnPrimary, field } from '@/lib/ui.js'
import Select from '@/ui/select.jsx'
import GamePicker from '@/features/catalog/game-picker.jsx'
import { COLLECTION_STATUSES, DEFAULT_STATUS } from '@/lib/collection-status.js'

// Mesma conversao {code,label} -> {value,label} da tela de detalhe do jogo.
const STATUS_OPTIONS = COLLECTION_STATUSES.map(({ code, label }) => ({ value: code, label }))

// Popup pra adicionar um jogo na colecao. O fluxo: a pessoa busca/filtra pelo
// nome, escolhe um jogo da lista, define horas e status, e confirma. Nada e
// adicionado automaticamente - so quando ela clica em "Adicionar".
//
// A busca em si mora no GamePicker, compartilhado com a tela de lista tematica.
export default function AddToCollectionModal({ onClose, onAdded, initialGame = null }) {
  // se veio um jogo pre-selecionado (ex: clicou no "+" de um card), ja comeca nele
  const [selected, setSelected] = useState(initialGame)
  const [hours, setHours] = useState(0)
  // guarda o CODIGO do enum (QUERO_JOGAR), que e o que a API espera - e nao o
  // rotulo, que era o que ia antes e fazia a requisicao voltar 400.
  const [status, setStatus] = useState(DEFAULT_STATUS)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

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
          <h3 className="font-display text-lg text-ink">Adicionar jogo à coleção</h3>
          <button onClick={onClose} className="text-slate hover:text-ink cursor-pointer"><X size={20} /></button>
        </div>

        <GamePicker selected={selected} onSelect={setSelected} />

        {/* rodape: so aparece quando um jogo esta selecionado */}
        {selected && (
          <div className="border-t border-line p-4 flex flex-wrap items-end gap-3">
            <div className="flex flex-col gap-1">
              <label className="text-xs font-semibold text-slate">Horas</label>
              <input type="number" min="0" value={hours} onChange={(e) => setHours(e.target.value)} className={`${field} w-24`} />
            </div>
            <div className="flex flex-col gap-1">
              <label className="text-xs font-semibold text-slate">Status</label>
              <Select value={status} onChange={setStatus} options={STATUS_OPTIONS} className="w-40" />
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
