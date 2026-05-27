import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'
import { clearSession, getCurrentUser } from '../auth/session.js'
import Logo from '../components/Logo.jsx'

const emptyExam = { title: '', description: '', courseName: 'Testing de Aplicaciones', durationMinutes: 120 }
const emptyQuestion = { prompt: '', modelAnswer: '', points: '1' }

export default function ProfesorLanding() {
  const navigate = useNavigate()
  const user = getCurrentUser() || {}

  const [exams, setExams] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [examForm, setExamForm] = useState(emptyExam)
  const [topicName, setTopicName] = useState('Tema A')
  const [questionForms, setQuestionForms] = useState({})
  const [submissions, setSubmissions] = useState([])
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)

  const selectedExam = useMemo(
    () => exams.find((exam) => exam.id === selectedId) || exams[0] || null,
    [exams, selectedId]
  )

  const fetchExams = useCallback(async () => {
    setLoading(true)
    try {
      const res = await api.get('/exams/mine')
      setExams(res.data)
      if (!selectedId && res.data.length > 0) setSelectedId(res.data[0].id)
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudieron cargar los examenes.')
    } finally {
      setLoading(false)
    }
  }, [selectedId])

  useEffect(() => {
    fetchExams()
  }, [fetchExams])

  useEffect(() => {
    if (!selectedExam || selectedExam.status === 'BORRADOR') {
      setSubmissions([])
      return
    }
    api.get(`/submissions/exams/${selectedExam.id}`)
      .then((res) => setSubmissions(res.data))
      .catch(() => setSubmissions([]))
  }, [selectedExam?.id, selectedExam?.status])

  async function handleLogout() {
    try {
      await api.post('/auth/logout')
    } finally {
      clearSession()
      navigate('/login', { replace: true })
    }
  }

  async function createExam(e) {
    e.preventDefault()
    setMessage('')
    try {
      const res = await api.post('/exams', {
        title: examForm.title,
        description: examForm.description,
        courseName: examForm.courseName,
        durationMinutes: Number(examForm.durationMinutes) || null,
      })
      setExamForm(emptyExam)
      setExams((current) => [res.data, ...current])
      setSelectedId(res.data.id)
      setMessage('Examen creado en borrador.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo crear el examen.')
    }
  }

  async function addTopic(e) {
    e.preventDefault()
    if (!selectedExam) return
    setMessage('')
    try {
      const res = await api.post(`/exams/${selectedExam.id}/topics`, { name: topicName })
      replaceExam(res.data)
      setTopicName(nextTopicName(res.data.topics.length + 1))
      setMessage('Tema agregado.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo agregar el tema.')
    }
  }

  async function addQuestion(e, topicId) {
    e.preventDefault()
    if (!selectedExam) return
    const form = questionForms[topicId] || emptyQuestion
    setMessage('')
    try {
      const res = await api.post(`/exams/${selectedExam.id}/topics/${topicId}/questions`, {
        prompt: form.prompt,
        modelAnswer: form.modelAnswer,
        points: Number(form.points),
      })
      replaceExam(res.data)
      setQuestionForms((current) => ({ ...current, [topicId]: emptyQuestion }))
      setMessage('Pregunta agregada.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo agregar la pregunta.')
    }
  }

  async function removeQuestion(topicId, questionId) {
    if (!selectedExam) return
    setMessage('')
    try {
      const res = await api.delete(`/exams/${selectedExam.id}/topics/${topicId}/questions/${questionId}`)
      replaceExam(res.data)
      setMessage('Pregunta eliminada.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo eliminar la pregunta.')
    }
  }

  async function publishExam() {
    if (!selectedExam) return
    setMessage('')
    try {
      const res = await api.patch(`/exams/${selectedExam.id}/publish`)
      replaceExam(res.data)
      setMessage('Examen publicado. Los alumnos ya pueden iniciarlo.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo publicar el examen.')
    }
  }

  async function closeExam() {
    if (!selectedExam) return
    setMessage('')
    try {
      const res = await api.patch(`/exams/${selectedExam.id}/close`)
      replaceExam(res.data)
      setMessage('Examen cerrado. Ya no se aceptan nuevas entregas.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo cerrar el examen.')
    }
  }

  function replaceExam(updated) {
    setExams((current) => current.map((exam) => exam.id === updated.id ? updated : exam))
    setSelectedId(updated.id)
  }

  function updateQuestionForm(topicId, field, value) {
    setQuestionForms((current) => ({
      ...current,
      [topicId]: { ...(current[topicId] || emptyQuestion), [field]: value },
    }))
  }

  const canEdit = selectedExam?.status === 'BORRADOR'

  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <div style={styles.brand}>
          <Logo dark size={36} />
          <div>
            <h1 style={styles.headerTitle}>Panel de Profesor</h1>
            <span style={styles.headerUser}>{user.fullName || user.email}</span>
          </div>
        </div>
        <button onClick={handleLogout} style={styles.logoutBtn}>Cerrar sesión</button>
      </header>

      <main style={styles.shell}>
        <aside style={styles.sidebar}>
          <form onSubmit={createExam} style={styles.createBox}>
            <h2 style={styles.panelTitle}>Nuevo examen</h2>
            <label style={styles.label}>Titulo</label>
            <input
              value={examForm.title}
              onChange={(e) => setExamForm({ ...examForm, title: e.target.value })}
              style={styles.input}
              placeholder="Primer parcial"
              required
            />
            <label style={styles.label}>Descripcion</label>
            <textarea
              value={examForm.description}
              onChange={(e) => setExamForm({ ...examForm, description: e.target.value })}
              style={styles.textarea}
              rows={3}
              placeholder="Evaluacion de Testing de Aplicaciones"
            />
            <label style={styles.label}>Materia</label>
            <input
              value={examForm.courseName}
              onChange={(e) => setExamForm({ ...examForm, courseName: e.target.value })}
              style={styles.input}
              placeholder="Testing de Aplicaciones"
            />
            <label style={styles.label}>Duracion estimada</label>
            <input
              type="number"
              min="1"
              value={examForm.durationMinutes}
              onChange={(e) => setExamForm({ ...examForm, durationMinutes: e.target.value })}
              style={styles.input}
            />
            <button type="submit" style={styles.primaryBtn}>Crear borrador</button>
          </form>

          <div style={styles.listBox}>
            <h2 style={styles.panelTitle}>Mis examenes</h2>
            {loading && <p style={styles.muted}>Cargando...</p>}
            {exams.length === 0 && !loading && <p style={styles.muted}>Todavia no hay examenes.</p>}
            {exams.map((exam) => (
              <button
                key={exam.id}
                onClick={() => setSelectedId(exam.id)}
                style={exam.id === selectedExam?.id ? styles.examItemActive : styles.examItem}
              >
                <span style={styles.examItemTitle}>{exam.title}</span>
                <span style={statusStyle(exam.status)}>{labelStatus(exam.status)}</span>
              </button>
            ))}
          </div>
        </aside>

        <section style={styles.workspace}>
          {message && <div style={styles.message}>{message}</div>}

          {!selectedExam ? (
            <div style={styles.emptyState}>Crea un examen para empezar a cargar temas y preguntas.</div>
          ) : (
            <>
              <div style={styles.examHeader}>
                <div>
                  <h2 style={styles.examTitle}>{selectedExam.title}</h2>
                  <p style={styles.examMeta}>
                    {selectedExam.courseName || 'Testing de Aplicaciones'} · {selectedExam.description || 'Sin descripcion'} · {selectedExam.durationMinutes || '-'} min
                  </p>
                </div>
                <div style={styles.headerActions}>
                  <span style={statusStyle(selectedExam.status)}>{labelStatus(selectedExam.status)}</span>
                  {canEdit && (
                    <button onClick={publishExam} style={styles.primaryBtn}>Publicar</button>
                  )}
                  {selectedExam.status === 'PUBLICADO' && (
                    <button onClick={closeExam} style={styles.closeBtn}>Cerrar examen</button>
                  )}
                </div>
              </div>

              {canEdit && (
                <form onSubmit={addTopic} style={styles.topicForm}>
                  <input
                    value={topicName}
                    onChange={(e) => setTopicName(e.target.value)}
                    style={styles.input}
                    placeholder="Tema A"
                    required
                  />
                  <button type="submit" style={styles.secondaryBtn}>Agregar tema</button>
                </form>
              )}

              {!canEdit && (
                <section style={styles.submissionPanel}>
                  <div style={styles.submissionHeader}>
                    <h3 style={styles.submissionTitle}>Entregas de alumnos</h3>
                    <button onClick={() => {
                      api.get(`/submissions/exams/${selectedExam.id}`).then((res) => setSubmissions(res.data)).catch(() => setSubmissions([]))
                    }} style={styles.secondaryBtn}>Actualizar</button>
                  </div>
                  {submissions.length === 0 ? (
                    <p style={styles.muted}>Todavia no hay alumnos que hayan iniciado este examen.</p>
                  ) : (
                    <div style={styles.submissionList}>
                      {submissions.map((submission) => (
                        <div key={submission.id} style={styles.submissionRow}>
                          <div>
                            <strong>{submission.studentName}</strong>
                            <p style={styles.answer}>Tema: {submission.topicName}</p>
                          </div>
                          <span style={submission.status === 'ENTREGADO' ? styles.submittedBadge : styles.progressBadge}>
                            {submission.status === 'ENTREGADO' ? 'Entregado' : 'En progreso'}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </section>
              )}

              <div style={styles.topicGrid}>
                {selectedExam.topics?.length === 0 && (
                  <div style={styles.emptyState}>Agrega al menos un tema. Para publicar, cada tema debe sumar 10 puntos.</div>
                )}

                {selectedExam.topics?.map((topic) => {
                  const totalOk = Number(topic.totalPoints) === 10
                  const form = questionForms[topic.id] || emptyQuestion
                  return (
                    <article key={topic.id} style={{ ...styles.topicCard, borderTop: `4px solid ${topic.colorHex || '#1956D8'}` }}>
                      <div style={styles.topicHeader}>
                        <div>
                          <div style={styles.topicTitleRow}>
                            <span style={{ ...styles.topicSwatch, background: topic.colorHex || '#1956D8' }} />
                            <h3 style={styles.topicTitle}>{topic.name}</h3>
                          </div>
                          <span style={totalOk ? styles.totalOk : styles.totalPending}>
                            Total: {topic.totalPoints} / 10
                          </span>
                        </div>
                      </div>

                      <div style={styles.questions}>
                        {topic.questions.length === 0 && <p style={styles.muted}>Sin preguntas cargadas.</p>}
                        {topic.questions.map((question) => (
                          <div key={question.id} style={styles.questionRow}>
                            <div>
                              <strong>{question.displayOrder}. {question.prompt}</strong>
                              <p style={styles.answer}>Modelo: {question.modelAnswer}</p>
                            </div>
                            <div style={styles.questionActions}>
                              <span style={styles.points}>{question.points} pts</span>
                              {canEdit && (
                                <button onClick={() => removeQuestion(topic.id, question.id)} style={styles.linkBtn}>
                                  Eliminar
                                </button>
                              )}
                            </div>
                          </div>
                        ))}
                      </div>

                      {canEdit && (
                        <form onSubmit={(e) => addQuestion(e, topic.id)} style={styles.questionForm}>
                          <label style={styles.label}>Enunciado</label>
                          <textarea
                            value={form.prompt}
                            onChange={(e) => updateQuestionForm(topic.id, 'prompt', e.target.value)}
                            style={styles.textarea}
                            rows={2}
                            required
                          />
                          <label style={styles.label}>Respuesta modelo</label>
                          <textarea
                            value={form.modelAnswer}
                            onChange={(e) => updateQuestionForm(topic.id, 'modelAnswer', e.target.value)}
                            style={styles.textarea}
                            rows={2}
                            required
                          />
                          <div style={styles.inlineFields}>
                            <div>
                              <label style={styles.label}>Puntos</label>
                              <input
                                type="number"
                                step="0.25"
                                min="0.25"
                                max="10"
                                value={form.points}
                                onChange={(e) => updateQuestionForm(topic.id, 'points', e.target.value)}
                                style={styles.smallInput}
                                required
                              />
                            </div>
                            <button type="submit" style={styles.secondaryBtn}>Agregar pregunta</button>
                          </div>
                        </form>
                      )}
                    </article>
                  )
                })}
              </div>
            </>
          )}
        </section>
      </main>
    </div>
  )
}

function labelStatus(status) {
  return {
    BORRADOR: 'Borrador',
    PUBLICADO: 'Publicado',
    CERRADO: 'Cerrado',
  }[status] || status
}

function statusStyle(status) {
  const base = {
    display: 'inline-flex',
    alignItems: 'center',
    height: 24,
    padding: '0 10px',
    borderRadius: 999,
    fontSize: 12,
    fontWeight: 700,
  }
  if (status === 'PUBLICADO') return { ...base, background: '#DDF6EC', color: '#087A55' }
  if (status === 'CERRADO') return { ...base, background: '#ECEFF3', color: '#4A5565' }
  return { ...base, background: '#E6EEFF', color: '#1956D8' }
}

function nextTopicName(count) {
  const letter = String.fromCharCode(64 + Math.min(count, 26))
  return `Tema ${letter}`
}

const styles = {
  page: { minHeight: '100vh', background: '#F4F8FA', color: '#09222A' },
  header: { background: '#09222A', color: '#fff', padding: '16px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  brand: { display: 'flex', alignItems: 'center', gap: 12 },
  headerTitle: { fontSize: 20, fontWeight: 700, margin: 0 },
  headerUser: { fontSize: 13, opacity: 0.72 },
  logoutBtn: { padding: '8px 18px', background: 'rgba(203,238,243,0.1)', border: '1px solid rgba(203,238,243,0.4)', color: '#CBEEF3', borderRadius: 8, fontWeight: 600, fontSize: 13, cursor: 'pointer' },
  shell: { display: 'grid', gridTemplateColumns: '320px 1fr', gap: 24, padding: 24, maxWidth: 1360, margin: '0 auto' },
  sidebar: { display: 'flex', flexDirection: 'column', gap: 16 },
  createBox: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 18, display: 'flex', flexDirection: 'column', gap: 8 },
  listBox: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 14 },
  panelTitle: { fontSize: 15, fontWeight: 800, color: '#1956D8', margin: '0 0 10px' },
  label: { fontSize: 12, fontWeight: 700, color: '#304653' },
  input: { minHeight: 38, padding: '8px 10px', border: '1px solid #C9DDE3', borderRadius: 6, fontSize: 14, color: '#09222A', boxSizing: 'border-box', width: '100%' },
  smallInput: { minHeight: 38, width: 100, padding: '8px 10px', border: '1px solid #C9DDE3', borderRadius: 6, fontSize: 14, color: '#09222A', boxSizing: 'border-box' },
  textarea: { padding: '8px 10px', border: '1px solid #C9DDE3', borderRadius: 6, fontSize: 14, color: '#09222A', resize: 'vertical', boxSizing: 'border-box', width: '100%', fontFamily: 'inherit' },
  primaryBtn: { minHeight: 38, padding: '8px 16px', background: '#1956D8', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  secondaryBtn: { minHeight: 38, padding: '8px 14px', background: '#fff', color: '#1956D8', border: '1px solid #1956D8', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  disabledBtn: { minHeight: 38, padding: '8px 16px', background: '#C9DDE3', color: '#536B76', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 700 },
  closeBtn: { minHeight: 38, padding: '8px 16px', background: '#fff', color: '#9B2C2C', border: '1px solid #9B2C2C', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  examItem: { width: '100%', border: 'none', borderBottom: '1px solid #E7F0F3', background: '#fff', padding: '12px 4px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer', textAlign: 'left' },
  examItemActive: { width: '100%', border: 'none', borderBottom: '1px solid #E7F0F3', background: '#F0F5FF', padding: '12px 8px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer', textAlign: 'left', borderRadius: 6 },
  examItemTitle: { fontSize: 14, fontWeight: 700, color: '#09222A', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 175 },
  workspace: { minWidth: 0 },
  message: { background: '#FFF8DF', border: '1px solid #E7CE74', color: '#5D4700', padding: '10px 12px', borderRadius: 8, marginBottom: 14, fontSize: 14 },
  emptyState: { background: '#fff', border: '1px dashed #B9CDD3', borderRadius: 8, padding: 24, color: '#536B76', textAlign: 'center' },
  examHeader: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16, marginBottom: 16 },
  examTitle: { fontSize: 24, fontWeight: 800, margin: '0 0 6px' },
  examMeta: { fontSize: 14, color: '#536B76', margin: 0 },
  headerActions: { display: 'flex', alignItems: 'center', gap: 10 },
  topicForm: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 14, display: 'grid', gridTemplateColumns: '1fr auto', gap: 10, marginBottom: 16 },
  topicGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: 16 },
  topicCard: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 16 },
  topicHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 },
  topicTitleRow: { display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 },
  topicSwatch: { width: 12, height: 12, borderRadius: 999, flex: '0 0 auto' },
  topicTitle: { fontSize: 18, margin: 0 },
  totalOk: { color: '#087A55', fontSize: 13, fontWeight: 800 },
  totalPending: { color: '#9B6A00', fontSize: 13, fontWeight: 800 },
  questions: { display: 'flex', flexDirection: 'column', gap: 10 },
  questionRow: { border: '1px solid #E7F0F3', borderRadius: 6, padding: 10, display: 'grid', gridTemplateColumns: '1fr auto', gap: 12 },
  answer: { margin: '6px 0 0', color: '#536B76', fontSize: 13, lineHeight: 1.4 },
  questionActions: { display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 6 },
  points: { color: '#1956D8', fontWeight: 800, fontSize: 13 },
  linkBtn: { background: 'none', border: 'none', color: '#9B2C2C', fontWeight: 700, cursor: 'pointer', fontSize: 13 },
  questionForm: { marginTop: 14, borderTop: '1px solid #E7F0F3', paddingTop: 14, display: 'flex', flexDirection: 'column', gap: 8 },
  inlineFields: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', gap: 12 },
  muted: { color: '#536B76', fontSize: 14, margin: 0 },
  submissionPanel: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 16, marginBottom: 16 },
  submissionHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, marginBottom: 12 },
  submissionTitle: { fontSize: 17, margin: 0 },
  submissionList: { display: 'flex', flexDirection: 'column', gap: 8 },
  submissionRow: { border: '1px solid #E7F0F3', borderRadius: 6, padding: 10, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 },
  submittedBadge: { background: '#DDF6EC', color: '#087A55', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
  progressBadge: { background: '#E6EEFF', color: '#1956D8', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
}
