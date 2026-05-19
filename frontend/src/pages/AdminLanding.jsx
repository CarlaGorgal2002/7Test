import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'

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
  const [formSuccess, setFormSuccess] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const [policy, setPolicy] = useState({ minLength: 8, maxLength: 100, requireUppercase: false, requireLowercase: false, requireNumbers: false, requireSpecialChars: false })
  const [policyMsg, setPolicyMsg] = useState('')

  const fetchUsers = useCallback(async () => {
    setLoadingUsers(true)
    try {
      const params = searchEmail ? { search: searchEmail } : {}
      const res = await api.get('/users', { params })
      setUsers(res.data.content ?? res.data ?? [])
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

  async function handleLogout() {
    try { await api.post('/auth/logout') } finally {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      navigate('/login')
    }
  }

  function openCreate() {
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

  async function toggleStatus(u) {
    try {
      if (u.status === 'ACTIVO') {
        await api.patch(`/users/${u.id}/deactivate`)
      } else {
        await api.patch(`/users/${u.id}/reactivate`)
      }
      fetchUsers()
    } catch (err) {
      alert(err.response?.data?.message || 'Error al cambiar estado.')
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
        <div>
          <h1 style={styles.headerTitle}>7test — Panel de Administración</h1>
          <span style={styles.headerUser}>{user.fullName || user.email}</span>
          <span style={styles.badge}>Administrador</span>
        </div>
        <button onClick={handleLogout} style={styles.logoutBtn}>Cerrar sesión</button>
      </header>

      <div style={styles.tabBar}>
        <button onClick={() => setTab('users')} style={tab === 'users' ? styles.tabActive : styles.tab}>Usuarios</button>
        <button onClick={() => setTab('policy')} style={tab === 'policy' ? styles.tabActive : styles.tab}>Política de Contraseñas</button>
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
              <p style={{ padding: 24, color: '#888' }}>Cargando usuarios...</p>
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
                      <tr><td colSpan={5} style={{ textAlign: 'center', padding: 24, color: '#aaa' }}>No se encontraron usuarios.</td></tr>
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
                          <button onClick={() => toggleStatus(u)} style={{ ...styles.actionBtn, color: u.status === 'ACTIVO' ? '#e53935' : '#2e7d32' }}>
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
      </main>
    </div>
  )
}

const styles = {
  page: { minHeight: '100vh', background: '#f5f5f5' },
  header: { background: '#1a237e', color: '#fff', padding: '16px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  headerTitle: { fontSize: 20, fontWeight: 700, marginBottom: 4 },
  headerUser: { fontSize: 13, opacity: 0.8, marginRight: 8 },
  badge: { background: 'rgba(255,255,255,0.2)', padding: '2px 10px', borderRadius: 20, fontSize: 11, fontWeight: 600 },
  logoutBtn: { padding: '8px 18px', background: 'rgba(255,255,255,0.1)', border: '1px solid rgba(255,255,255,0.4)', color: '#fff', borderRadius: 8, fontWeight: 600, fontSize: 13 },
  tabBar: { background: '#fff', borderBottom: '1px solid #e0e0e0', display: 'flex', padding: '0 32px' },
  tab: { padding: '14px 20px', background: 'none', border: 'none', borderBottom: '3px solid transparent', color: '#666', fontSize: 14, fontWeight: 500 },
  tabActive: { padding: '14px 20px', background: 'none', border: 'none', borderBottom: '3px solid #1a237e', color: '#1a237e', fontSize: 14, fontWeight: 700 },
  main: { padding: 32, maxWidth: 1100, margin: '0 auto' },
  toolbar: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
  searchRow: { display: 'flex', gap: 8 },
  searchInput: { padding: '8px 14px', border: '1.5px solid #ddd', borderRadius: 8, fontSize: 14, width: 260 },
  btnPrimary: { padding: '9px 20px', background: '#1a237e', color: '#fff', border: 'none', borderRadius: 8, fontSize: 14, fontWeight: 600 },
  btnSecondary: { padding: '9px 16px', background: '#fff', color: '#444', border: '1.5px solid #ddd', borderRadius: 8, fontSize: 14, fontWeight: 500 },
  formCard: { background: '#fff', borderRadius: 10, padding: 24, marginBottom: 24, border: '1px solid #e0e0e0' },
  formTitle: { fontSize: 16, fontWeight: 700, color: '#1a237e', marginBottom: 16 },
  formGrid: { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 },
  fieldGroup: { display: 'flex', flexDirection: 'column', gap: 4 },
  label: { fontSize: 12, fontWeight: 600, color: '#555' },
  input: { padding: '8px 12px', border: '1.5px solid #ddd', borderRadius: 8, fontSize: 14 },
  formActions: { gridColumn: '1 / -1', display: 'flex', gap: 10 },
  error: { gridColumn: '1 / -1', color: '#c62828', background: '#ffebee', padding: '8px 12px', borderRadius: 6, fontSize: 13 },
  success: { gridColumn: '1 / -1', color: '#2e7d32', background: '#e8f5e9', padding: '8px 12px', borderRadius: 6, fontSize: 13 },
  tableWrapper: { background: '#fff', borderRadius: 10, overflow: 'hidden', border: '1px solid #e0e0e0' },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: { background: '#f5f5f5', padding: '12px 16px', textAlign: 'left', fontSize: 12, fontWeight: 700, color: '#555', borderBottom: '1px solid #e0e0e0' },
  td: { padding: '12px 16px', fontSize: 14, borderBottom: '1px solid #f0f0f0', verticalAlign: 'middle' },
  inactiveRow: { opacity: 0.6 },
  statusActive: { background: '#e8f5e9', color: '#2e7d32', padding: '2px 10px', borderRadius: 20, fontSize: 12, fontWeight: 600 },
  statusInactive: { background: '#fce4ec', color: '#c62828', padding: '2px 10px', borderRadius: 20, fontSize: 12, fontWeight: 600 },
  actionBtn: { background: 'none', border: 'none', fontSize: 13, fontWeight: 600, color: '#1a237e', marginRight: 8, cursor: 'pointer', textDecoration: 'underline' },
  policyCard: { background: '#fff', borderRadius: 10, padding: 32, maxWidth: 480, border: '1px solid #e0e0e0' },
  policyForm: { display: 'flex', flexDirection: 'column', gap: 16 },
  checkRow: { display: 'flex', alignItems: 'center', gap: 10 },
  checkLabel: { fontSize: 14, color: '#333' },
}
