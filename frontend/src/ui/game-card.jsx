import { Link } from 'react-router-dom'
import GameCover from './game-cover.jsx'

// A casca de todo card de jogo do app.
//
// Existiam tres deles, quase iguais e divergentes em tudo que se mede: padding
// p-4, p-3 e p-3.5; titulo ora em corpo base, ora text-sm; a mesma etiqueta em
// posicoes diferentes. Ninguem decidiu isso - cada card foi escrito na tela onde
// nasceu, copiando o anterior de memoria. O resultado e uma grade que parece
// montada por tres pessoas que nao conversaram.
//
// A casca fixa o que deve ser igual em todo lugar - proporcao da capa, zoom no
// hover, regua entre capa e texto, medida de padding, corpo do titulo - e abre
// ESPACOS NOMEADOS pro que muda de tela pra tela.
//
// Espacos, e nao props de variante ("variant='catalog'"), porque os tres usos
// diferem justamente nas sobreposicoes: o catalogo poe genero e um botao de
// adicionar, a recomendacao poe pontuacao e dois botoes de feedback, a colecao
// poe status e horas. Uma prop de variante teria que enumerar essas combinacoes,
// e o quarto uso - as listas tematicas - obrigaria a mexer na casca de novo.
//
// O que vai em cada espaco:
//   overlayTopLeft  selo sobre a capa, canto superior esquerdo (genero)
//   overlayBottom   faixa sobre a base da capa (pontuacao)
//   actions         botoes no canto superior direito, FORA do link
//   footer          qualquer coisa abaixo do titulo (nota, status, horas)
//
// actions fica fora do <Link> de proposito, como irmao dele: um <button> dentro
// de um <a> e HTML invalido, e o navegador desmonta a arvore de um jeito que
// costuma engolir o clique do botao.
export default function GameCard({
  to,
  game,
  overlayTopLeft,
  overlayBottom,
  actions,
  footer,
  className = '',
}) {
  return (
    <div className={`relative group ${className}`}>
      <Link
        to={to}
        className="block border border-line bg-mist transition-colors hover:border-accent"
      >
        <div className="relative overflow-hidden">
          <GameCover
            game={game}
            className="aspect-[3/4] transition-transform duration-700 group-hover:scale-105"
          />

          {overlayTopLeft && (
            <span className="absolute top-0 left-0 text-[0.7rem] font-medium tracking-wide px-2.5 py-1 bg-canvas/90 text-ink border-r border-b border-line backdrop-blur">
              {overlayTopLeft}
            </span>
          )}

          {overlayBottom && (
            <div className="absolute bottom-0 left-0 px-2 py-0.5 bg-canvas/90 border-t border-r border-line backdrop-blur">
              {overlayBottom}
            </div>
          )}
        </div>

        <div className="p-4 border-t border-line">
          {/* title= no elemento pro nome completo aparecer no hover: truncate
              esconde o fim, e jogo tem titulo longo o suficiente pra isso doer. */}
          <h3 className="font-display text-sm text-ink leading-tight truncate" title={game.title}>
            {game.title}
          </h3>

          {footer && <div className="mt-2">{footer}</div>}
        </div>
      </Link>

      {actions && <div className="absolute top-0 right-0 flex">{actions}</div>}
    </div>
  )
}

// Botao de canto sobre a capa. Some ate o mouse entrar no card, mas reaparece
// pra quem navega por teclado (focus-within no grupo) - senao a acao existiria
// so pra quem usa mouse.
export const cardActionButton =
  'grid place-items-center h-8 w-8 bg-canvas/90 text-ink border-l border-b border-line ' +
  'opacity-0 group-hover:opacity-100 group-focus-within:opacity-100 focus-visible:opacity-100 ' +
  'transition disabled:opacity-40 cursor-pointer'
