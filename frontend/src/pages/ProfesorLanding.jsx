import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'
import Logo from '../components/Logo.jsx'

export default function ProfesorLanding() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('user') || '{}')

  async function handleLogout() {
    try {
      await api.post('/auth/logout')
    } finally {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      navigate('/login')
    }
  }

  return (
    <div style={styles.container}>
      <div style={styles.card}>
        <div style={styles.header}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <Logo dark={false} size={40} />
            <div>
              <h1 style={styles.title}>Bienvenido/a, {user.fullName || user.email}</h1>
              <span style={styles.badge}>Profesor</span>
            </div>
          </div>
          <button onClick={handleLogout} style={styles.logoutBtn}>Cerrar sesión</button>
        </div>
        <div style={styles.content}>
          <p style={styles.placeholder}>Panel de creación de exámenes — próximamente.</p>
          <p style={styles.note}>Aquí podrás crear y gestionar tus exámenes en los próximos sprints.</p>
        </div>
      </div>
    </div>
  )
}

const styles = {
  container: { minHeight: '100vh', background: '#CBEEF3', padding: 24 },
  card: { maxWidth: 800, margin: '0 auto', background: '#fff', borderRadius: 12, padding: 32, boxShadow: '0 2px 12px rgba(9,34,42,0.10)', border: '1.5px solid #CBEEF3' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 32 },
  title: { fontSize: 22, fontWeight: 700, color: '#09222A', marginBottom: 6 },
  badge: { display: 'inline-block', background: '#CBEEF3', color: '#1956D8', padding: '3px 10px', borderRadius: 20, fontSize: 12, fontWeight: 700 },
  logoutBtn: { padding: '8px 18px', background: '#fff', border: '1.5px solid #09222A', color: '#09222A', borderRadius: 8, fontWeight: 600, fontSize: 13, cursor: 'pointer' },
  content: { textAlign: 'center', padding: '40px 0' },
  placeholder: { fontSize: 16, color: '#09222A', marginBottom: 8 },
  note: { fontSize: 13, color: '#09222A', opacity: 0.5 },
}
