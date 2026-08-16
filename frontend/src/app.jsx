import { Routes, Route } from 'react-router-dom'
import Navbar from '@/layout/navbar.jsx'
import Footer from '@/layout/footer.jsx'
import LandingPage from '@/pages/landing-page.jsx'
import GamesPage from '@/pages/games-page.jsx'
import GameDetailPage from '@/pages/game-detail-page.jsx'
import ProfilePage from '@/pages/profile-page.jsx'
import RecommendationsPage from '@/pages/recommendations-page.jsx'
import GalleryPage from '@/pages/gallery-page.jsx'
import LoginPage from '@/pages/login-page.jsx'
import RegisterPage from '@/pages/register-page.jsx'

// O App so define o "mapa" do site. O layout e uma coluna que ocupa a tela
// inteira (min-h-screen): o <main> cresce pra preencher o espaco (flex-1), o
// que empurra o footer pro fim sempre - mesmo em paginas curtas, tipo o login,
// ele nao fica flutuando no meio.
export default function App() {
  return (
    <div className="min-h-screen flex flex-col">
      <Navbar />
      <main className="flex-1">
        <Routes>
          <Route path="/" element={<LandingPage />} />
          <Route path="/games" element={<GamesPage />} />
          <Route path="/games/:id" element={<GameDetailPage />} />
          {/* Vitrine visual do catalogo: so a arte, sem nota nem texto */}
          <Route path="/galeria" element={<GalleryPage />} />
          {/* Tela servida pelo microsservico de recomendacoes (via gateway) */}
          <Route path="/recommendations" element={<RecommendationsPage />} />
          <Route path="/users/:username" element={<ProfilePage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />
        </Routes>
      </main>
      <Footer />
    </div>
  )
}
