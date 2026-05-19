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
      await api.post('/auth/password-recovery', { email: recoveryEmail })
      setRecoveryMsg('Si el email existe, recibirás instrucciones para restablecer tu contraseña.')
    } catch {
      setRecoveryMsg('Si el email existe, recibirás instrucciones para restablecer tu contraseña.')
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <h1 style={styles.title}>7test</h1>
        <p style={styles.subtitle}>Plataforma de Evaluaciones — UADE</p>

        {!showRecovery ? (
          <>
            <form onSubmit={handleLogin} style={styles.form}>
              <label style={styles.label}>Email institucional</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="usuario@uade.edu.ar"
                required
                style={styles.input}
              />

              <label style={styles.label}>Contraceña</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                required
                style={styles.input}
              />

              {error && <p style={styles.error}>{error}</p>}

              <button type="submit" disabled={loading} style={styles.button}>
                {loading ? 'Loading...' : 'Ingresar'}
              </button>
            </form>

            <button onClick={() => setShowRecovery(true)} style={styles.link}>
              ¿Olvidaste tu contraseña?
            </button>
          </>
        ) : (
          <>
            <form onSubmit={handleRecovery} style={styles.form}>
              <label style={styles.label}>Ingresá tu email para recuperar la contraseña</label>
              <input
                type="email"
                value={recoveryEmail}
                onChange={(e) => setRecoveryEmail(e.target.value)}
                placeholder="usuario@uade.edu.ar"
                required
                style={styles.input}
              />
              {recoveryMsg && <p style={styles.success}>{recoveryMsg}</p>}
              <button type="submit" style={styles.button}>Enviar solicitud</button>
            </form>

            <button onClick={() => setShowRecovery(false)} style={styles.link}>
              ← Volver al login
            </button>
          </>
        )}
      </div>
    </div>
  )
}

const styles = {
  container: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    background: 'linear-gradient(135deg, #1a237e 0%, #283593 100%)',
  },
  card: {
    background: '#fff',
    borderRadius: 12,
    padding: '40px 48px',
    width: '100%',
    maxWidth: 420,
    boxShadow: '0 8px 32px rgba(0,0,0,0.18)',
  },
  title: {
    fontSize: 32,
    fontWeight: 700,
    color: '#1a237e',
    textAlign: 'center',
    marginBottom: 4,
  },
  subtitle: {
    fontSize: 13,
    color: '#666',
    textAlign: 'center',
    marginBottom: 32,
  },
  form: {
    display: 'flex',
    flexDirection: 'column',
    gap: 12,
  },
  label: {
    fontSize: 13,
    fontWeight: 600,
    color: '#444',
  },
  input: {
    padding: '10px 14px',
    border: '1.5px solid #ddd',
    borderRadius: 8,
    fontSize: 15,
    outline: 'none',
    transition: 'border-color 0.2s',
  },
  button: {
    marginTop: 8,
    padding: '12px',
    background: '#1a237e',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    fontSize: 15,
    fontWeight: 600,
    transition: 'background 0.2s',
  },
  link: {
    marginTop: 16,
    display: 'block',
    textAlign: 'center',
    background: 'none',
    border: 'none',
    color: '#1a237e',
    fontSize: 13,
    textDecoration: 'underline',
    width: '100%',
  },
  error: {
    color: '#c62828',
    fontSize: 13,
    background: '#ffebee',
    padding: '8px 12px',
    borderRadius: 6,
  },
  success: {
    color: '#2e7d32',
    fontSize: 13,
    background: '#e8f5e9',
    padding: '8px 12px',
    borderRadius: 6,
  },
}
