import { createContext, useContext, useEffect, useState } from 'react'
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
        await afterAuth(await loginRequest(username, password))
    }

    // Staff accounts wait for the H.O.D, so sign-up may not log the person in.
    async function signup(idNumber, fullName, email, username, password) {
        const result = await signupRequest(idNumber, fullName, email, username, password)
        if (!result.pendingApproval) await afterAuth(result.login)
        return result
    }

    // Reloads the signed-in person's details. A new token is passed when the username changed.
    async function refreshUser(newToken) {
        if (newToken) sessionStorage.setItem('csdtdas_token', newToken)
        const profile = await getCurrentUser()
        sessionStorage.setItem('csdtdas_user', JSON.stringify(profile))
        setUser(profile)
    }

    function logout() {
        sessionStorage.removeItem('csdtdas_token')
        sessionStorage.removeItem('csdtdas_user')
        setUser(null)
    }

    // Keeps the saved profile fresh, so a change made elsewhere (a linked student record,
    // a new role) shows up when the page opens or the browser tab regains focus.
    useEffect(() => {
        if (!user || !sessionStorage.getItem('csdtdas_token')) return undefined

        let cancelled = false

        function refresh() {
            getCurrentUser()
                .then((profile) => {
                    if (cancelled) return
                    sessionStorage.setItem('csdtdas_user', JSON.stringify(profile))
                    setUser(profile)
                })
                .catch(() => {})
        }

        refresh()
        window.addEventListener('focus', refresh)

        return () => {
            cancelled = true
            window.removeEventListener('focus', refresh)
        }
    }, [user?.username])

    return (
        <AuthContext.Provider value={{ user, login, signup, logout, refreshUser }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth() {
    return useContext(AuthContext)
}