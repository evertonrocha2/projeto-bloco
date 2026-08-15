import { describe, it, expect } from 'vitest'
import { reasonText, serviceStatus } from './recommendation-text.js'

// A API do microsservico devolve DADO ESTRUTURADO (uma lista de generos, um
// booleano stale); a frase que o usuario le e montada aqui. Isso mantem
// acentuacao e idioma na camada que cuida de apresentacao, e nao no back-end -
// que, no resto do projeto, escreve strings sem acento.
//
// Vale testar porque sao as regras que mudam o que aparece na tela, e sao faceis
// de errar em silencio: uma lista vazia virando "porque voce gosta de " ou uma
// virgula sobrando ficariam feias sem quebrar nada.
describe('reasonText', () => {
  it('usa o unico genero quando ha so um', () => {
    expect(reasonText(['RPG'])).toBe('porque você gosta de RPG')
  })

  it('junta dois generos com "e", sem virgula', () => {
    expect(reasonText(['Indie', 'Action'])).toBe('porque você gosta de Indie e Action')
  })

  it('explica pela comunidade quando nao ha genero a citar', () => {
    // Caso do usuario novo: sem perfil de gosto, o microsservico devolve
    // reasonGenres vazio. Sem este ramo a tela mostraria uma frase truncada.
    expect(reasonText([])).toBe('um dos jogos mais bem avaliados da comunidade')
  })

  it('tolera a lista ausente', () => {
    // Defesa contra resposta inesperada: campo faltando nao pode derrubar a tela.
    expect(reasonText(undefined)).toBe('um dos jogos mais bem avaliados da comunidade')
  })
})

describe('serviceStatus', () => {
  it('indica que esta ao vivo quando os dados sao recentes', () => {
    const status = serviceStatus(false)

    expect(status.live).toBe(true)
    expect(status.label).toBe('ao vivo')
  })

  it('indica modo degradado quando a resposta veio marcada como desatualizada', () => {
    // stale=true significa que o microsservico nao conseguiu falar com o
    // monolito e serviu o que tinha guardado. Mostrar isso e mais honesto do que
    // fingir normalidade - senao o usuario clicaria em "recalcular" varias vezes
    // sem entender por que a lista nao muda.
    const status = serviceStatus(true)

    expect(status.live).toBe(false)
    expect(status.label).toBe('modo degradado')
    expect(status.detail).toMatch(/catálogo/i)
  })
})
