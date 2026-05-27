import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'
import { setSession } from '../auth/session.js'
import Logo from '../components/Logo.jsx'

const ROLE_ROUTES = {
  ADMINISTRADOR: '/admin',
  PROFESOR: '/profesor',
  ALUMNO: '/alumno',
  DIRECTOR_DE_CATEDRA: '/director',
}

export default function LoginPage({ recovery = false }) {
  const navigate = useNavigate()
  const [email, setEmail] = useState(() => sessionStorage.getItem('lastLoginEmail') || '')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [recoveryEmail, setRecoveryEmail] = useState('')
  const [recoveryMsg, setRecoveryMsg] = useState('')
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('seventest.theme') === 'dark')
  const [showPassword, setShowPassword] = useState(false)

  useEffect(() => {
    localStorage.setItem('seventest.theme', darkMode ? 'dark' : 'light')
  }, [darkMode])

  async function handleLogin(e) {
    e.preventDefault()
    const cleanEmail = email.trim().toLowerCase()
    const cleanPassword = password.trim()
    if (!cleanEmail || !cleanPassword) return

    setError('')
    setLoading(true)
    sessionStorage.setItem('lastLoginEmail', cleanEmail)
    try {
      const res = await api.post('/auth/login', { email: cleanEmail, password: cleanPassword })
      setSession(res.data.token, { role: res.data.role, fullName: res.data.fullName })
      sessionStorage.removeItem('lastLoginEmail')
      navigate(ROLE_ROUTES[res.data.role] || '/login', { replace: true })
    } catch (err) {
      const msg = err.response?.data?.message || 'No se pudo iniciar sesión.'
      setError(msg)
    } finally {
      setLoading(false)
    }
  }

  async function handleRecovery(e) {
    e.preventDefault()
    const cleanEmail = recoveryEmail.trim().toLowerCase()
    if (!cleanEmail) return

    setRecoveryMsg('')
    try {
      await api.post('/auth/password-recovery', { email: cleanEmail })
      setRecoveryMsg('Si el email existe, registramos la solicitud de recuperación.')
    } catch {
      setRecoveryMsg('Revisa que el email tenga un formato valido.')
    }
  }

  const dm = darkMode
  const canSubmit = email.trim() && password.trim() && !loading

  return (
    <>
      {dm && (
        <style>{`
          .dm-input {
            background-color: #122430 !important;
            -webkit-box-shadow: 0 0 0 30px #122430 inset !important;
            color: #F4F8FA !important;
            -webkit-text-fill-color: #F4F8FA !important;
          }
          .dm-input::placeholder {
            color: #9BB6C1 !important;
            opacity: 1;
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
            type="button"
            onClick={() => setDarkMode(!dm)}
            style={styles.themeButton}
            title={dm ? 'Modo claro' : 'Modo oscuro'}
            aria-label={dm ? 'Activar modo claro' : 'Activar modo oscuro'}
          >
            {dm ? 'Claro' : 'Oscuro'}
          </button>

          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: 4 }}>
            <Logo dark={dm} size={52} />
            <h1 style={{ fontSize: 32, fontWeight: 700, color: dm ? '#03BB83' : '#09222A', textAlign: 'center', marginTop: 8 }}>
              7test
            </h1>
          </div>
          <p style={{ fontSize: 13, color: dm ? '#CBEEF3' : '#1956D8', textAlign: 'center', marginBottom: 32 }}>
            Plataforma de evaluaciones - UADE
          </p>

          {!recovery ? (
            <>
              <form onSubmit={handleLogin} style={styles.form}>
                <label htmlFor="email" style={labelStyle(dm)}>Email institucional</label>
                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => {
                    setEmail(e.target.value)
                    sessionStorage.setItem('lastLoginEmail', e.target.value.trim().toLowerCase())
                  }}
                  placeholder="usuario@uade.edu.ar"
                  required
                  className={dm ? 'dm-input' : ''}
                  style={inputStyle(dm)}
                />

                <label htmlFor="password" style={labelStyle(dm)}>Contraseña</label>
                <div style={{ position: 'relative' }}>
                  <input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Ingresa tu contraseña"
                    required
                    className={dm ? 'dm-input' : ''}
                    style={{ ...inputStyle(dm), width: '100%', paddingRight: 86, boxSizing: 'border-box' }}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    style={{ ...styles.inlineButton, color: dm ? '#CBEEF3' : '#09222A' }}
                  >
                    {showPassword ? 'Ocultar' : 'Ver'}
                  </button>
                </div>

                {error && <p style={messageStyle('#FFC012')}>{error}</p>}

                <button type="submit" disabled={!canSubmit} style={canSubmit ? styles.primaryButton : styles.disabledButton}>
                  {loading ? 'Ingresando...' : 'Iniciar sesión'}
                </button>
              </form>

              <button onClick={() => navigate('/recuperar-contrasena')} style={linkStyle(dm)}>
                Olvidé mi contraseña
              </button>
            </>
          ) : (
            <>
              <form onSubmit={handleRecovery} style={styles.form}>
                <label htmlFor="recovery-email" style={labelStyle(dm)}>Email institucional</label>
                <input
                  id="recovery-email"
                  type="email"
                  value={recoveryEmail}
                  onChange={(e) => setRecoveryEmail(e.target.value)}
                  placeholder="usuario@uade.edu.ar"
                  required
                  className={dm ? 'dm-input' : ''}
                  style={inputStyle(dm)}
                />
                {recoveryMsg && <p style={messageStyle('#03BB83')}>{recoveryMsg}</p>}
                <button type="submit" disabled={!recoveryEmail.trim()} style={recoveryEmail.trim() ? styles.primaryButton : styles.disabledButton}>
                  Enviar solicitud
                </button>
              </form>

              <button onClick={() => navigate('/login')} style={linkStyle(dm)}>
                Volver al inicio de sesión
              </button>
            </>
          )}
        </div>
      </div>
    </>
  )
}

function labelStyle(dm) {
  return { fontSize: 13, fontWeight: 600, color: dm ? '#CBEEF3' : '#09222A' }
}

function inputStyle(dm) {
  return {
    padding: '10px 14px',
    border: `1.5px solid ${dm ? '#3D6574' : '#CBEEF3'}`,
    borderRadius: 8,
    fontSize: 15,
    outline: 'none',
    color: dm ? '#F4F8FA' : '#09222A',
    background: dm ? '#122430' : '#fff',
  }
}

function messageStyle(borderColor) {
  return {
    color: '#09222A',
    fontSize: 13,
    background: '#CBEEF3',
    padding: '8px 12px',
    borderRadius: 6,
    borderLeft: `3px solid ${borderColor}`,
  }
}

function linkStyle(dm) {
  return {
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
  }
}

const styles = {
  form: { display: 'flex', flexDirection: 'column', gap: 12 },
  themeButton: {
    position: 'absolute',
    top: 16,
    right: 16,
    background: 'none',
    border: 'none',
    fontSize: 13,
    cursor: 'pointer',
    color: '#1956D8',
    fontWeight: 700,
  },
  inlineButton: {
    position: 'absolute',
    right: 10,
    top: '50%',
    transform: 'translateY(-50%)',
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    fontSize: 13,
    fontWeight: 700,
  },
  primaryButton: {
    marginTop: 8,
    padding: '12px',
    background: '#1956D8',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    fontSize: 15,
    fontWeight: 600,
    cursor: 'pointer',
  },
  disabledButton: {
    marginTop: 8,
    padding: '12px',
    background: '#C9DDE3',
    color: '#536B76',
    border: 'none',
    borderRadius: 8,
    fontSize: 15,
    fontWeight: 600,
    cursor: 'not-allowed',
  },
}
