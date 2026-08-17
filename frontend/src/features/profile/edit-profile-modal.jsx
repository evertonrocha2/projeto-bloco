import { useState } from 'react'
import { X } from 'lucide-react'
import { api } from '@/lib/api'
import { btnPrimary, field } from '@/lib/ui.js'
import ImagePicker from '@/features/lists/image-picker.jsx'

// Editar o proprio perfil: bio, avatar e capa.
//
// Manda os tres campos SEMPRE, inclusive os vazios. E o contrato do PUT: um
// campo ausente nao significa "nao mexe", significa nulo - e e assim que se
// remove um avatar. Mandar so o que mudou faria "apagar" ser impossivel de
// expressar.
export default function EditProfileModal({ profile, onClose, onSaved }) {
  const [bio, setBio] = useState(profile.bio || '')
  const [avatarUrl, setAvatarUrl] = useState(profile.avatarUrl || '')
  const [bannerUrl, setBannerUrl] = useState(profile.bannerUrl || '')
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  async function handleSubmit(event) {
    event.preventDefault()
    setSaving(true)
    setError(null)

    try {
      onSaved(await api.updateMyProfile({ bio, avatarUrl, bannerUrl }))
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
          <h3 className="font-display text-lg text-ink">Editar perfil</h3>
          <button type="button" onClick={onClose} className="text-slate hover:text-ink cursor-pointer">
            <X size={20} />
          </button>
        </div>

        <div className="overflow-y-auto p-5 flex flex-col gap-5">
          <div className="flex flex-col gap-1.5">
            <label className="text-xs font-semibold text-slate">Bio</label>
            <textarea
              autoFocus
              rows={3}
              maxLength={500}
              value={bio}
              onChange={(e) => setBio(e.target.value)}
              placeholder="Quem joga aqui?"
              className={`${field} resize-y`}
            />
            <p className="text-xs text-slate/70">{bio.length}/500</p>
          </div>

          {/* O avatar e quadrado na tela, entao a previa tambem e - uma previa em
              paisagem mentiria sobre o enquadramento. */}
          <ImagePicker
            value={avatarUrl}
            onChange={setAvatarUrl}
            label="Avatar"
            aspect="aspect-square"
          />

          <ImagePicker value={bannerUrl} onChange={setBannerUrl} label="Capa do perfil" />

          {error && <p className="text-danger text-sm font-medium">{error}</p>}
        </div>

        <div className="border-t border-line p-4">
          <button type="submit" disabled={saving} className={`${btnPrimary} !py-2.5`}>
            {saving ? 'Salvando...' : 'Salvar perfil'}
          </button>
        </div>
      </form>
    </div>
  )
}
