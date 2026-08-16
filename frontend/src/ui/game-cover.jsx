import { useState } from 'react'

// A capa de um jogo, com um plano B quando ela nao existe ou nao carrega.
//
// Por que isso e componente e nao uma <img> solta: catalogo de jogos SEMPRE tem
// buraco. A API da RAWG as vezes devolve o jogo sem imagem, e a lista de reserva
// (usada quando a RAWG nao responde) tem titulos cuja capa nao esta em nenhum
// host confiavel. O icone de imagem quebrada do navegador nessas horas parece
// defeito da aplicacao.
//
// O plano B nao e um cinza vazio: e o titulo desenhado na fonte de display sobre
// o painel escuro. Fica coerente com o resto da interface, ao ponto de nao parecer
// falha - so uma capa mais sobria.
export default function GameCover({ game, className = '', sizes }) {
  // Comeca em erro quando nem ha URL, pulando direto pro plano B.
  const [falhou, setFalhou] = useState(!game?.coverUrl)

  const base = `block w-full object-cover bg-mist ${className}`

  if (falhou) {
    return (
      <div
        className={`${base} flex flex-col justify-end gap-2 p-4 border-b border-line`}
        // A imagem some, mas a informacao nao: quem usa leitor de tela ouve o
        // titulo do mesmo jeito.
        role="img"
        aria-label={game?.title ? `${game.title} — sem capa disponível` : 'Sem capa disponível'}
      >
        <span className="block h-px w-8 bg-accent" aria-hidden="true" />
        <span className="font-display font-semibold text-ink leading-tight line-clamp-3">
          {game?.title}
        </span>
        <span className="eyebrow">sem capa</span>
      </div>
    )
  }

  return (
    <img
      src={game.coverUrl}
      alt={game.title}
      loading="lazy"
      sizes={sizes}
      onError={() => setFalhou(true)}
      className={base}
    />
  )
}
