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
  const [showPassword, setShowPassword] = useState(false)

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
      setError(err.response?.data?.message || 'No se pudo iniciar sesión.')
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
      setRecoveryMsg('Revisá que el email tenga un formato válido.')
    }
  }

  const canSubmit = email.trim() && password.trim() && !loading

  return (
    <div style={styles.root}>
      {/* Panel izquierdo */}
      <div style={styles.leftPanel}>
        <div style={styles.leftContent}>
          <div style={styles.logoRow}>
            <Logo dark={false} size={48} />
            <span style={styles.brandName}>7test</span>
          </div>
          <h2 style={styles.tagline}>
            Toda la gestión de evaluaciones en una sola plataforma.
          </h2>
          <p style={styles.sub}>UADE · Testing de Aplicaciones</p>
        </div>
      </div>

      {/* Panel derecho */}
      <div style={styles.rightPanel}>
        <div style={styles.formCard}>
          {!recovery ? (
            <>
              <h1 style={styles.formTitle}>Bienvenido, iniciá sesión</h1>
              <p style={styles.formSub}>Ingresá con tu cuenta institucional</p>

              <form onSubmit={handleLogin} style={styles.form}>
                <div style={styles.fieldGroup}>
                  <label htmlFor="email" style={styles.label}>Email institucional</label>
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
                    style={styles.input}
                  />
                </div>

                <div style={styles.fieldGroup}>
                  <label htmlFor="password" style={styles.label}>Contraseña</label>
                  <div style={{ position: 'relative' }}>
                    <input
                      id="password"
                      type={showPassword ? 'text' : 'password'}
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="Ingresá tu contraseña"
                      required
                      style={{ ...styles.input, paddingRight: 72, boxSizing: 'border-box', width: '100%' }}
                    />
                    <button
                      type="button"
                      onClick={() => setShowPassword(!showPassword)}
                      style={styles.showBtn}
                    >
                      {showPassword ? 'Ocultar' : 'Ver'}
                    </button>
                  </div>
                </div>

                {error && (
                  <p style={styles.errorMsg}>{error}</p>
                )}

                <button
                  type="submit"
                  disabled={!canSubmit}
                  style={canSubmit ? styles.submitBtn : styles.submitBtnDisabled}
                >
                  {loading ? 'Ingresando...' : 'Ingresá'}
                </button>
              </form>

              <button onClick={() => navigate('/recuperar-contrasena')} style={styles.linkBtn}>
                Olvidé mi contraseña
              </button>
            </>
          ) : (
            <>
              <h1 style={styles.formTitle}>Recuperar contraseña</h1>
              <p style={styles.formSub}>Te enviaremos las instrucciones por email</p>

              <form onSubmit={handleRecovery} style={styles.form}>
                <div style={styles.fieldGroup}>
                  <label htmlFor="recovery-email" style={styles.label}>Email institucional</label>
                  <input
                    id="recovery-email"
                    type="email"
                    value={recoveryEmail}
                    onChange={(e) => setRecoveryEmail(e.target.value)}
                    placeholder="usuario@uade.edu.ar"
                    required
                    style={styles.input}
                  />
                </div>
                {recoveryMsg && <p style={{ ...styles.errorMsg, borderColor: '#03BB83', background: '#F0FDF4', color: '#087A55' }}>{recoveryMsg}</p>}
                <button
                  type="submit"
                  disabled={!recoveryEmail.trim()}
                  style={recoveryEmail.trim() ? styles.submitBtn : styles.submitBtnDisabled}
                >
                  Enviar solicitud
                </button>
              </form>

              <button onClick={() => navigate('/login')} style={styles.linkBtn}>
                ← Volver al inicio de sesión
              </button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

const styles = {
  root: {
    display: 'flex',
    minHeight: '100vh',
  },
  leftPanel: {
    flex: '0 0 45%',
    background: 'linear-gradient(150deg, #09222A 0%, #0d3060 50%, #1956D8 100%)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '48px 56px',
    '@media (max-width: 768px)': { display: 'none' },
  },
  leftContent: {
    maxWidth: 360,
  },
  logoRow: {
    display: 'flex',
    alignItems: 'center',
    gap: 14,
    marginBottom: 40,
  },
  brandName: {
    fontSize: 32,
    fontWeight: 800,
    color: '#fff',
    letterSpacing: '-0.5px',
  },
  tagline: {
    fontSize: 28,
    fontWeight: 700,
    color: '#fff',
    lineHeight: 1.3,
    margin: '0 0 16px',
  },
  sub: {
    fontSize: 14,
    color: 'rgba(203,238,243,0.7)',
    margin: 0,
  },
  rightPanel: {
    flex: 1,
    background: '#fff',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: '48px 40px',
  },
  formCard: {
    width: '100%',
    maxWidth: 400,
  },
  formTitle: {
    fontSize: 28,
    fontWeight: 700,
    color: '#1956D8',
    margin: '0 0 6px',
  },
  formSub: {
    fontSize: 14,
    color: '#536B76',
    margin: '0 0 32px',
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: 18,
  },
  fieldGroup: {
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
  },
  label: {
    fontSize: 13,
    fontWeight: 600,
    color: '#09222A',
  },
  input: {
    padding: '12px 14px',
    border: '1.5px solid #D8E8EC',
    borderRadius: 8,
    fontSize: 15,
    outline: 'none',
    color: '#09222A',
    background: '#FAFCFD',
    transition: 'border-color .15s',
    width: '100%',
    boxSizing: 'border-box',
  },
  showBtn: {
    position: 'absolute',
    right: 12,
    top: '50%',
    transform: 'translateY(-50%)',
    background: 'none',
    border: 'none',
    cursor: 'pointer',
    fontSize: 13,
    fontWeight: 700,
    color: '#1956D8',
  },
  errorMsg: {
    fontSize: 13,
    color: '#7A0000',
    background: '#FFF0F0',
    padding: '10px 14px',
    borderRadius: 6,
    borderLeft: '3px solid #DC2626',
    margin: 0,
  },
  submitBtn: {
    padding: '13px',
    background: '#1956D8',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    fontSize: 16,
    fontWeight: 700,
    cursor: 'pointer',
    marginTop: 4,
  },
  submitBtnDisabled: {
    padding: '13px',
    background: '#C9DDE3',
    color: '#536B76',
    border: 'none',
    borderRadius: 8,
    fontSize: 16,
    fontWeight: 700,
    cursor: 'not-allowed',
    marginTop: 4,
  },
  linkBtn: {
    marginTop: 20,
    display: 'block',
    textAlign: 'center',
    background: 'none',
    border: 'none',
    color: '#1956D8',
    fontSize: 13,
    textDecoration: 'underline',
    width: '100%',
    cursor: 'pointer',
  },
}
