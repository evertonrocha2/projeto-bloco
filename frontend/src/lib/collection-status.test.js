import { describe, it, expect } from 'vitest'
import { COLLECTION_STATUSES, DEFAULT_STATUS, statusLabel } from './collection-status.js'

// Este modulo existe por causa de um bug real: o back-end migrou de String livre
// pro enum CollectionStatus, mas duas telas continuaram mandando o ROTULO
// ("Quero jogar") no lugar do codigo ("QUERO_JOGAR"). A API respondia 400 e
// adicionar jogo na colecao parou de funcionar.
//
// A causa foi a lista de status existir DUAS vezes no front, escrita a mao como
// texto de tela em ambas. Aqui ela existe uma vez, e a distincao entre o que vai
// pra API e o que aparece pro usuario e explicita.
describe('COLLECTION_STATUSES', () => {
  it('tem os cinco status do enum do back-end, na ordem da jornada', () => {
    // A ordem nao e alfabetica: e a sequencia natural de quem joga - quer jogar,
    // esta jogando, terminou, completou tudo, ou desistiu. As abas do perfil e o
    // select saem daqui, entao a ordem da lista E a ordem da tela.
    expect(COLLECTION_STATUSES.map((status) => status.code)).toEqual([
      'QUERO_JOGAR',
      'JOGANDO',
      'ZERADO',
      'PLATINADO',
      'LARGADO',
    ])
  })

  it('separa o codigo que vai pra API do rotulo que vai pra tela', () => {
    // O erro que originou o modulo foi exatamente confundir os dois.
    expect(COLLECTION_STATUSES[0]).toEqual({ code: 'QUERO_JOGAR', label: 'Quero jogar' })
  })

  it('nenhum codigo tem acento ou espaco', () => {
    // Codigo e nome de constante Java. Se um espaco entrar aqui, volta o 400.
    for (const { code } of COLLECTION_STATUSES) {
      expect(code).toMatch(/^[A-Z_]+$/)
    }
  })
})

describe('DEFAULT_STATUS', () => {
  it('comeca em "quero jogar", o status de quem ainda nao jogou', () => {
    // E o valor inicial dos dois formularios de adicionar a colecao.
    expect(DEFAULT_STATUS).toBe('QUERO_JOGAR')
  })
})

describe('statusLabel', () => {
  it('traduz o codigo da API pro texto da tela', () => {
    expect(statusLabel('PLATINADO')).toBe('Platinado')
  })

  it('devolve o proprio codigo quando nao conhece o valor', () => {
    // Um status novo no back-end nao deve deixar o card em branco. Mostrar o
    // codigo cru e feio, mas e informacao - e denuncia a defasagem.
    expect(statusLabel('LENDARIO')).toBe('LENDARIO')
  })

  it('devolve string vazia quando nao ha status', () => {
    expect(statusLabel(null)).toBe('')
    expect(statusLabel(undefined)).toBe('')
  })
})
