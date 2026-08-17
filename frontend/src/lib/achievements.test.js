import { describe, it, expect } from 'vitest'
import { describeAchievements } from './achievements.js'

// O back-end manda so o CODIGO da conquista ganha; o texto mora aqui.
//
// Foi de proposito: rotulo e descricao sao redacao, e redacao muda muito mais que
// regra. Do jeito que esta, reescrever "Primeira platina" nao passa por deploy do
// servidor - e o calculo de o que dizer fica testavel sem banco nenhum.
describe('describeAchievements', () => {
  it('descreve uma conquista ganha', () => {
    const [conquista] = describeAchievements(['FIRST_PLATINUM'])

    expect(conquista.code).toBe('FIRST_PLATINUM')
    expect(conquista.label).toBe('Primeira platina')
    expect(conquista.description).toBeTruthy()
  })

  it('mantem a ordem em que o servidor mandou', () => {
    // O servidor devolve da mais facil pra mais dificil. Reordenar aqui faria a
    // faixa de conquistas mudar de arranjo sem motivo visivel.
    const codigos = ['FIRST_PLATINUM', 'TEN_FINISHED', 'TWENTY_FIVE_REVIEWS']

    expect(describeAchievements(codigos).map((c) => c.code)).toEqual(codigos)
  })

  it('conhece as quatro conquistas atuais', () => {
    const codigos = ['FIRST_PLATINUM', 'TEN_FINISHED', 'TWENTY_FIVE_REVIEWS', 'LIST_OF_TWENTY']

    for (const conquista of describeAchievements(codigos)) {
      expect(conquista.description).toBeTruthy()
    }
  })

  it('nao esconde uma conquista que o front ainda nao conhece', () => {
    // Um codigo novo no servidor nao pode simplesmente sumir da tela: a pessoa
    // ganhou aquilo. Aparece com o codigo no lugar do nome, feio o bastante pra
    // denunciar a defasagem, em vez de silenciosamente nao existir.
    const [desconhecida] = describeAchievements(['LENDARIO'])

    expect(desconhecida.code).toBe('LENDARIO')
    expect(desconhecida.label).toBe('LENDARIO')
  })

  it('devolve lista vazia pra quem nao ganhou nada', () => {
    expect(describeAchievements([])).toEqual([])
    expect(describeAchievements(null)).toEqual([])
    expect(describeAchievements(undefined)).toEqual([])
  })
})
