import { BASE_URL } from './api/client.js'

// Onde uma imagem de personalizacao realmente mora.
//
// As tres origens possiveis viram a mesma coisa no banco - uma string de URL - mas
// duas delas sao caminhos relativos, e relativo a QUEM depende de quem serve o
// arquivo:
//
//   /background-2.jpg         arte do projeto, servida pelo FRONT (5173)
//   /api/uploads/<uuid>.jpg   upload, servido pela API (8090)
//   https://cdn.../capa.jpg   endereco colado, servido por terceiro
//
// Este modulo existe por causa de um bug concreto: o avatar enviado nao aparecia.
// O <img src="/api/uploads/..."> era resolvido contra a origem do front, e o
// servidor do Vite respondia 404 - a tela caia no fallback da inicial, como se
// ninguem tivesse enviado nada. Build e teste passavam; so a imagem da tela
// mostrava.
//
// Guardar a URL absoluta no banco resolveria e criaria coisa pior: o endereco do
// servidor viraria dado persistido, e mudar de porta ou de host exigiria migracao
// de dado. O caminho relativo continua no banco, e a resolucao acontece na hora de
// desenhar - que e o unico momento em que se sabe quem serve o que.

// Caminhos que pertencem a API. Tudo sob /api vem do back-end, hoje e depois.
const PREFIXO_API = '/api/'

export function resolveImageUrl(url, base = BASE_URL) {
  if (!url) {
    return ''
  }

  // Endereco absoluto: quem colou ja disse onde a imagem mora.
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }

  if (url.startsWith(PREFIXO_API)) {
    // replace tira barra final da base pra nao sair "8090//api/...". Alguns
    // navegadores toleram a barra dupla, outros nao, e depender disso e pedir um
    // bug que aparece so num deles.
    return `${base.replace(/\/$/, '')}${url}`
  }

  // Sobra a arte do projeto, servida pelo proprio front. Prefixar com a API aqui
  // daria 404 - o erro simetrico do que motivou este modulo.
  return url
}
