import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage.jsx'
import AdminLanding from './pages/AdminLanding.jsx'
import ProfesorLanding from './pages/ProfesorLanding.jsx'
import AlumnoLanding from './pages/AlumnoLanding.jsx'
import DirectorLanding from './pages/DirectorLanding.jsx'

function PrivateRoute({ children, allowedRole }) {
  const token = localStorage.getItem('token')
  const user = JSON.parse(localStorage.getItem('user') || 'null')

  if (!token || !user) return <Navigate to="/login" replace />
  if (allowedRole && user.role !== allowedRole) return <Navigate to="/login" replace />
  return children
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
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
