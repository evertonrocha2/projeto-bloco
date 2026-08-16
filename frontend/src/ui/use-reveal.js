import { useEffect } from 'react'

// Revela os elementos marcados com [data-reveal] quando eles entram na tela.
//
// Um IntersectionObserver so, registrado uma vez pela pagina, em vez de um
// observer (ou um listener de scroll) por elemento. Listener de scroll dispara
// dezenas de vezes por segundo e forca o navegador a recalcular layout; o
// observer avisa so quando o elemento cruza a borda da tela.
//
// unobserve depois de revelar: o elemento ja apareceu, nao ha o que observar
// depois disso - e a animacao nao se repete quando a pessoa rola de volta, o que
// seria irritante.
//
// Quem pediu menos movimento no sistema operacional nao recebe animacao nenhuma:
// os elementos ja nascem visiveis (ver o @media no index.css) e o observer so
// acrescenta a classe, sem nunca esconder nada.
export function useReveal(deps = []) {
  useEffect(() => {
    const alvos = document.querySelectorAll('[data-reveal]:not(.is-revealed)')
    if (alvos.length === 0) return

    const observer = new IntersectionObserver(
      (entradas) => {
        for (const entrada of entradas) {
          if (entrada.isIntersecting) {
            entrada.target.classList.add('is-revealed')
            observer.unobserve(entrada.target)
          }
        }
      },
      // Revela um pouco ANTES de encostar na borda, pra animacao terminar
      // enquanto o elemento ainda esta subindo - senao ela acontece fora de vista.
      { rootMargin: '0px 0px -12% 0px', threshold: 0.05 },
    )

    alvos.forEach((alvo) => observer.observe(alvo))
    return () => observer.disconnect()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps)
}
