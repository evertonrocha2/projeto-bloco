import { Routes, Route, useLocation } from 'react-router-dom'
import Navbar from '@/layout/navbar.jsx'
import Footer from '@/layout/footer.jsx'
import ScrollProgress from '@/ui/scroll-progress.jsx'
import LandingPage from '@/pages/landing-page.jsx'
import GamesPage from '@/pages/games-page.jsx'
import GameDetailPage from '@/pages/game-detail-page.jsx'
import ProfilePage from '@/pages/profile-page.jsx'
import RecommendationsPage from '@/pages/recommendations-page.jsx'
import LoginPage from '@/pages/login-page.jsx'
import RegisterPage from '@/pages/register-page.jsx'

// O App so define o "mapa" do site. O layout e uma coluna que ocupa a tela
// inteira (min-h-screen): o <main> cresce pra preencher o espaco (flex-1), o
// que empurra o footer pro fim sempre - mesmo em paginas curtas, tipo o login,
// ele nao fica flutuando no meio.
export default function App() {
  // As telas de acesso trazem a propria moldura (AuthLayout): arte em tela cheia,
  // cabecalho minimo e nada mais pra clicar. Menu e rodape sao distracao num
  // momento em que a pessoa veio fazer uma coisa so.
  const { pathname } = useLocation()
  const telaDeAcesso = pathname === '/login' || pathname === '/register'

  return (
    <div className="min-h-screen flex flex-col">
      {!telaDeAcesso && <ScrollProgress />}
      {!telaDeAcesso && <Navbar />}
      <main className="flex-1">
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/games" element={<GamesPage />} />
          <Route path="/games/:id" element={<GameDetailPage />} />
          {/* Tela servida pelo microsservico de recomendacoes (via gateway) */}
          <Route path="/recommendations" element={<RecommendationsPage />} />
          <Route path="/users/:username" element={<ProfilePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Routes>
      </main>
      {!telaDeAcesso && <Footer />}
    </div>
  )
}
