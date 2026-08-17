import { Link } from 'react-router-dom'
import { CornerDownRight, Trash2 } from 'lucide-react'

// Uma resposta e tudo o que pende dela.
//
// Componente recursivo, espelhando a estrutura que o servidor ja manda montada.
// A profundidade e limitada em 3 la, entao a recursao aqui e rasa por construcao.
//
// A indentacao vem de padding + uma regua de 1px a esquerda, e nao de margem: a
// regua e o que deixa visivel A QUEM cada resposta se dirige quando ha varias no
// mesmo nivel. Sem ela, quatro respostas alinhadas parecem todas responder a
// avaliacao.
export default function ReplyThread({ replies, currentUser, onReply, onDelete, busy }) {
  if (!replies || replies.length === 0) {
    return null
  }

  return (
    <ul className="flex flex-col gap-3">
      {replies.map((reply) => (
        <li key={reply.id}>
          <div className="flex items-start gap-2">
            <CornerDownRight size={13} className="mt-1 shrink-0 text-slate/50" />

            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-2 flex-wrap">
                {/* A lapide nao leva link: o texto sumiu, e mandar pro perfil de
                    quem apagou daria destaque justamente a quem se retirou. */}
                {reply.deleted ? (
                  <span className="text-sm text-slate/60">resposta removida</span>
                ) : (
                  <Link
                    to={`/users/${reply.username}`}
                    className="text-sm font-semibold text-ink hover:text-accent"
                  >
                    @{reply.username}
                  </Link>
                )}

                {!reply.deleted && currentUser && (
                  <>
                    <button
                      onClick={() => onReply(reply)}
                      className="text-xs text-slate hover:text-accent cursor-pointer"
                    >
                      responder
                    </button>

                    {reply.username === currentUser && (
                      <button
                        onClick={() => onDelete(reply.id)}
                        disabled={busy}
                        title="Apagar resposta"
                        className="text-slate hover:text-danger disabled:opacity-40 cursor-pointer"
                      >
                        <Trash2 size={12} />
                      </button>
                    )}
                  </>
                )}
              </div>

              {!reply.deleted && (
                <p className="text-sm text-slate leading-relaxed mt-0.5 whitespace-pre-wrap break-words">
                  {reply.text}
                </p>
              )}

              {/* As filhas. A regua a esquerda liga visualmente cada galho ao
                  seu pai. */}
              {reply.children.length > 0 && (
                <div className="mt-3 pl-3 border-l border-line">
                  <ReplyThread
                    replies={reply.children}
                    currentUser={currentUser}
                    onReply={onReply}
                    onDelete={onDelete}
                    busy={busy}
                  />
                </div>
              )}
            </div>
          </div>
        </li>
      ))}
    </ul>
  )
}
