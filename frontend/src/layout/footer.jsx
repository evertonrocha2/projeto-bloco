import { Link } from 'react-router-dom'
import { Gamepad2, Code2 } from 'lucide-react'

// Rodape claro, em colunas. Sem texto de "jogos via RAWG".
export default function Footer() {
  return (
    <footer className="bg-mist border-t border-line mt-auto">
      <div className="max-w-6xl mx-auto px-6 py-14 grid gap-10 md:grid-cols-[1.5fr_1fr_1fr]">
        <div>
          <div className="flex items-center gap-2 font-display font-bold text-xl text-ink">
            <span className="grid place-items-center h-8 w-8 bg-accent text-canvas">
              <Gamepad2 size={18} />
            </span>
            GameLog
          </div>
          <p className="text-slate text-sm mt-3 max-w-xs">
            Sua estante de jogos: registre, avalie e organize tudo o que você jogou
            num só lugar.
          </p>
        </div>

        <div>
          <h4 className="font-display font-bold text-sm text-ink mb-3">Navegação</h4>
          <ul className="space-y-2 text-sm text-slate">
            <li><Link to="/games" className="hover:text-ink transition">Catálogo</Link></li>
            <li><Link to="/login" className="hover:text-ink transition">Entrar</Link></li>
            <li><Link to="/register" className="hover:text-ink transition">Criar conta</Link></li>
          </ul>
        </div>

        <div>
          <h4 className="font-display font-bold text-sm text-ink mb-3">Projeto</h4>
          <ul className="space-y-2 text-sm text-slate">
            <li>
              <a href="https://github.com/evertonrocha2/projeto-bloco-01" target="_blank" rel="noreferrer" className="inline-flex items-center gap-2 hover:text-ink transition">
                <Code2 size={16} /> Repositório
              </a>
            </li>
          </ul>
        </div>
      </div>

      <div className="border-t border-line">
        <div className="max-w-6xl mx-auto px-6 py-5 text-xs text-slate">
          © 2026 GameLog — Projeto de Bloco.
        </div>
      </div>
    </footer>
  )
}
