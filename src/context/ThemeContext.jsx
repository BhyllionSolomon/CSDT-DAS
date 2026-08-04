import { createContext, useContext, useEffect, useState } from 'react'

const ThemeContext = createContext(null)

export const THEMES = [
  { id: 'light', label: 'Light', swatch: '#1C6DD0' },
  { id: 'blue', label: 'Blue Control Room', swatch: '#3A9EFF' },
  { id: 'green', label: 'Field Green', swatch: '#22C55E' },
  { id: 'dark', label: 'Dark Ops', swatch: '#6366F1' },
  { id: 'neon', label: 'Neon Alert', swatch: '#D946EF' },
]

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => localStorage.getItem('epi-theme') || 'blue')

  useEffect(() => {
    document.documentElement.setAttribute('data-theme', theme)
    localStorage.setItem('epi-theme', theme)
  }, [theme])

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider')
  return ctx
}
