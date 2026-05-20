import axios from 'axios'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL
    ? `${import.meta.env.VITE_API_URL}/api`
    : '/api',
  headers: { 'Content-Type': 'application/json' },
})

const AUTH_ENDPOINTS = ['/auth/login', '/auth/password-recovery', '/auth/recover-by-name']

function isAuthEndpoint(url = '') {
  return AUTH_ENDPOINTS.some((endpoint) => url.endsWith(endpoint))
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token && !isAuthEndpoint(config.url)) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && localStorage.getItem('token') && !isAuthEndpoint(error.config?.url)) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      window.location.href = '/login'
    }
    return Promise.reject(error)
  }
)

export default api
