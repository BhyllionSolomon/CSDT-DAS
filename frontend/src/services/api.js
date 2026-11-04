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

// ---------------------------------------------------------------------------
// One place that keeps lists in a sensible order on every page.
// ---------------------------------------------------------------------------
const PROGRAMME_ORDER = ['CS', 'SEN', 'CYB', 'IFT']

function tidy(url, data) {
    if (!Array.isArray(data)) return data

    const path = String(url || '').split('?')[0].replace(/\/+$/, '')

    if (path.endsWith('/programmes')) {
        const rank = (p) => {
            const i = PROGRAMME_ORDER.indexOf(String(p.code || '').toUpperCase())
            return i === -1 ? 99 : i
        }
        return [...data].sort((a, b) => rank(a) - rank(b) || String(a.name).localeCompare(String(b.name)))
    }

    if (path.endsWith('/levels')) {
        return [...data].sort((a, b) => Number(a.levelNumber ?? a.code) - Number(b.levelNumber ?? b.code))
    }

    if (path.endsWith('/academic-sessions') || path.endsWith('/sessions')) {
        return [...data].sort((a, b) => String(b.startDate).localeCompare(String(a.startDate)))
    }

    return data
}

api.interceptors.request.use((config) => {
    const token = sessionStorage.getItem('csdtdas_token')
    if (token) config.headers.Authorization = `Bearer ${token}`
    return config
})

api.interceptors.response.use(
    (response) => {
        if ((response.config.method || 'get').toLowerCase() === 'get') {
            response.data = tidy(response.config.url, response.data)
        }
        return response
    },
    (error) => {
        // Expired or missing login -> back to sign-in.
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