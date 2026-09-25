import { createContext, useContext, useEffect, useState } from 'react'

const THEMES = ['light', 'dark', 'navy', 'purple', 'green']

const ThemeContext = createContext(null)

export function ThemeProvider({ children }) {
    const [theme, setTheme] = useState(() => localStorage.getItem('csdtdas_theme') || 'navy')

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', theme)
        localStorage.setItem('csdtdas_theme', theme)
    }, [theme])

    return (
        <ThemeContext.Provider value={{ theme, setTheme, THEMES }}>
            {children}
        </ThemeContext.Provider>
    )
}

export function useTheme() {
    return useContext(ThemeContext)
}