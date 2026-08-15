import { createContext, useContext, useState } from 'react'

// Context de autenticacao: um "estado global" leve pro app inteiro saber se
// tem alguem logado e quem e, sem ficar passando isso de pai pra filho na mao.
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  // Inicializa lendo o localStorage, pra continuar logado depois de um refresh.
  const [token, setToken] = useState(() => localStorage.getItem('gamelog_token'))
  const [username, setUsername] = useState(() => localStorage.getItem('gamelog_username'))

  // Chamado depois de um login/cadastro bem sucedido.
  function login(newToken, newUsername) {
    localStorage.setItem('gamelog_token', newToken)
    localStorage.setItem('gamelog_username', newUsername)
    setToken(newToken)
    setUsername(newUsername)
  }

  function logout() {
    localStorage.removeItem('gamelog_token')
    localStorage.removeItem('gamelog_username')
    setToken(null)
    setUsername(null)
  }

  const value = { token, username, login, logout, isAuthenticated: Boolean(token) }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// Hook de atalho pra qualquer componente pegar o estado de login.
export function useAuth() {
  return useContext(AuthContext)
}
