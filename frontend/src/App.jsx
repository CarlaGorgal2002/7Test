import { useEffect, useState } from 'react'
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage.jsx'
import AdminLanding from './pages/AdminLanding.jsx'
import ProfesorLanding from './pages/ProfesorLanding.jsx'
import AlumnoLanding from './pages/AlumnoLanding.jsx'
import DirectorLanding from './pages/DirectorLanding.jsx'
import api from './api/client.js'
import { clearSession, getCurrentUser, getToken, updateCurrentUser } from './auth/session.js'

function PrivateRoute({ children, allowedRole }) {
  const [state, setState] = useState({ loading: true, allowed: false })

  useEffect(() => {
    let cancelled = false
    async function validateSession() {
      const token = getToken()
      if (!token) {
        clearSession()
        if (!cancelled) setState({ loading: false, allowed: false })
        return
      }
      try {
        const res = await api.get('/auth/me')
        updateCurrentUser(res.data)
        const roleAllowed = !allowedRole || res.data.role === allowedRole
        if (!cancelled) setState({ loading: false, allowed: roleAllowed })
      } catch {
        clearSession()
        if (!cancelled) setState({ loading: false, allowed: false })
      }
    }
    validateSession()
    return () => { cancelled = true }
  }, [allowedRole])

  const user = getCurrentUser()
  if (state.loading) return <div style={{ padding: 24 }}>Validando sesión...</div>
  if (!state.allowed || !user) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/recuperar-contrasena" element={<LoginPage recovery />} />
        <Route path="/admin" element={
          <PrivateRoute allowedRole="ADMINISTRADOR"><AdminLanding /></PrivateRoute>
        } />
        <Route path="/profesor" element={
          <PrivateRoute allowedRole="PROFESOR"><ProfesorLanding /></PrivateRoute>
        } />
        <Route path="/alumno" element={
          <PrivateRoute allowedRole="ALUMNO"><AlumnoLanding /></PrivateRoute>
        } />
        <Route path="/director" element={
          <PrivateRoute allowedRole="DIRECTOR_DE_CATEDRA"><DirectorLanding /></PrivateRoute>
        } />
        <Route path="*" element={<Navigate to="/login" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
