// O texto das conquistas.
//
// O servidor manda so o codigo do que foi ganho; o nome e a descricao moram aqui.
// Foi decisao de projeto: rotulo e descricao sao redacao, e redacao muda muito
// mais que regra. Assim, reescrever "Primeira platina" nao passa por deploy do
// back-end, e o que a tela vai dizer fica testavel sem banco nenhum.

const CATALOGO = {
  FIRST_PLATINUM: {
    label: 'Primeira platina',
    description: 'Você completou tudo o que um jogo tinha a oferecer.',
  },
  TEN_FINISHED: {
    label: 'Dez zerados',
    description: 'Dez jogos levados até o fim. Não é pouca coisa.',
  },
  TWENTY_FIVE_REVIEWS: {
    label: 'Crítico',
    description: 'Vinte e cinco avaliações escritas.',
  },
  LIST_OF_TWENTY: {
    label: 'Curador',
    description: 'Uma lista sua reuniu vinte jogos.',
  },
}

// Transforma os codigos ganhos em algo que a tela sabe desenhar.
//
// Preserva a ordem que veio do servidor - ele devolve da mais facil pra mais
// dificil, e reordenar aqui faria a faixa mudar de arranjo sem motivo visivel.
//
// Codigo desconhecido NAO some. A pessoa ganhou aquilo; esconder por o front
// estar desatualizado seria tirar dela algo que o servidor ja concedeu. Aparece
// com o codigo no lugar do nome - feio o suficiente pra denunciar a defasagem.
export function describeAchievements(codes) {
  if (!codes) {
    return []
  }

  return codes.map((code) => {
    const conhecida = CATALOGO[code]

    return {
      code,
      label: conhecida ? conhecida.label : code,
      description: conhecida ? conhecida.description : null,
    }
  })
}
