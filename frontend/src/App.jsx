import { Routes, Route } from 'react-router-dom'
import Navbar from './components/Navbar.jsx'
import Footer from './components/Footer.jsx'
import LandingPage from './pages/LandingPage.jsx'
import GamesPage from './pages/GamesPage.jsx'
import GameDetailPage from './pages/GameDetailPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import RecommendationsPage from './pages/RecommendationsPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import RegisterPage from './pages/RegisterPage.jsx'

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
