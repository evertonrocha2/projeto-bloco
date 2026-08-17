import { useEffect, useState } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import { ArrowLeft, Plus, Pencil, Trash2, Lock, X, Check } from 'lucide-react'
import { api } from '@/lib/api'
import { useAuth } from '@/lib/auth.jsx'
import { useReveal } from '@/ui/use-reveal.js'
import GameCover from '@/ui/game-cover.jsx'
import Spinner from '@/ui/spinner.jsx'
import GamePicker from '@/features/catalog/game-picker.jsx'
import ListEditor from '@/features/lists/list-editor.jsx'
import { btnPrimary, btnGhost, field, card } from '@/lib/ui.js'
import { resolveImageUrl } from '@/lib/image-url.js'

const wrap = 'max-w-4xl mx-auto px-6 py-12'

// Uma lista tematica aberta: cabecalho, tags, e os jogos com a nota de cada um.
//
// O jogo aparece deitado, e nao em grade de capas: a NOTA e o que faz uma lista
// tematica valer a leitura, e numa grade ela nao caberia. Uma parede de capas
// seria bonita e nao diria nada.
export default function ListDetailPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { username } = useAuth()

  const [list, setList] = useState(null)
  const [error, setError] = useState(null)
  const [editing, setEditing] = useState(false)
  const [adding, setAdding] = useState(false)
  const [busy, setBusy] = useState(false)

  // Qual item esta com a nota aberta pra edicao.
  const [editingNote, setEditingNote] = useState(null)
  const [noteDraft, setNoteDraft] = useState('')

  function load() {
    api.getList(id).then(setList).catch((erro) => setError(erro.message))
  }

  useEffect(() => {
    setList(null)
    setError(null)
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id])

  useReveal([list?.id, list?.items.length])

  const isOwner = username && list && username === list.owner

  async function adicionarJogo(game) {
    setBusy(true)
    try {
      setList(await api.addListItem(list.id, { gameId: game.id, note: null }))
      setAdding(false)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function removerJogo(itemId) {
    setBusy(true)
    try {
      setList(await api.removeListItem(list.id, itemId))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function salvarNota(itemId) {
    setBusy(true)
    try {
      setList(await api.updateListItemNote(list.id, itemId, noteDraft.trim() || null))
      setEditingNote(null)
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function apagarLista() {
    // Apagar leva os jogos e as notas junto, e nao ha como desfazer.
    if (!window.confirm(`Apagar a lista "${list.title}"? Isso não tem volta.`)) return

    try {
      await api.deleteList(list.id)
      navigate(`/users/${list.owner}`)
    } catch (err) {
      setError(err.message)
    }
  }

  // Lista privada de outra pessoa chega aqui como "nao encontrada" - o servidor
  // responde 404 de proposito, pra nao confirmar que ela existe.
  if (error) return <div className={wrap}><p className="text-danger font-medium">{error}</p></div>
  if (!list) return <div className={wrap}><Spinner /></div>

  return (
    <div className={wrap}>
      <Link
        to={`/users/${list.owner}`}
        className="inline-flex items-center gap-1.5 text-sm font-semibold text-slate hover:text-ink mb-6"
      >
        <ArrowLeft size={16} /> perfil de @{list.owner}
      </Link>

      {list.coverUrl && (
        <div className="relative aspect-[4/1] overflow-hidden border border-line mb-6">
          <img
            src={resolveImageUrl(list.coverUrl)}
            alt=""
            className="h-full w-full object-cover"
            style={{ filter: 'brightness(0.5) saturate(0.9)' }}
          />
        </div>
      )}

      <header className="mb-8">
        <div className="flex items-start justify-between gap-4 flex-wrap">
          <div className="min-w-0">
            <h1 className="text-3xl text-ink leading-tight inline-flex items-center gap-3">
              {list.title}
              {list.visibility === 'PRIVATE' && (
                <span className="inline-flex items-center gap-1 text-xs font-medium text-slate border border-line px-2 py-1">
                  <Lock size={11} /> privada
                </span>
              )}
            </h1>
            <p className="text-sm text-slate mt-1">
              por <Link to={`/users/${list.owner}`} className="text-accent font-semibold">@{list.owner}</Link>
              {' · '}{list.items.length} {list.items.length === 1 ? 'jogo' : 'jogos'}
            </p>
          </div>

          {isOwner && (
            <div className="flex gap-2">
              <button onClick={() => setEditing(true)} className={`${btnGhost} !px-4 !py-2 text-sm`}>
                <Pencil size={14} /> Editar
              </button>
              <button
                onClick={apagarLista}
                className={`${btnGhost} !px-4 !py-2 text-sm hover:!border-danger hover:!text-danger`}
              >
                <Trash2 size={14} />
              </button>
            </div>
          )}
        </div>

        {list.description && (
          <p className="text-slate leading-relaxed mt-4 max-w-2xl whitespace-pre-wrap">{list.description}</p>
        )}

        {list.tags.length > 0 && (
          <div className="flex flex-wrap gap-1.5 mt-4">
            {list.tags.map((tag) => (
              <Link
                key={tag}
                to={`/lists?tag=${encodeURIComponent(tag)}`}
                className="text-[0.7rem] font-medium text-accent bg-accent-soft border border-accent/25 px-2 py-0.5 hover:border-accent"
              >
                {tag}
              </Link>
            ))}
          </div>
        )}
      </header>

      {isOwner && (
        <button onClick={() => setAdding(true)} className={`${btnPrimary} !py-2.5 text-sm mb-6`}>
          <Plus size={16} /> Adicionar jogo
        </button>
      )}

      {list.items.length === 0 ? (
        <p className="text-slate text-sm">Essa lista ainda não tem jogos.</p>
      ) : (
        <ul className="flex flex-col gap-3" data-reveal-group>
          {list.items.map((item) => (
            <li key={item.id} className={`${card} p-4 flex gap-4`}>
              <Link to={`/games/${item.gameId}`} className="shrink-0">
                <span className="block h-24 w-16 overflow-hidden border border-line">
                  <GameCover
                    game={{ title: item.gameTitle, coverUrl: item.gameCoverUrl }}
                    className="h-full"
                  />
                </span>
              </Link>

              <div className="min-w-0 flex-1">
                <div className="flex items-start justify-between gap-3">
                  <Link
                    to={`/games/${item.gameId}`}
                    className="font-display text-ink hover:text-accent leading-tight"
                  >
                    {item.gameTitle}
                  </Link>

                  {isOwner && (
                    <div className="flex gap-2 shrink-0">
                      <button
                        onClick={() => {
                          setEditingNote(item.id)
                          setNoteDraft(item.note || '')
                        }}
                        title="Editar nota"
                        className="text-slate hover:text-accent cursor-pointer"
                      >
                        <Pencil size={13} />
                      </button>
                      <button
                        onClick={() => removerJogo(item.id)}
                        disabled={busy}
                        title="Tirar da lista"
                        className="text-slate hover:text-danger disabled:opacity-40 cursor-pointer"
                      >
                        <X size={14} />
                      </button>
                    </div>
                  )}
                </div>

                {editingNote === item.id ? (
                  <div className="mt-2 flex flex-col gap-2">
                    <textarea
                      autoFocus
                      rows={2}
                      maxLength={280}
                      value={noteDraft}
                      onChange={(e) => setNoteDraft(e.target.value)}
                      placeholder="Por que esse jogo está aqui?"
                      className={`${field} resize-y`}
                    />
                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => salvarNota(item.id)}
                        disabled={busy}
                        className={`${btnPrimary} !px-3 !py-1.5 text-xs`}
                      >
                        <Check size={12} /> Salvar
                      </button>
                      <button
                        onClick={() => setEditingNote(null)}
                        className="text-xs text-slate hover:text-ink cursor-pointer"
                      >
                        cancelar
                      </button>
                    </div>
                  </div>
                ) : (
                  item.note && (
                    <p className="text-sm text-slate leading-relaxed mt-1.5 whitespace-pre-wrap">
                      {item.note}
                    </p>
                  )
                )}
              </div>
            </li>
          ))}
        </ul>
      )}

      {editing && (
        <ListEditor list={list} onClose={() => setEditing(false)} onSaved={setList} />
      )}

      {adding && (
        <div
          className="fixed inset-0 z-50 bg-canvas/85 flex items-center justify-center p-4"
          onClick={() => setAdding(false)}
        >
          <div
            className="bg-canvas border border-line w-full max-w-lg max-h-[85vh] flex flex-col overflow-hidden"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="flex items-center justify-between px-5 py-4 border-b border-line">
              <h3 className="font-display text-lg text-ink">Adicionar à lista</h3>
              <button onClick={() => setAdding(false)} className="text-slate hover:text-ink cursor-pointer">
                <X size={20} />
              </button>
            </div>

            {/* exclude tira o que ja esta na lista: oferecer um jogo que so vai
                voltar como erro e fazer a pessoa descobrir depois de clicar. */}
            <GamePicker
              onSelect={adicionarJogo}
              exclude={list.items.map((item) => item.gameId)}
            />
          </div>
        </div>
      )}
    </div>
  )
}
