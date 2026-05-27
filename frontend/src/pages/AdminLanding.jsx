import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'
import Logo from '../components/Logo.jsx'

const ROLES = ['ALUMNO', 'PROFESOR', 'DIRECTOR_DE_CATEDRA', 'ADMINISTRADOR']
const ROLE_LABELS = {
  ALUMNO: 'Alumno',
  PROFESOR: 'Profesor',
  DIRECTOR_DE_CATEDRA: 'Director de Cátedra',
  ADMINISTRADOR: 'Administrador',
}

function emptyForm() {
  return { fullName: '', email: '', password: '', role: 'ALUMNO' }
}

export default function AdminLanding() {
  const navigate = useNavigate()
  const user = JSON.parse(localStorage.getItem('user') || '{}')

  const [tab, setTab] = useState('users')
  const [users, setUsers] = useState([])
  const [loadingUsers, setLoadingUsers] = useState(false)
  const [searchEmail, setSearchEmail] = useState('')

  const [showForm, setShowForm] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(emptyForm())
  const [formError, setFormError] = useState('')

  const [totalUsers, setTotalUsers] = useState(0)

  const [tokenModal, setTokenModal] = useState(null)
  const [tokenInput, setTokenInput] = useState('')
  const [tokenError, setTokenError] = useState('')
  const [formSuccess, setFormSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const [policy, setPolicy] = useState({ minLength: 8, maxLength: 100, requireUppercase: false, requireLowercase: false, requireNumbers: false, requireSpecialChars: false })
  const [policyMsg, setPolicyMsg] = useState('')
  const [exams, setExams] = useState([])
  const [examsMsg, setExamsMsg] = useState('')

  const fetchUsers = useCallback(async () => {
    setLoadingUsers(true)
    try {
      const params = searchEmail ? { search: searchEmail } : {}
      const res = await api.get('/users', { params })
      const list = res.data.content ?? res.data ?? []
      setUsers(list)
      setTotalUsers(res.data.totalElements ?? list.length)
    } catch {
      setUsers([])
    } finally {
      setLoadingUsers(false)
    }
  }, [searchEmail])

  useEffect(() => {
    if (tab === 'users') fetchUsers()
  }, [tab, fetchUsers])

  useEffect(() => {
    if (tab === 'policy') {
      api.get('/config/password-policy').then(res => setPolicy(res.data)).catch(() => {})
    }
  }, [tab])

  useEffect(() => {
    if (tab === 'exams') {
      api.get('/exams/supervision')
        .then(res => setExams(res.data))
        .catch(err => setExamsMsg(err.response?.data?.message || 'Error al cargar examenes.'))
    }
  }, [tab])

  async function handleLogout() {
    try { await api.post('/auth/logout') } finally {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      navigate('/login')
    }
  }

  function openCreate() {
    if (totalUsers >= 60) {
      setTokenInput('')
      setTokenError('')
      setTokenModal('create')
      return
    }
    setEditingId(null)
    setForm(emptyForm())
    setFormError('')
    setFormSuccess('')
    setShowForm(true)
  }

  function openEdit(u) {
    setEditingId(u.id)
    setForm({ fullName: u.fullName, email: u.email, password: '', role: u.role })
    setFormError('')
    setFormSuccess('')
    setShowForm(true)
  }

  async function handleSubmit(e) {
    e.preventDefault()
    setFormError('')
    setFormSuccess('')
    setSubmitting(true)
    try {
      if (editingId) {
        await api.put(`/users/${editingId}`, {
          fullName: form.fullName,
          email: form.email,
          role: form.role,
          newPassword: form.password || null,
        })
        setFormSuccess('Usuario actualizado correctamente.')
      } else {
        await api.post('/users', {
          fullName: form.fullName,
          email: form.email,
          role: form.role,
          initialPassword: form.password,
        })
        setFormSuccess('Usuario creado correctamente.')
      }
      fetchUsers()
      if (!editingId) setForm(emptyForm())
    } catch (err) {
      setFormError(err.response?.data?.message || 'Error al guardar el usuario.')
    } finally {
      setSubmitting(false)
    }
  }

  function requestToggleStatus(u) {
    if (u.status === 'ACTIVO') {
      setTokenInput('')
      setTokenError('')
      setTokenModal(u)
    } else {
      doReactivate(u)
    }
  }

  async function doReactivate(u) {
    try {
      await api.patch(`/users/${u.id}/reactivate`)
      fetchUsers()
    } catch (err) {
      alert(err.response?.data?.message || 'Error al reactivar.')
    }
  }

  async function confirmToken() {
    if (tokenInput !== '4989') {
      setTokenError('Token de Dev incorrecto.')
      return
    }
    if (tokenModal === 'create') {
      setTokenModal(null)
      setEditingId(null)
      setForm(emptyForm())
      setFormError('')
      setFormSuccess('')
      setShowForm(true)
    } else {
      try {
        await api.patch(`/users/${tokenModal.id}/deactivate`)
        setTokenModal(null)
        fetchUsers()
      } catch (err) {
        setTokenModal(null)
        alert(err.response?.data?.message || 'Error al desactivar.')
      }
    }
  }

  async function savePolicy(e) {
    e.preventDefault()
    setPolicyMsg('')
    try {
      await api.put('/config/password-policy', policy)
      setPolicyMsg('Política actualizada correctamente.')
    } catch {
      setPolicyMsg('Error al guardar la política.')
    }
  }

  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          <Logo dark size={36} />
          <div>
            <h1 style={styles.headerTitle}>7test — Panel de Administración</h1>
            <span style={styles.headerUser}>{user.fullName || user.email}</span>
            <span style={styles.badge}>Administrador</span>
          </div>
        </div>
        <button onClick={handleLogout} style={styles.logoutBtn}>Cerrar sesión</button>
      </header>

      <div style={styles.tabBar}>
        <button onClick={() => setTab('users')} style={tab === 'users' ? styles.tabActive : styles.tab}>Usuarios</button>
        <button onClick={() => setTab('policy')} style={tab === 'policy' ? styles.tabActive : styles.tab}>Política de Contraseñas</button>
        <button onClick={() => setTab('exams')} style={tab === 'exams' ? styles.tabActive : styles.tab}>Examenes</button>
      </div>

      <main style={styles.main}>
        {tab === 'users' && (
          <div>
            <div style={styles.toolbar}>
              <div style={styles.searchRow}>
                <input
                  placeholder="Buscar por nombre o email..."
                  value={searchEmail}
                  onChange={(e) => setSearchEmail(e.target.value)}
                  style={styles.searchInput}
                />
                <button onClick={fetchUsers} style={styles.btnSecondary}>Search</button>
              </div>
              <button onClick={openCreate} style={styles.btnPrimary}>+ Nuevo usuario</button>
            </div>

            {showForm && (
              <div style={styles.formCard}>
                <h3 style={styles.formTitle}>{editingId ? 'Editar usuario' : 'Crear usuario'}</h3>
                <form onSubmit={handleSubmit} style={styles.formGrid}>
                  <div style={styles.fieldGroup}>
                    <label style={styles.label}>Nombre completo</label>
                    <input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required style={styles.input} />
                  </div>
                  <div style={styles.fieldGroup}>
                    <label style={styles.label}>Email</label>
                    <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required style={styles.input} />
                  </div>
                  <div style={styles.fieldGroup}>
                    <label style={styles.label}>Contraseña {editingId && '(dejar vacío para no cambiar)'}</label>
                    <input type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required={!editingId} style={styles.input} />
                  </div>
                  <div style={styles.fieldGroup}>
                    <label style={styles.label}>Rol</label>
                    <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })} style={styles.input}>
                      {ROLES.map(r => <option key={r} value={r}>{ROLE_LABELS[r]}</option>)}
                    </select>
                  </div>
                  {formError && <p style={styles.error}>{formError}</p>}
                  {formSuccess && <p style={styles.success}>{formSuccess}</p>}
                  <div style={styles.formActions}>
                    <button type="submit" disabled={submitting} style={styles.btnPrimary}>{submitting ? 'Guardando...' : 'Guardar'}</button>
                    <button type="button" onClick={() => setShowForm(false)} style={styles.btnSecondary}>Cancelar</button>
                  </div>
                </form>
              </div>
            )}

            {loadingUsers ? (
              <p style={{ padding: 24, color: '#09222A', opacity: 0.5 }}>Cargando usuarios...</p>
            ) : (
              <div style={styles.tableWrapper}>
                <table style={styles.table}>
                  <thead>
                    <tr>
                      <th style={styles.th}>Nombre</th>
                      <th style={styles.th}>Email</th>
                      <th style={styles.th}>Rol</th>
                      <th style={styles.th}>Estado</th>
                      <th style={styles.th}>Acciones</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.length === 0 ? (
                      <tr><td colSpan={5} style={{ textAlign: 'center', padding: 24, color: '#09222A', opacity: 0.4 }}>No se encontraron usuarios.</td></tr>
                    ) : users.map(u => (
                      <tr key={u.id} style={u.status === 'INACTIVO' ? styles.inactiveRow : {}}>
                        <td style={styles.td}>{u.fullName}</td>
                        <td style={styles.td}>{u.email}</td>
                        <td style={styles.td}>{ROLE_LABELS[u.role] ?? u.role}</td>
                        <td style={styles.td}>
                          <span style={u.status === 'ACTIVO' ? styles.statusActive : styles.statusInactive}>
                            {u.status}
                          </span>
                        </td>
                        <td style={styles.td}>
                          <button onClick={() => openEdit(u)} style={styles.actionBtn}>Editar</button>
                          <button onClick={() => requestToggleStatus(u)} style={{ ...styles.actionBtn, color: u.status === 'ACTIVO' ? '#FFC012' : '#03BB83' }}>
                            {u.status === 'ACTIVO' ? 'Desactivar' : 'Reactivar'}
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {tab === 'policy' && (
          <div style={styles.policyCard}>
            <h3 style={styles.formTitle}>Política de contraseñas</h3>
            <form onSubmit={savePolicy} style={styles.policyForm}>
              <div style={{ display: 'flex', gap: 16 }}>
                <div style={styles.fieldGroup}>
                  <label style={styles.label}>Longitud mínima</label>
                  <input type="number" min={1} max={policy.maxLength} value={policy.minLength} onChange={(e) => setPolicy({ ...policy, minLength: parseInt(e.target.value) })} style={{ ...styles.input, width: 80 }} />
                </div>
                <div style={styles.fieldGroup}>
                  <label style={styles.label}>Longitud máxima</label>
                  <input type="number" min={policy.minLength} max={200} value={policy.maxLength} onChange={(e) => setPolicy({ ...policy, maxLength: parseInt(e.target.value) })} style={{ ...styles.input, width: 80 }} />
                </div>
              </div>
              <div style={styles.checkRow}>
                <input type="checkbox" id="upper" checked={policy.requireUppercase} onChange={(e) => setPolicy({ ...policy, requireUppercase: e.target.checked })} />
                <label htmlFor="upper" style={styles.checkLabel}>Requerir mayúscula</label>
              </div>
              <div style={styles.checkRow}>
                <input type="checkbox" id="lower" checked={policy.requireLowercase} onChange={(e) => setPolicy({ ...policy, requireLowercase: e.target.checked })} />
                <label htmlFor="lower" style={styles.checkLabel}>Requerir minúscula</label>
              </div>
              <div style={styles.checkRow}>
                <input type="checkbox" id="num" checked={policy.requireNumbers} onChange={(e) => setPolicy({ ...policy, requireNumbers: e.target.checked })} />
                <label htmlFor="num" style={styles.checkLabel}>Requerir número</label>
              </div>
              <div style={styles.checkRow}>
                <input type="checkbox" id="special" checked={policy.requireSpecialChars} onChange={(e) => setPolicy({ ...policy, requireSpecialChars: e.target.checked })} />
                <label htmlFor="special" style={styles.checkLabel}>Requerir carácter especial</label>
              </div>
              {policyMsg && <p style={policyMsg.includes('Error') ? styles.error : styles.success}>{policyMsg}</p>}
              <button type="submit" style={styles.btnPrimary}>Guardar política</button>
            </form>
          </div>
        )}

        {tab === 'exams' && (
          <div style={styles.tableWrapper}>
            {examsMsg && <p style={styles.error}>{examsMsg}</p>}
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
                  <tr><td colSpan={6} style={{ textAlign: 'center', padding: 24, color: '#09222A', opacity: 0.4 }}>No hay examenes cargados.</td></tr>
                ) : exams.map(exam => (
                  <tr key={exam.id}>
                    <td style={styles.td}>{exam.title}</td>
                    <td style={styles.td}>{exam.courseName || 'Testing de Aplicaciones'}</td>
                    <td style={styles.td}>{exam.teacherName}</td>
                    <td style={styles.td}>{exam.status}</td>
                    <td style={styles.td}>{exam.topics?.length || 0}</td>
                    <td style={styles.td}>{exam.updatedAt ? new Date(exam.updatedAt).toLocaleString('es-AR') : '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>

      {tokenModal && (
        <div style={styles.overlay}>
          <div style={styles.modal}>
            <h3 style={styles.modalTitle}>
              {tokenModal === 'create' ? 'Límite de usuarios alcanzado' : 'Confirmar desactivación'}
            </h3>
            <p style={styles.modalText}>
              {tokenModal === 'create'
                ? `Hay ${totalUsers} usuarios registrados (límite: 60). Ingresá el token de Dev para continuar:`
                : <span>Para desactivar a <strong>{tokenModal.fullName}</strong> ingresá el token de Dev:</span>
              }
            </p>
            <input
              type="password"
              maxLength={4}
              value={tokenInput}
              onChange={e => { setTokenInput(e.target.value); setTokenError('') }}
              placeholder="• • • •"
              autoFocus
              style={styles.tokenInput}
            />
            {tokenError && <p style={styles.tokenError}>{tokenError}</p>}
            <div style={styles.modalActions}>
              <button onClick={confirmToken} style={styles.btnPrimary}>Confirmar</button>
              <button onClick={() => setTokenModal(null)} style={styles.btnSecondary}>Cancelar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

const styles = {
  page: { minHeight: '100vh', background: '#CBEEF3' },
  header: { background: '#09222A', color: '#fff', padding: '16px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  headerTitle: { fontSize: 20, fontWeight: 700, marginBottom: 4 },
  headerUser: { fontSize: 13, opacity: 0.7, marginRight: 8 },
  badge: { background: 'rgba(203,238,243,0.2)', color: '#CBEEF3', padding: '2px 10px', borderRadius: 20, fontSize: 11, fontWeight: 600 },
  logoutBtn: { padding: '8px 18px', background: 'rgba(203,238,243,0.1)', border: '1px solid rgba(203,238,243,0.4)', color: '#CBEEF3', borderRadius: 8, fontWeight: 600, fontSize: 13 },
  tabBar: { background: '#fff', borderBottom: '2px solid #CBEEF3', display: 'flex', padding: '0 32px' },
  tab: { padding: '14px 20px', background: 'none', border: 'none', borderBottom: '3px solid transparent', color: '#09222A', opacity: 0.5, fontSize: 14, fontWeight: 500, cursor: 'pointer' },
  tabActive: { padding: '14px 20px', background: 'none', border: 'none', borderBottom: '3px solid #1956D8', color: '#1956D8', fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  main: { padding: 32, maxWidth: 1100, margin: '0 auto' },
  toolbar: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
  searchRow: { display: 'flex', gap: 8 },
  searchInput: { padding: '8px 14px', border: '1.5px solid #CBEEF3', borderRadius: 8, fontSize: 14, width: 260, color: '#09222A', outline: 'none' },
  btnPrimary: { padding: '9px 20px', background: '#1956D8', color: '#fff', border: 'none', borderRadius: 8, fontSize: 14, fontWeight: 600, cursor: 'pointer' },
  btnSecondary: { padding: '9px 16px', background: '#fff', color: '#09222A', border: '1.5px solid #CBEEF3', borderRadius: 8, fontSize: 14, fontWeight: 500, cursor: 'pointer' },
  formCard: { background: '#fff', borderRadius: 10, padding: 24, marginBottom: 24, border: '1.5px solid #CBEEF3' },
  formTitle: { fontSize: 16, fontWeight: 700, color: '#1956D8', marginBottom: 16 },
  formGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 },
  fieldGroup: { display: 'flex', flexDirection: 'column', gap: 4 },
  label: { fontSize: 12, fontWeight: 600, color: '#09222A' },
  input: { padding: '8px 12px', border: '1.5px solid #CBEEF3', borderRadius: 8, fontSize: 14, color: '#09222A', outline: 'none' },
  formActions: { gridColumn: '1 / -1', display: 'flex', gap: 10 },
  error: { gridColumn: '1 / -1', color: '#09222A', background: '#CBEEF3', padding: '8px 12px', borderRadius: 6, fontSize: 13, borderLeft: '3px solid #FFC012' },
  success: { gridColumn: '1 / -1', color: '#09222A', background: '#CBEEF3', padding: '8px 12px', borderRadius: 6, fontSize: 13, borderLeft: '3px solid #03BB83' },
  tableWrapper: { background: '#fff', borderRadius: 10, overflow: 'hidden', border: '1.5px solid #CBEEF3' },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: { background: '#CBEEF3', padding: '12px 16px', textAlign: 'left', fontSize: 12, fontWeight: 700, color: '#09222A', borderBottom: '2px solid #CBEEF3' },
  td: { padding: '12px 16px', fontSize: 14, color: '#09222A', borderBottom: '1px solid #CBEEF3' },
  inactiveRow: { opacity: 0.5 },
  statusActive: { background: '#CBEEF3', color: '#03BB83', padding: '2px 10px', borderRadius: 20, fontSize: 12, fontWeight: 700 },
  statusInactive: { background: '#CBEEF3', color: '#FFC012', padding: '2px 10px', borderRadius: 20, fontSize: 12, fontWeight: 700 },
  actionBtn: { background: 'none', border: 'none', fontSize: 13, fontWeight: 600, color: '#1956D8', marginRight: 8, cursor: 'pointer', textDecoration: 'underline' },
  policyCard: { background: '#fff', borderRadius: 10, padding: 32, maxWidth: 480, border: '1.5px solid #CBEEF3' },
  policyForm: { display: 'flex', flexDirection: 'column', gap: 16 },
  checkRow: { display: 'flex', alignItems: 'center', gap: 10 },
  checkLabel: { fontSize: 14, color: '#09222A' },
  overlay: { position: 'fixed', inset: 0, background: 'rgba(9,34,42,0.55)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 },
  modal: { background: '#fff', borderRadius: 12, padding: '32px 36px', width: 340, boxShadow: '0 8px 32px rgba(9,34,42,0.22)', border: '1.5px solid #CBEEF3' },
  modalTitle: { fontSize: 17, fontWeight: 700, color: '#09222A', marginBottom: 12 },
  modalText: { fontSize: 14, color: '#09222A', marginBottom: 16, lineHeight: 1.5 },
  tokenInput: { width: '100%', padding: '12px', border: '1.5px solid #CBEEF3', borderRadius: 8, fontSize: 22, textAlign: 'center', letterSpacing: 8, color: '#09222A', outline: 'none', boxSizing: 'border-box' },
  tokenError: { color: '#FFC012', fontSize: 13, fontWeight: 600, marginTop: 8 },
  modalActions: { display: 'flex', gap: 10, marginTop: 20 },
}
