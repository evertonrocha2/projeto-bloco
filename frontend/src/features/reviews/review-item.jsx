import { useState } from 'react'
import { Link } from 'react-router-dom'
import { ThumbsUp, ThumbsDown, MessageSquare, X } from 'lucide-react'
import { api } from '@/lib/api'
import StarRating from '@/ui/star-rating.jsx'
import ReplyThread from './reply-thread.jsx'
import { card, btnPrimary, field } from '@/lib/ui.js'

// Uma avaliacao com a conversa em volta dela: os dois placares e a arvore de
// respostas.
//
// O estado social e LOCAL deste componente. Votar ou responder nao recarrega a
// pagina do jogo - a resposta do servidor ja traz o placar recalculado, entao a
// tela troca so o que mudou. Recarregar tudo faria o clique num polegar mexer a
// pagina inteira e perder a posicao da rolagem.
export default function ReviewItem({ review, currentUser }) {
  const [social, setSocial] = useState(review.social || { positiveVotes: 0, negativeVotes: 0, myVote: null, replies: [] })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState(null)

  // Quando nao e nulo, o formulario de resposta esta aberto. Guarda a resposta
  // que esta sendo respondida (ou 'raiz' pra responder a avaliacao).
  const [replyingTo, setReplyingTo] = useState(null)
  const [replyText, setReplyText] = useState('')

  const isOwnReview = currentUser === review.username

  async function handleVote(type) {
    setBusy(true)
    setError(null)
    try {
      setSocial(await api.voteReview(review.id, type))
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  function abrirResposta(parent) {
    setReplyingTo(parent || 'raiz')
    // Responder alguem ja comeca com a mencao escrita. Importa mais do que
    // parece no nivel 3: la a resposta entra como IRMA, e sem o @ ninguem sabe a
    // quem ela se dirige.
    setReplyText(parent ? `@${parent.username} ` : '')
  }

  async function enviarResposta(event) {
    event.preventDefault()
    if (!replyText.trim()) return

    setBusy(true)
    setError(null)
    try {
      const parentId = replyingTo === 'raiz' ? null : replyingTo.id
      await api.replyToReview(review.id, { text: replyText.trim(), parentId })

      // A resposta nova vem sozinha, sem a arvore. Recarregar o jogo inteiro so
      // pra encaixa-la seria pesado; buscar de novo a pagina do jogo devolve a
      // arvore ja montada pelo servidor, que e a unica que conhece a regra de
      // aninhamento.
      const atualizado = await api.getGame(review.gameId)
      const estaReview = atualizado.reviews.find((item) => item.id === review.id)
      if (estaReview) {
        setSocial(estaReview.social)
      }

      setReplyingTo(null)
      setReplyText('')
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  async function apagarResposta(replyId) {
    setBusy(true)
    setError(null)
    try {
      await api.deleteReply(replyId)
      const atualizado = await api.getGame(review.gameId)
      const estaReview = atualizado.reviews.find((item) => item.id === review.id)
      if (estaReview) {
        setSocial(estaReview.social)
      }
    } catch (err) {
      setError(err.message)
    } finally {
      setBusy(false)
    }
  }

  // Polegar aceso quando e o meu voto. A cor separa os dois lados; o contorno
  // sozinho nao distinguiria "votei positivo" de "votei negativo" pra quem nao
  // percebe a diferenca entre os dois icones.
  const voteButton = (type, Icon, label, activeClass) => {
    const ativo = social.myVote === type

    return (
      <button
        onClick={() => handleVote(type)}
        disabled={busy || !currentUser || isOwnReview}
        title={
          isOwnReview
            ? 'Não dá pra votar na própria avaliação'
            : currentUser
              ? label
              : 'Entre pra votar'
        }
        className={
          'inline-flex items-center gap-1.5 text-xs transition-colors cursor-pointer ' +
          'disabled:cursor-not-allowed disabled:opacity-50 ' +
          (ativo ? activeClass : 'text-slate hover:text-ink')
        }
      >
        <Icon size={13} />
        <span className="tabular-nums">
          {type === 'POSITIVE' ? social.positiveVotes : social.negativeVotes}
        </span>
      </button>
    )
  }

  return (
    <li className={`${card} p-5`}>
      <div className="flex items-center justify-between gap-3 mb-2">
        <Link to={`/users/${review.username}`} className="font-semibold text-ink hover:text-accent">
          @{review.username}
        </Link>
        <StarRating value={review.rating} />
      </div>

      {review.text && (
        <p className="text-slate leading-relaxed whitespace-pre-wrap break-words">{review.text}</p>
      )}

      <div className="flex items-center gap-4 mt-3 pt-3 border-t border-line">
        {voteButton('POSITIVE', ThumbsUp, 'Achei útil', 'text-positive')}
        {voteButton('NEGATIVE', ThumbsDown, 'Não achei útil', 'text-danger')}

        {currentUser && (
          <button
            onClick={() => abrirResposta(null)}
            className="inline-flex items-center gap-1.5 text-xs text-slate hover:text-accent cursor-pointer"
          >
            <MessageSquare size={13} /> responder
          </button>
        )}
      </div>

      {error && <p className="text-danger text-xs mt-2">{error}</p>}

      {replyingTo && (
        <form onSubmit={enviarResposta} className="mt-3 flex flex-col gap-2">
          <textarea
            autoFocus
            rows={2}
            value={replyText}
            onChange={(e) => setReplyText(e.target.value)}
            maxLength={1000}
            placeholder="Escreva sua resposta..."
            className={`${field} resize-y`}
          />
          <div className="flex items-center gap-2">
            <button type="submit" disabled={busy} className={`${btnPrimary} !px-4 !py-1.5 text-xs`}>
              {busy ? 'Enviando...' : 'Responder'}
            </button>
            <button
              type="button"
              onClick={() => setReplyingTo(null)}
              className="inline-flex items-center gap-1 text-xs text-slate hover:text-ink cursor-pointer"
            >
              <X size={12} /> cancelar
            </button>
          </div>
        </form>
      )}

      {social.replies.length > 0 && (
        <div className="mt-4 pt-4 border-t border-line">
          <ReplyThread
            replies={social.replies}
            currentUser={currentUser}
            onReply={abrirResposta}
            onDelete={apagarResposta}
            busy={busy}
          />
        </div>
      )}
    </li>
  )
}
