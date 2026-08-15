import { describe, it, expect } from 'vitest'
import { parseGenres, primaryGenre, collectGenres } from './genres.js'

// No catalogo, o genero de um jogo vem como UMA string com varios generos
// dentro: "Action, RPG". E o formato que a API da RAWG devolve e que o monolito
// guarda desde o TP1.
//
// Antes deste modulo, essa regra estava espalhada em quatro telas com tres
// implementacoes diferentes - e nenhuma tinha nome. Testar aqui e o que permite
// que as telas simplesmente chamem parseGenres() e parem de se preocupar com
// virgula, espaco sobrando ou campo vazio.
describe('parseGenres', () => {
  it('separa os generos de uma string unica', () => {
    expect(parseGenres('Action, RPG')).toEqual(['Action', 'RPG'])
  })

  it('devolve um unico genero quando nao ha virgula', () => {
    expect(parseGenres('Roguelike')).toEqual(['Roguelike'])
  })

  it('ignora espacos sobrando e virgula solta no fim', () => {
    // Dado real do catalogo costuma vir sujo assim. Sem esse tratamento
    // apareceria um chip de filtro vazio na tela.
    expect(parseGenres('  Action ,  RPG ,')).toEqual(['Action', 'RPG'])
  })

  it('trata campo ausente como lista vazia', () => {
    // Jogo sem genero e caso real: nem toda importacao traz o campo preenchido.
    expect(parseGenres(null)).toEqual([])
    expect(parseGenres(undefined)).toEqual([])
    expect(parseGenres('')).toEqual([])
    expect(parseGenres('   ')).toEqual([])
  })
})

describe('primaryGenre', () => {
  it('devolve o primeiro genero, usado no selo do card', () => {
    expect(primaryGenre({ genre: 'Action, RPG' })).toBe('Action')
  })

  it('devolve undefined quando o jogo nao tem genero', () => {
    // undefined pra que o React nao renderize nada, em vez de um selo vazio.
    expect(primaryGenre({ genre: null })).toBeUndefined()
    expect(primaryGenre({})).toBeUndefined()
  })
})

describe('collectGenres', () => {
  it('junta os generos de todos os jogos sem repetir', () => {
    const games = [
      { genre: 'Action, RPG' },
      { genre: 'RPG' },
      { genre: 'Indie' },
    ]

    expect(collectGenres(games)).toEqual(['Action', 'RPG', 'Indie'])
  })

  it('ignora jogos sem genero', () => {
    const games = [{ genre: 'Action' }, { genre: null }, { genre: '' }]

    expect(collectGenres(games)).toEqual(['Action'])
  })

  it('devolve lista vazia para catalogo vazio', () => {
    expect(collectGenres([])).toEqual([])
  })
})
