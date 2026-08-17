// Os status de um jogo na colecao, do jeito que o front precisa deles.
//
// Espelha o enum CollectionStatus do back-end. Cada status tem duas faces, e
// confundir as duas ja quebrou o app: o CODIGO (QUERO_JOGAR) e o que viaja na
// API, o ROTULO ("Quero jogar") e o que a pessoa le. Depois da migracao pro enum
// no back-end, duas telas continuaram mandando o rotulo no corpo da requisicao e
// a API passou a responder 400 - adicionar jogo na colecao simplesmente parou.
//
// A causa nao foi distracao: a lista existia DUAS vezes, escrita a mao como texto
// de tela nos dois arquivos, sem nada indicando que aquele texto tambem era
// protocolo. Aqui ela existe uma vez so, e a distincao esta no proprio formato do
// dado.
//
// A ordem e a da jornada de quem joga, nao a alfabetica: querer, jogar, terminar,
// completar, desistir. As abas do perfil e o select saem daqui na ordem em que
// estao escritos.
export const COLLECTION_STATUSES = [
  { code: 'QUERO_JOGAR', label: 'Quero jogar' },
  { code: 'JOGANDO', label: 'Jogando' },
  { code: 'ZERADO', label: 'Zerado' },
  { code: 'PLATINADO', label: 'Platinado' },
  { code: 'LARGADO', label: 'Largado' },
]

// Com o que os formularios de adicionar a colecao comecam.
export const DEFAULT_STATUS = COLLECTION_STATUSES[0].code

// O rotulo de um codigo.
//
// A API ja manda statusLabel junto de cada item da colecao, entao prefira o campo
// que veio dela. Isto serve pros casos em que so existe o codigo em maos - o
// select, as abas, o resultado otimista antes da resposta chegar.
//
// Codigo desconhecido volta como veio: um status novo no back-end deixa o card
// feio, mas nao em branco - e a feiura denuncia a defasagem.
export function statusLabel(code) {
  if (!code) {
    return ''
  }

  const found = COLLECTION_STATUSES.find((status) => status.code === code)
  return found ? found.label : code
}
