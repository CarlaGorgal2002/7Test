import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'

const ROLE_ROUTES = {
  ADMINISTRADOR: '/admin',
  PROFESOR: '/profesor',
  ALUMNO: '/alumno',
  DIRECTOR_DE_CATEDRA: '/director',
}

export default function LoginPage() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
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
    try {
      const res = await api.post('/auth/login', { email, password })
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify({ email, role: res.data.role, fullName: res.data.fullName }))
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
          background-color: #2a2a3e !important;
          -webkit-box-shadow: 0 0 0 30px #2a2a3e inset !important;
          color: #2a2a3e !important;
          -webkit-text-fill-color: #2a2a3e !important;
        }
      `}</style>
    )}
    <div style={{
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      minHeight: '100vh',
      background: dm
        ? 'linear-gradient(135deg, #0d0d1a 0%, #1a1a2e 100%)'
        : 'linear-gradient(135deg, #1a237e 0%, #283593 100%)',
    }}>
      <div style={{
        background: dm ? '#1e1e2e' : '#fff',
        borderRadius: 12,
        padding: '40px 48px',
        width: '100%',
        maxWidth: 420,
        boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
        position: 'relative',
      }}>

        {/* Toggle modo oscuro */}
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

        <h1 style={{ fontSize: 32, fontWeight: 700, color: dm ? '#a0aaff' : '#1a237e', textAlign: 'center', marginBottom: 4 }}>
          7test
        </h1>
        <p style={{ fontSize: 13, color: dm ? '#aaa' : '#666', textAlign: 'center', marginBottom: 32 }}>
          Plataforma de Evaluaciones — UADE
        </p>

        {!showRecovery ? (
          <>
            <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <label style={{ fontSize: 13, fontWeight: 600, color: dm ? '#ccc' : '#444' }}>
                Email institucional
              </label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="usuario@uade.edu.ar"
                required
                className={dm ? 'dm-input' : ''}
                style={{
                  padding: '10px 14px',
                  border: `1.5px solid ${dm ? '#444' : '#ddd'}`,
                  borderRadius: 8,
                  fontSize: 15,
                  outline: 'none',
                }}
              />

              <label style={{ fontSize: 13, fontWeight: 600, color: dm ? '#ccc' : '#444' }}>
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
                    border: `1.5px solid ${dm ? '#444' : '#ddd'}`,
                    borderRadius: 8,
                    fontSize: 15,
                    outline: 'none',
                    boxSizing: 'border-box',
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
                    color: dm ? '#aaa' : '#888',
                  }}
                >
                  {showPassword ? '🙈' : '👁'}
                </button>
              </div>

              {error && (
                <p style={{ color: '#c62828', fontSize: 13, background: '#ffebee', padding: '8px 12px', borderRadius: 6 }}>
                  {error}
                </p>
              )}

              <button type="submit" disabled={loading} style={{
                marginTop: 8,
                padding: '12px',
                background: '#1a237e',
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
              color: dm ? '#a0aaff' : '#1a237e',
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
              <label style={{ fontSize: 13, fontWeight: 600, color: dm ? '#ccc' : '#444' }}>
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
                  border: `1.5px solid ${dm ? '#444' : '#ddd'}`,
                  borderRadius: 8,
                  fontSize: 15,
                  outline: 'none',
                }}
              />
              {recoveryMsg && (
                <p style={{ color: '#2e7d32', fontSize: 13, background: '#e8f5e9', padding: '8px 12px', borderRadius: 6 }}>
                  {recoveryMsg}
                </p>
              )}
              <button type="submit" style={{
                marginTop: 8,
                padding: '12px',
                background: '#1a237e',
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
              color: dm ? '#a0aaff' : '#1a237e',
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
