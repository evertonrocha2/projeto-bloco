import React from 'react'
import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './app.jsx'
import { AuthProvider } from '@/lib/auth.jsx'
import './index.css'

// Ponto de entrada do front. Aqui a gente "embrulha" o App em dois contextos:
// - BrowserRouter: liga o roteamento por URL (paginas sem recarregar o site).
// - AuthProvider: guarda o estado de login e deixa qualquer pagina acessar.
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <AuthProvider>
        <App />
      </AuthProvider>
    </BrowserRouter>
  </React.StrictMode>,
)
