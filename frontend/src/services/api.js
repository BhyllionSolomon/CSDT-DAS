import axios from 'axios'

const LOCAL_API = 'http://localhost:8081/api'
const onThisComputer = ['localhost', '127.0.0.1'].includes(window.location.hostname)

// On your computer: the local server. On the live site: the address set on Render's website service,
// under either name (VITE_API_URL or the older VITE_API_BASE_URL). The same code works in both places.
export const API_URL = String(
    import.meta.env.VITE_API_URL ||
    import.meta.env.VITE_API_BASE_URL ||
    (onThisComputer ? LOCAL_API : '')
).replace(/\/+$/, '')

const api = axios.create({
    baseURL: API_URL || '/api',
    headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
    const token = sessionStorage.getItem('csdtdas_token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

// Expired or missing login -> back to sign-in.
api.interceptors.response.use(
    (response) => response,
    (error) => {
        const url = error.config?.url || ''
        if (error.response?.status === 401 && !url.startsWith('/auth/')) {
            sessionStorage.removeItem('csdtdas_token')
            sessionStorage.removeItem('csdtdas_user')
            if (window.location.pathname !== '/login') window.location.assign('/login')
        }
        return Promise.reject(error)
    }
)

export default api