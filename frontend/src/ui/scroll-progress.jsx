import { useEffect, useRef } from 'react'

// Fio âmbar no topo mostrando o quanto da página já foi percorrido.
//
// Escrito sem estado do React de propósito: guardar a posição num useState
// dispararia uma re-renderização a cada pixel rolado. Aqui o handler escreve
// direto no style do elemento, então nada re-renderiza.
//
// A escrita acontece dentro de requestAnimationFrame, sincronizada com o quadro
// do navegador - o evento de scroll dispara muito mais vezes do que a tela
// atualiza, e sem isso boa parte do trabalho seria descartada.
export default function ScrollProgress() {
  const barra = useRef(null)

  useEffect(() => {
    let agendado = false

    function atualizar() {
      const rolavel = document.documentElement.scrollHeight - window.innerHeight
      // Página que cabe na tela não tem progresso a mostrar.
      const progresso = rolavel > 0 ? window.scrollY / rolavel : 0
      if (barra.current) {
        barra.current.style.transform = `scaleX(${progresso})`
      }
      agendado = false
    }

    function aoRolar() {
      if (agendado) return
      agendado = true
      requestAnimationFrame(atualizar)
    }

    atualizar()
    window.addEventListener('scroll', aoRolar, { passive: true })
    window.addEventListener('resize', aoRolar)
    return () => {
      window.removeEventListener('scroll', aoRolar)
      window.removeEventListener('resize', aoRolar)
    }
  }, [])

  // aria-hidden: é indicação puramente visual, e o leitor de tela já informa a
  // posição na página por conta própria.
  return <div ref={barra} className="scroll-progress" style={{ transform: 'scaleX(0)' }} aria-hidden="true" />
}
