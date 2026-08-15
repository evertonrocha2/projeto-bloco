// Traduz o dado que o microsservico devolve na frase que o usuario le.
//
// O back-end manda estrutura (uma lista de generos, um booleano), nao texto
// pronto. A frase e montada aqui de proposito: acentuacao, idioma e tom sao
// assunto da camada de apresentacao. No resto do projeto o back-end escreve
// strings sem acento, e "porque voce gosta de RPG" na tela ficaria estranho.
//
// Como sao funcoes puras, ficam num modulo separado dos componentes e sao
// testadas sem precisar renderizar nada.

// A justificativa da recomendacao, a partir dos generos que o algoritmo citou.
export function reasonText(reasonGenres) {
  // Lista vazia (ou ausente) significa que nao havia afinidade a citar: o jogo
  // entrou pela nota da comunidade. E o caso de quem acabou de se cadastrar.
  if (!reasonGenres || reasonGenres.length === 0) {
    return 'um dos jogos mais bem avaliados da comunidade'
  }

  // Um ou dois generos - o algoritmo nunca devolve mais que dois. "Indie e
  // Action" le melhor que "Indie, Action".
  const generos = reasonGenres.join(' e ')
  return `porque você gosta de ${generos}`
}

// O estado da conversa entre o microsservico e o monolito, pra tela poder mostrar.
//
// Expor isso e uma escolha de honestidade: quando o monolito esta fora do ar, as
// recomendacoes vem do banco proprio do microsservico e podem estar
// desatualizadas. Avisar e melhor do que deixar a pessoa clicando em "recalcular"
// sem entender por que a lista nao muda.
export function serviceStatus(stale) {
  if (stale) {
    return {
      live: false,
      label: 'modo degradado',
      detail:
        'O serviço de catálogo não respondeu. Estas recomendações vêm do último ' +
        'cálculo salvo e podem estar desatualizadas.',
    }
  }

  return {
    live: true,
    label: 'ao vivo',
    detail: 'Recomendações calculadas agora, com os dados atuais do catálogo.',
  }
}
