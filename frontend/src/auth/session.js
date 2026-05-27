const TOKEN_KEY = 'seventest.token'
const USER_KEY = 'seventest.user'
const LAST_ACTIVITY_KEY = 'seventest.lastActivity'
const SESSION_TIMEOUT_MS = 30 * 60 * 1000

export function setSession(token, user) {
  sessionStorage.setItem(TOKEN_KEY, token)
  sessionStorage.setItem(USER_KEY, JSON.stringify(user))
  touchSession()
}

export function getToken() {
  if (isSessionExpired()) {
    clearSession()
    return null
  }
  return sessionStorage.getItem(TOKEN_KEY)
}

export function getCurrentUser() {
  if (isSessionExpired()) {
    clearSession()
    return null
  }
  try {
    return JSON.parse(sessionStorage.getItem(USER_KEY) || 'null')
  } catch {
    clearSession()
    return null
  }
}

export function updateCurrentUser(user) {
  sessionStorage.setItem(USER_KEY, JSON.stringify(user))
  touchSession()
}

export function touchSession() {
  sessionStorage.setItem(LAST_ACTIVITY_KEY, String(Date.now()))
}

export function clearSession() {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(USER_KEY)
  sessionStorage.removeItem(LAST_ACTIVITY_KEY)
  localStorage.removeItem('token')
  localStorage.removeItem('user')
}

function isSessionExpired() {
  const token = sessionStorage.getItem(TOKEN_KEY)
  if (!token) return false
  const lastActivity = Number(sessionStorage.getItem(LAST_ACTIVITY_KEY) || 0)
  return lastActivity > 0 && Date.now() - lastActivity > SESSION_TIMEOUT_MS
}
