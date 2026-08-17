import { useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Tag } from 'lucide-react'
import { api } from '@/lib/api'
import { useReveal } from '@/ui/use-reveal.js'
import Spinner from '@/ui/spinner.jsx'
import ListCard from '@/features/lists/list-card.jsx'

const wrap = 'max-w-5xl mx-auto px-6 py-12'

// Descoberta de listas por tag. E onde se cai ao clicar numa etiqueta.
//
// So publicas - a consulta do servidor ja garante isso, e de proposito: filtrar
// depois seria a versao que um refactor futuro esquece de filtrar, e esta e a
// tela em que vazar uma lista privada seria mais facil e mais silencioso, porque
// quem busca por tag nunca e o dono.
export default function ListsPage() {
  const [params] = useSearchParams()
  const tag = params.get('tag') || ''

  const [lists, setLists] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    setLists(null)
    setError(null)

    if (!tag) {
      setLists([])
      return
    }

    api.getListsByTag(tag).then(setLists).catch((erro) => setError(erro.message))
  }, [tag])

  useReveal([tag, lists?.length])

  return (
    <div className={wrap}>
      <p className="eyebrow mb-2">Listas</p>
      <h1 className="text-3xl text-ink leading-tight inline-flex items-center gap-2.5">
        <Tag size={22} className="text-accent" />
        {tag || 'sem etiqueta'}
      </h1>

      {error && <p className="text-danger font-medium mt-6">{error}</p>}
      {!error && lists === null && <div className="mt-8"><Spinner /></div>}

      {lists && lists.length === 0 && (
        <p className="text-slate mt-6">
          {tag
            ? 'Nenhuma lista pública com essa etiqueta ainda.'
            : 'Clique numa etiqueta de lista pra ver as outras que a usam.'}
        </p>
      )}

      {lists && lists.length > 0 && (
        <div className="grid gap-5 grid-cols-[repeat(auto-fill,minmax(280px,1fr))] mt-8" data-reveal-group>
          {lists.map((list) => <ListCard key={list.id} list={list} />)}
        </div>
      )}
    </div>
  )
}
