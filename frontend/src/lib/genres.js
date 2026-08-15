// Como o catalogo guarda genero, e o que fazer com isso.
//
// Um jogo nao tem "um genero": tem uma STRING com varios dentro - "Action, RPG".
// E o formato que vem da API da RAWG e que o monolito guarda desde o TP1.
//
// Este modulo existe porque essa regra estava repetida em quatro telas, com tres
// implementacoes diferentes (uma com forEach e Set, outra com map e filter, outra
// pegando so o indice 0) e nenhum nome. Quem lia cada tela precisava reconstruir
// a regra de novo. Agora ela mora num lugar, tem nome e tem teste.

// Separa a string de generos numa lista, descartando espacos e entradas vazias.
//
// Tolera campo nulo, string vazia e virgula solta no fim porque dado de catalogo
// vem sujo assim - e um chip de filtro em branco na tela e feio e confunde.
export function parseGenres(genreField) {
  if (!genreField) {
    return []
  }

  return genreField
    .split(',')
    .map((genre) => genre.trim())
    .filter((genre) => genre.length > 0)
}

// O genero principal de um jogo - o primeiro da lista. E o que aparece no selo
// sobre a capa, onde so cabe um.
//
// Devolve undefined quando o jogo nao tem genero, e nao string vazia: assim o
// React nao renderiza nada, em vez de desenhar um selo vazio.
export function primaryGenre(game) {
  const [first] = parseGenres(game?.genre)
  return first
}

// Todos os generos distintos que aparecem num conjunto de jogos, na ordem em que
// sao encontrados. Usado pra montar os filtros do catalogo e as categorias da
// landing.
//
// Ordem de encontro, e nao alfabetica, porque as duas telas que usam isso hoje
// dependem de coisas diferentes: o catalogo ordena depois (deixando o .sort()
// visivel na tela, que e quem quer isso), e a landing corta os primeiros dez.
export function collectGenres(games) {
  const found = new Set()

  for (const game of games) {
    for (const genre of parseGenres(game.genre)) {
      found.add(genre)
    }
  }

  return Array.from(found)
}
