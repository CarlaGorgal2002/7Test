import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'

export default function DirectorLanding() {
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
          <div>
            <h1 style={styles.title}>Bienvenido/a, {user.fullName || user.email}</h1>
            <span style={styles.badge}>Director de Cátedra</span>
          </div>
          <button onClick={handleLogout} style={styles.logoutBtn}>Cerrar sesión</button>
        </div>
        <div style={styles.content}>
          <p style={styles.placeholder}>Panel de supervisión — próximamente.</p>
          <p style={styles.note}>Aquí podrás supervisar los exámenes y reportes de la cátedra.</p>
        </div>
      </div>
    </div>
  )
}

const styles = {
  container: { minHeight: '100vh', background: '#f5f5f5', padding: 24 },
  card: { maxWidth: 800, margin: '0 auto', background: '#fff', borderRadius: 12, padding: 32, boxShadow: '0 2px 12px rgba(0,0,0,0.08)' },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 32 },
  title: { fontSize: 22, fontWeight: 700, color: '#1a237e', marginBottom: 6 },
  badge: { display: 'inline-block', background: '#e8f5e9', color: '#2e7d32', padding: '3px 10px', borderRadius: 20, fontSize: 12, fontWeight: 600 },
  logoutBtn: { padding: '8px 18px', background: '#fff', border: '1.5px solid #e53935', color: '#e53935', borderRadius: 8, fontWeight: 600, fontSize: 13 },
  content: { textAlign: 'center', padding: '40px 0' },
  placeholder: { fontSize: 16, color: '#555', marginBottom: 8 },
  note: { fontSize: 13, color: '#999' },
}
