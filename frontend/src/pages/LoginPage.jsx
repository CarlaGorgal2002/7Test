import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'
import Logo from '../components/Logo.jsx'

const ROLE_ROUTES = {
  ADMINISTRADOR: '/admin',
  PROFESOR: '/profesor',
  ALUMNO: '/alumno',
  DIRECTOR_DE_CATEDRA: '/director',
}

export default function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState(() => sessionStorage.getItem('lastLoginEmail') || '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [showRecovery, setShowRecovery] = useState(false)
  const [recoveryEmail, setRecoveryEmail] = useState('')
  const [recoveryMsg, setRecoveryMsg] = useState('')
  const [darkMode, setDarkMode] = useState(false)
  const [showPassword, setShowPassword] = useState(false)

  async function handleLogin(e) {
    e.preventDefault()
    setError('')
    setLoading(true)
    sessionStorage.setItem('lastLoginEmail', email)
    try {
      const res = await api.post('/auth/login', { email, password })
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify({ email, role: res.data.role, fullName: res.data.fullName }))
      sessionStorage.removeItem('lastLoginEmail')
      navigate(ROLE_ROUTES[res.data.role] || '/login')
    } catch (err) {
      const msg = err.response?.data?.message || 'Error al iniciar session'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  async function handleRecovery(e) {
    e.preventDefault()
    setRecoveryMsg('')
    try {
      const res = await api.post('/auth/recover-by-name', { name: recoveryEmail })
      setRecoveryMsg(`Se ha enviado un correo de recuperación a: ${res.data.email}`)
    } catch {
      setRecoveryMsg('No se encontró ningún usuario con ese nombre.')
    }
  }

  const dm = darkMode

  return (
    <>
    {dm && (
      <style>{`
        .dm-input {
          background-color: #122430 !important;
          -webkit-box-shadow: 0 0 0 30px #122430 inset !important;
          color: #122430 !important;
          -webkit-text-fill-color: #122430 !important;
        }
      `}</style>
    )}
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '100vh',
      background: dm
        ? 'linear-gradient(135deg, #050f14 0%, #09222A 100%)'
        : 'linear-gradient(135deg, #09222A 0%, #1956D8 100%)',
    }}>
      <div style={{
        background: dm ? '#0d1e28' : '#fff',
        borderRadius: 12,
        padding: '40px 48px',
        width: '100%',
        maxWidth: 420,
        boxShadow: '0 8px 32px rgba(9,34,42,0.28)',
        position: 'relative',
      }}>

        <button
          onClick={() => setDarkMode(!dm)}
          style={{
            position: 'absolute',
            top: 16,
            right: 16,
            background: 'none',
            border: 'none',
            fontSize: 20,
            cursor: 'pointer',
          }}
          title={dm ? 'Modo claro' : 'Modo oscuro'}
        >
          {dm ? '☀️' : '🌙'}
        </button>

        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: 4 }}>
          <Logo dark={dm} size={52} />
          <h1 style={{ fontSize: 32, fontWeight: 700, color: dm ? '#03BB83' : '#09222A', textAlign: 'center', marginTop: 8 }}>
            7test
          </h1>
        </div>
        <p style={{ fontSize: 13, color: dm ? '#CBEEF3' : '#1956D8', textAlign: 'center', marginBottom: 32 }}>
          Plataforma de Evaluaciones — UADE
        </p>

        {!showRecovery ? (
          <>
            <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <label style={{ fontSize: 13, fontWeight: 600, color: dm ? '#CBEEF3' : '#09222A' }}>
                Email institucional
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value)
                  sessionStorage.setItem('lastLoginEmail', e.target.value)
                }}
                placeholder="usuario@uade.edu.ar"
                required
                className={dm ? 'dm-input' : ''}
                style={{
                  padding: '10px 14px',
                  border: `1.5px solid ${dm ? '#1a3545' : '#CBEEF3'}`,
                  borderRadius: 8,
                  fontSize: 15,
                  outline: 'none',
                  color: '#09222A',
                }}
              />

              <label style={{ fontSize: 13, fontWeight: 600, color: dm ? '#CBEEF3' : '#09222A' }}>
                Contraceña
              </label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showPassword ? 'text' : 'password'}
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  required
                  className={dm ? 'dm-input' : ''}
                  style={{
                    width: '100%',
                    padding: '10px 40px 10px 14px',
                    border: `1.5px solid ${dm ? '#1a3545' : '#CBEEF3'}`,
                    borderRadius: 8,
                    fontSize: 15,
                    outline: 'none',
                    boxSizing: 'border-box',
                    color: '#09222A',
                  }}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    position: 'absolute',
                    right: 10,
                    top: '50%',
                    transform: 'translateY(-50%)',
                    background: 'none',
                    border: 'none',
                    cursor: 'pointer',
                    fontSize: 16,
                    color: dm ? '#CBEEF3' : '#09222A',
                  }}
                >
                  {showPassword ? '🙈' : '👁'}
                </button>
              </div>

              {error && (
                <p style={{ color: '#09222A', fontSize: 13, background: '#CBEEF3', padding: '8px 12px', borderRadius: 6, borderLeft: '3px solid #FFC012' }}>
                  {error}
                </p>
              )}

              <button type="submit" disabled={loading} style={{
                marginTop: 8,
                padding: '12px',
                background: '#1956D8',
                color: '#fff',
                border: 'none',
                borderRadius: 8,
                fontSize: 15,
                fontWeight: 600,
              }}>
                {loading ? 'Loading...' : 'Login'}
              </button>
            </form>

            <button onClick={() => setShowRecovery(true)} style={{
              marginTop: 16,
              display: 'block',
              textAlign: 'center',
              background: 'none',
              border: 'none',
              color: dm ? '#CBEEF3' : '#1956D8',
              fontSize: 13,
              textDecoration: 'underline',
              width: '100%',
              cursor: 'pointer',
            }}>
              ¿Olvidaste tu contraseña?
            </button>
          </>
        ) : (
          <>
            <form onSubmit={handleRecovery} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <label style={{ fontSize: 13, fontWeight: 600, color: dm ? '#CBEEF3' : '#09222A' }}>
                Ingresá tu nombre completo
              </label>
              <input
                type="text"
                value={recoveryEmail}
                onChange={(e) => setRecoveryEmail(e.target.value)}
                placeholder="Juan Pérez"
                required
                className={dm ? 'dm-input' : ''}
                style={{
                  padding: '10px 14px',
                  border: `1.5px solid ${dm ? '#1a3545' : '#CBEEF3'}`,
                  borderRadius: 8,
                  fontSize: 15,
                  outline: 'none',
                  color: '#09222A',
                }}
              />
              {recoveryMsg && (
                <p style={{ color: '#09222A', fontSize: 13, background: '#CBEEF3', padding: '8px 12px', borderRadius: 6, borderLeft: '3px solid #03BB83' }}>
                  {recoveryMsg}
                </p>
              )}
              <button type="submit" style={{
                marginTop: 8,
                padding: '12px',
                background: '#1956D8',
                color: '#fff',
                border: 'none',
                borderRadius: 8,
                fontSize: 15,
                fontWeight: 600,
              }}>
                Enviar solicitud
              </button>
            </form>

            <button onClick={() => setShowRecovery(false)} style={{
              marginTop: 16,
              display: 'block',
              textAlign: 'center',
              background: 'none',
              border: 'none',
              color: dm ? '#CBEEF3' : '#1956D8',
              fontSize: 13,
              textDecoration: 'underline',
              width: '100%',
              cursor: 'pointer',
            }}>
              ← Volver al login
            </button>
          </>
        )}
      </div>
    </div>
    </>
  )
}
