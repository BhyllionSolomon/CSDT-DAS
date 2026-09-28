import { createContext, useContext, useState } from 'react'
import { login as loginRequest, signup as signupRequest, getCurrentUser } from '../services/authService'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
    const [user, setUser] = useState(() => {
        const stored = sessionStorage.getItem('csdtdas_user')
        return stored ? JSON.parse(stored) : null
    })

    async function afterAuth(data) {
        sessionStorage.setItem('csdtdas_token', data.token)
        const profile = await getCurrentUser()
        sessionStorage.setItem('csdtdas_user', JSON.stringify(profile))
        setUser(profile)
    }

    async function login(username, password) {
        const data = await loginRequest(username, password)
        await afterAuth(data)
    }

    async function signup(idNumber, fullName, email, username, password) {
        const data = await signupRequest(idNumber, fullName, email, username, password)
        await afterAuth(data)
    }

    function logout() {
        sessionStorage.removeItem('csdtdas_token')
        sessionStorage.removeItem('csdtdas_user')
        setUser(null)
    }

    return (
        <AuthContext.Provider value={{ user, login, signup, logout }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    return useContext(AuthContext)
}