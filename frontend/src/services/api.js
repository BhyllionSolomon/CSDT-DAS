
import axios from 'axios'

const LOCAL_API = 'http://localhost:8081/api'
const PRODUCTION_API = 'https://csdt-das-csdt-das-backend.onrender.com/api'

const onThisComputer = ['localhost', '127.0.0.1'].includes(
    window.location.hostname
)

// Local development uses localhost.
// The deployed site uses the Render backend by default.
// Environment variables can override the API endpoint.
export const API_URL = String(
    import.meta.env.VITE_API_URL ||
    import.meta.env.VITE_API_BASE_URL ||
    (onThisComputer ? LOCAL_API : PRODUCTION_API)
).replace(/\/+$/, '')

const api = axios.create({
    baseURL: API_URL,
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

            if (window.location.pathname !== '/login') {
                window.location.assign('/login')
            }
        }

        return Promise.reject(error)
    }
)

export default api
