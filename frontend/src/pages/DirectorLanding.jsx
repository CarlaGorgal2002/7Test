import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'
import { clearSession, getCurrentUser } from '../auth/session.js'
import Logo from '../components/Logo.jsx'

export default function DirectorLanding() {
  const navigate = useNavigate()
  const user = getCurrentUser() || {}
  const [exams, setExams] = useState([])
  const [status, setStatus] = useState('')
  const [message, setMessage] = useState('')

  useEffect(() => {
    const params = status ? { status } : {}
    api.get('/exams/supervision', { params })
      .then((res) => setExams(res.data))
      .catch((err) => setMessage(err.response?.data?.message || 'No se pudieron cargar los examenes.'))
  }, [status])

  async function handleLogout() {
    try {
      await api.post('/auth/logout')
    } finally {
      clearSession()
      navigate('/login', { replace: true })
    }
  }

  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <div style={styles.brand}>
          <Logo dark size={36} />
          <div>
            <h1 style={styles.headerTitle}>Panel de Director de Catedra</h1>
            <span style={styles.headerUser}>{user.fullName || user.email}</span>
          </div>
        </div>
        <button onClick={handleLogout} style={styles.logoutBtn}>Cerrar sesión</button>
      </header>

      <main style={styles.main}>
        <div style={styles.toolbar}>
          <h2 style={styles.title}>Estado de examenes</h2>
          <select value={status} onChange={(e) => setStatus(e.target.value)} style={styles.select}>
            <option value="">Todos</option>
            <option value="BORRADOR">Borrador</option>
            <option value="PUBLICADO">Publicado</option>
            <option value="CERRADO">Cerrado</option>
          </select>
        </div>

        {message && <div style={styles.message}>{message}</div>}

        <div style={styles.tableWrapper}>
          <table style={styles.table}>
            <thead>
              <tr>
                <th style={styles.th}>Examen</th>
                <th style={styles.th}>Materia</th>
                <th style={styles.th}>Profesor</th>
                <th style={styles.th}>Estado</th>
                <th style={styles.th}>Temas</th>
                <th style={styles.th}>Actualizado</th>
              </tr>
            </thead>
            <tbody>
              {exams.length === 0 ? (
                <tr><td colSpan={6} style={styles.empty}>No hay examenes para mostrar.</td></tr>
              ) : exams.map((exam) => (
                <tr key={exam.id}>
                  <td style={styles.td}>{exam.title}</td>
                  <td style={styles.td}>{exam.courseName || 'Testing de Aplicaciones'}</td>
                  <td style={styles.td}>{exam.teacherName}</td>
                  <td style={styles.td}><span style={statusBadge(exam.status)}>{labelStatus(exam.status)}</span></td>
                  <td style={styles.td}>{exam.topics?.length || 0}</td>
                  <td style={styles.td}>{formatDate(exam.updatedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </main>
    </div>
  )
}

function labelStatus(status) {
  return { BORRADOR: 'Borrador', PUBLICADO: 'Publicado', CERRADO: 'Cerrado' }[status] || status
}

function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' })
}

function statusBadge(status) {
  const base = { padding: '3px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 }
  if (status === 'PUBLICADO') return { ...base, background: '#DDF6EC', color: '#087A55' }
  if (status === 'CERRADO') return { ...base, background: '#ECEFF3', color: '#4A5565' }
  return { ...base, background: '#E6EEFF', color: '#1956D8' }
}

const styles = {
  page: { minHeight: '100vh', background: '#F4F8FA', color: '#09222A' },
  header: { background: '#09222A', color: '#fff', padding: '16px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  brand: { display: 'flex', alignItems: 'center', gap: 12 },
  headerTitle: { fontSize: 20, fontWeight: 700, margin: 0 },
  headerUser: { fontSize: 13, opacity: 0.72 },
  logoutBtn: { padding: '8px 18px', background: 'rgba(203,238,243,0.1)', border: '1px solid rgba(203,238,243,0.4)', color: '#CBEEF3', borderRadius: 8, fontWeight: 600, fontSize: 13, cursor: 'pointer' },
  main: { padding: 32, maxWidth: 1120, margin: '0 auto' },
  toolbar: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 },
  title: { fontSize: 24, margin: 0 },
  select: { minHeight: 38, padding: '8px 12px', border: '1px solid #C9DDE3', borderRadius: 6, color: '#09222A', background: '#fff' },
  message: { background: '#FFF8DF', border: '1px solid #E7CE74', color: '#5D4700', padding: '10px 12px', borderRadius: 8, marginBottom: 14, fontSize: 14 },
  tableWrapper: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, overflow: 'hidden' },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: { background: '#E7F0F3', padding: '12px 14px', textAlign: 'left', fontSize: 12, fontWeight: 800, color: '#304653' },
  td: { padding: '12px 14px', borderTop: '1px solid #E7F0F3', fontSize: 14 },
  empty: { padding: 24, textAlign: 'center', color: '#536B76' },
}
