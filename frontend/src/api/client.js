import axios from 'axios'
import { clearSession, getToken, touchSession } from '../auth/session.js'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL
    ? `${import.meta.env.VITE_API_URL}/api`
    : '/api',
  headers: { 'Content-Type': 'application/json' },
})

const AUTH_ENDPOINTS = ['/auth/login', '/auth/password-recovery']

function isAuthEndpoint(url = '') {
  return AUTH_ENDPOINTS.some((endpoint) => url.endsWith(endpoint))
}

api.interceptors.request.use((config) => {
  const token = getToken()
  if (token && !isAuthEndpoint(config.url)) {
    config.headers.Authorization = `Bearer ${token}`
    touchSession()
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && getToken() && !isAuthEndpoint(error.config?.url)) {
      clearSession()
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
