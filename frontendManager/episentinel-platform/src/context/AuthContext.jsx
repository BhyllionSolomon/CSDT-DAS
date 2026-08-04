import { createContext, useContext, useState } from 'react'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const raw = localStorage.getItem('epi-user')
    return raw ? JSON.parse(raw) : null
  })

  const login = (username) => {
    const profile = {
      name: username || 'Dr. A. Balogun',
      role: 'Senior Surveillance Officer',
      org: 'Nigeria Centre for Disease Control',
      initials: (username || 'AB').slice(0, 2).toUpperCase(),
    }
    localStorage.setItem('epi-user', JSON.stringify(profile))
    setUser(profile)
  }

  const logout = () => {
    localStorage.removeItem('epi-user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
