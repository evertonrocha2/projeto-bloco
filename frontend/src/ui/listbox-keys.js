// A aritmetica de teclado de um listbox, sem React por perto.
//
// Toda a navegacao de um select acessivel se resume a "dado o que a pessoa
// apertou e onde o destaque esta, pra onde ele vai". Isso e conta de indice, nao
// interface - e conta de indice e onde os bugos moram: a seta que trava na ultima
// opcao, o Home que nao funciona com nada destacado, a letra que sempre acha a
// mesma opcao.
//
// Mora fora do componente porque o projeto nao tem React Testing Library: logica
// dentro de JSX aqui nao e verificavel por teste nenhum.

// Indice de "nada destacado". E o estado do painel recem-aberto.
const NENHUM = -1

// Pra onde o destaque vai. Devolve null quando a tecla nao navega - o componente
// usa isso pra deixar o evento seguir em vez de engolir Tab e prender o foco.
export function nextHighlight(key, current, count) {
  if (count <= 0) {
    return null
  }

  switch (key) {
    case 'ArrowDown':
      // Do "nada destacado" a seta pra baixo cai na primeira: (-1 + 1) % n = 0.
      // A volta ao passar do fim sai do mesmo resto, sem caso especial.
      return (current + 1) % count

    case 'ArrowUp':
      // De "nada destacado" a seta pra cima entra pela ULTIMA opcao. Precisa ser
      // caso a parte: o resto daria (-1 - 1 + n) % n, ou seja a penultima, e a
      // primeira coisa que a pessoa veria seria uma opcao pulada.
      if (current === NENHUM) {
        return count - 1
      }

      // Somar count antes do resto evita indice negativo quando current e 0.
      return (current - 1 + count) % count

    case 'Home':
      return 0

    case 'End':
      return count - 1

    default:
      return null
  }
}

// A opcao que comeca com a letra digitada, procurando a PARTIR da seguinte.
//
// Comecar na seguinte e o que faz apertar a mesma letra varias vezes alternar
// entre as opcoes que a compartilham, em vez de bater sempre na primeira. A busca
// da a volta pra que a alternancia seja um ciclo.
//
// Devolve -1 quando nenhuma opcao serve.
export function findByPrefix(labels, char, from) {
  const alvo = char.toLowerCase()
  const total = labels.length

  for (let passo = 1; passo <= total; passo += 1) {
    const indice = (from + passo) % total

    if (labels[indice].toLowerCase().startsWith(alvo)) {
      return indice
    }
  }

  return NENHUM
}
