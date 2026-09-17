import { createContext, useContext, useState } from 'react'
import { login as loginRequest } from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => {
        const stored = localStorage.getItem('csdtdas_user')
        return stored ? JSON.parse(stored) : null
    })

    async function login(username, password) {
        const data = await loginRequest(username, password)

        localStorage.setItem('csdtdas_token', data.token)
        localStorage.setItem(
            'csdtdas_user',
            JSON.stringify({
                username: data.username,
                fullName: data.fullName,
                role: data.role,
            })
        )

        setUser({
            username: data.username,
            fullName: data.fullName,
            role: data.role,
        })
    }

    function logout() {
        localStorage.removeItem('csdtdas_token')
        localStorage.removeItem('csdtdas_user')
        setUser(null)
    }

    return (
        <AuthContext.Provider value={{ user, login, logout }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    return useContext(AuthContext)
}