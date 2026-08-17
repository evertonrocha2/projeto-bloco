import { useState } from 'react'
import { X, Lock, Globe } from 'lucide-react'
import { api } from '@/lib/api'
import { btnPrimary, field } from '@/lib/ui.js'
import Select from '@/ui/select.jsx'
import TagInput from '@/ui/tag-input.jsx'
import ImagePicker from './image-picker.jsx'

const VISIBILITY_OPTIONS = [
  { value: 'PUBLIC', label: 'Pública — aparece no seu perfil' },
  { value: 'PRIVATE', label: 'Privada — só você vê' },
]

// Criar ou editar uma lista tematica.
//
// O mesmo componente serve pros dois: criar e editar preenchem exatamente os
// mesmos campos, e duas telas identicas so criariam a duvida de qual manter
// atualizada. Recebendo `list`, edita; sem ela, cria.
export default function ListEditor({ list, onClose, onSaved }) {
  const [title, setTitle] = useState(list?.title || '')
  const [description, setDescription] = useState(list?.description || '')
  const [coverUrl, setCoverUrl] = useState(list?.coverUrl || '')
  const [tags, setTags] = useState(list ? [...list.tags] : [])
  const [visibility, setVisibility] = useState(list?.visibility || 'PUBLIC')
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setError(null)

    const body = { title, description, coverUrl, tags, visibility }

    try {
      const salva = list
        ? await api.updateList(list.id, body)
        : await api.createList(body)

      onSaved(salva)
      onClose()
    } catch (err) {
      setError(err.message)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 bg-canvas/85 flex items-center justify-center p-4" onClick={onClose}>
      <form
        onSubmit={handleSubmit}
        className="bg-canvas border border-line w-full max-w-lg max-h-[85vh] flex flex-col overflow-hidden"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between px-5 py-4 border-b border-line">
          <h3 className="font-display text-lg text-ink">{list ? 'Editar lista' : 'Nova lista'}</h3>
          <button type="button" onClick={onClose} className="text-slate hover:text-ink cursor-pointer">
            <X size={20} />
          </button>
        </div>

        <div className="overflow-y-auto p-5 flex flex-col gap-4">
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate">Título</label>
            <input
              autoFocus
              required
              maxLength={120}
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Os que valeram cada hora"
              className={field}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate">Descrição</label>
            <textarea
              rows={3}
              maxLength={2000}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Por que essa lista existe?"
              className={`${field} resize-y`}
            />
          </div>

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate">Tags</label>
            <TagInput tags={tags} onChange={setTags} />
          </div>

          <ImagePicker value={coverUrl} onChange={setCoverUrl} label="Capa da lista" />

          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate">Quem vê</label>
            <Select value={visibility} onChange={setVisibility} options={VISIBILITY_OPTIONS} />
            <p className="inline-flex items-center gap-1.5 text-xs text-slate/70">
              {visibility === 'PRIVATE' ? <Lock size={11} /> : <Globe size={11} />}
              {visibility === 'PRIVATE'
                ? 'Some do seu perfil e da busca por tag.'
                : 'Qualquer pessoa com o link consegue abrir.'}
            </p>
          </div>

          {error && <p className="text-danger text-sm font-medium">{error}</p>}
        </div>

        <div className="border-t border-line p-4">
          <button type="submit" disabled={saving} className={`${btnPrimary} !py-2.5`}>
            {saving ? 'Salvando...' : list ? 'Salvar alterações' : 'Criar lista'}
          </button>
        </div>
      </form>
    </div>
  )
}
