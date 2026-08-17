import { describe, it, expect } from 'vitest'
import { nextHighlight, findByPrefix } from './listbox-keys.js'

// A navegacao por teclado de um listbox e aritmetica de indice, e e onde moram os
// bugs: a seta que para na ultima opcao em vez de dar a volta, o Home que nao
// funciona com nada destacado, a tecla de letra que sempre acha a mesma opcao.
//
// Separar essa conta do componente e o que permite testa-la - o projeto nao tem
// React Testing Library, entao logica presa dentro de JSX nao e verificavel.
describe('nextHighlight', () => {
  it('desce uma opcao com a seta pra baixo', () => {
    expect(nextHighlight('ArrowDown', 0, 5)).toBe(1)
  })

  it('sobe uma opcao com a seta pra cima', () => {
    expect(nextHighlight('ArrowUp', 3, 5)).toBe(2)
  })

  it('da a volta ao passar da ultima opcao', () => {
    // Sem isso a seta "trava" no fim e a pessoa acha que o campo bugou.
    expect(nextHighlight('ArrowDown', 4, 5)).toBe(0)
  })

  it('da a volta ao passar da primeira opcao', () => {
    expect(nextHighlight('ArrowUp', 0, 5)).toBe(4)
  })

  it('comeca na primeira opcao quando nada esta destacado', () => {
    // -1 e o estado de painel recem-aberto sem selecao.
    expect(nextHighlight('ArrowDown', -1, 5)).toBe(0)
  })

  it('comeca na ultima opcao ao subir sem nada destacado', () => {
    expect(nextHighlight('ArrowUp', -1, 5)).toBe(4)
  })

  it('vai pras pontas com Home e End', () => {
    expect(nextHighlight('Home', 3, 5)).toBe(0)
    expect(nextHighlight('End', 1, 5)).toBe(4)
  })

  it('devolve null pra tecla que nao navega', () => {
    // null significa "nao e comigo" - o componente deixa o evento seguir, em vez
    // de engolir Tab e prender o foco no campo.
    expect(nextHighlight('Tab', 2, 5)).toBeNull()
    expect(nextHighlight('a', 2, 5)).toBeNull()
  })

  it('devolve null quando nao ha opcao nenhuma', () => {
    // Divisao por zero viraria NaN, e NaN como indice quebra o aria-activedescendant.
    expect(nextHighlight('ArrowDown', -1, 0)).toBeNull()
  })
})

describe('findByPrefix', () => {
  const labels = ['Quero jogar', 'Jogando', 'Zerado', 'Platinado', 'Largado']

  it('acha a primeira opcao que comeca com a letra', () => {
    expect(findByPrefix(labels, 'z', 0)).toBe(2)
  })

  it('ignora maiuscula e minuscula', () => {
    expect(findByPrefix(labels, 'P', 0)).toBe(3)
  })

  it('procura a partir da opcao seguinte, pra alternar entre as repetidas', () => {
    // "Jogando" e "Largado" nao colidem, mas "Quero jogar" e "Zerado" nao comecam
    // com L. Aqui o caso real: apertar L duas vezes com duas opcoes em L.
    const comRepetidas = ['Largado', 'Lendario', 'Zerado']
    expect(findByPrefix(comRepetidas, 'l', 0)).toBe(1)
  })

  it('da a volta ao chegar no fim', () => {
    const comRepetidas = ['Largado', 'Lendario', 'Zerado']
    expect(findByPrefix(comRepetidas, 'l', 1)).toBe(0)
  })

  it('devolve -1 quando nenhuma opcao comeca com a letra', () => {
    expect(findByPrefix(labels, 'x', 0)).toBe(-1)
  })
})
