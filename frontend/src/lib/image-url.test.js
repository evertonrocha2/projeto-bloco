import { describe, it, expect } from 'vitest'
import { resolveImageUrl } from './image-url.js'

// Uma imagem de perfil pode vir de tres lugares, e os tres viram uma string de
// URL guardada no banco. Duas delas sao caminhos relativos - e e aqui que mora a
// pegadinha, porque relativo a QUEM depende de quem serve o arquivo:
//
//   /background-2.jpg          -> arte do projeto, servida pelo front (5173)
//   /api/uploads/<uuid>.jpg    -> upload, servido pela API (8090)
//   https://cdn.../capa.jpg    -> endereco colado, servido por terceiro
//
// O bug que originou este modulo: o avatar enviado nao aparecia. O <img src> com
// "/api/uploads/..." era resolvido contra a origem do FRONT, e o servidor do Vite
// respondia 404. Build passava, teste passava, e a tela mostrava o fallback da
// inicial como se ninguem tivesse enviado nada.
describe('resolveImageUrl', () => {
  const base = 'http://localhost:8090'

  it('aponta o caminho da API pro servidor da API', () => {
    expect(resolveImageUrl('/api/uploads/abc.jpg', base))
      .toBe('http://localhost:8090/api/uploads/abc.jpg')
  })

  it('deixa a arte do projeto como caminho relativo', () => {
    // Essa e servida pelo proprio front. Prefixar com a API daria 404 - o erro
    // simetrico do que motivou o modulo.
    expect(resolveImageUrl('/background-2.jpg', base)).toBe('/background-2.jpg')
  })

  it('nao mexe em endereco absoluto', () => {
    expect(resolveImageUrl('https://cdn.exemplo.com/capa.jpg', base))
      .toBe('https://cdn.exemplo.com/capa.jpg')
    expect(resolveImageUrl('http://cdn.exemplo.com/capa.jpg', base))
      .toBe('http://cdn.exemplo.com/capa.jpg')
  })

  it('devolve vazio quando nao ha imagem', () => {
    // Quem chama usa o resultado num && pra decidir se desenha o <img>.
    expect(resolveImageUrl(null, base)).toBe('')
    expect(resolveImageUrl(undefined, base)).toBe('')
    expect(resolveImageUrl('', base)).toBe('')
  })

  it('nao duplica a barra quando a base termina com uma', () => {
    expect(resolveImageUrl('/api/uploads/abc.jpg', 'http://localhost:8090/'))
      .toBe('http://localhost:8090/api/uploads/abc.jpg')
  })

  it('vale pra qualquer caminho da API, nao so uploads', () => {
    // Se amanha a API servir outra coisa por caminho relativo, a regra ja cobre.
    expect(resolveImageUrl('/api/qualquer/coisa.png', base))
      .toBe('http://localhost:8090/api/qualquer/coisa.png')
  })
})
