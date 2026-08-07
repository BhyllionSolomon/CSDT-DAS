import axios from 'axios'

// Base URL comes from .env (VITE_API_BASE_URL). Falls back to the URL you gave us
// so the app still works if the env var isn't set.
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api'

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' },
})

// Central place to see failed calls while wiring things up.
// Swap this for your real auth/error handling (toast, redirect to /login on 401, etc).
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const method = error?.config?.method?.toUpperCase()
    const url = error?.config?.url
    const status = error?.response?.status
    console.error(`[API] ${method} ${url} failed (${status ?? 'network error'})`, error?.response?.data || error.message)
    return Promise.reject(error)
  }
)

export default api
