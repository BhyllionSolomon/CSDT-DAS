import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8081/api',
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