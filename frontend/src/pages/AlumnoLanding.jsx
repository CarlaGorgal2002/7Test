import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'
import { clearSession, getCurrentUser } from '../auth/session.js'
import AutoGrowTextarea from '../components/AutoGrowTextarea.jsx'
import DecisionTableEditor, { emptyDecisionTableValue, isDecisionTablePrompt } from '../components/DecisionTableEditor.jsx'
import DecisionTreeEditor, { emptyDecisionTreeValue, isDecisionTreePrompt } from '../components/DecisionTreeEditor.jsx'
import Logo from '../components/Logo.jsx'

export default function AlumnoLanding() {
  const navigate = useNavigate()
  const user = getCurrentUser() || {}
  const [exams, setExams] = useState([])
  const [submissions, setSubmissions] = useState([])
  const [current, setCurrent] = useState(null)
  const [answers, setAnswers] = useState({})
  const [dirty, setDirty] = useState(false)
  const [saving, setSaving] = useState('')
  const [message, setMessage] = useState('')

  const submissionsByExam = useMemo(() => {
    const map = {}
    submissions.forEach((submission) => { map[submission.examId] = submission })
    return map
  }, [submissions])

  const fetchData = useCallback(async () => {
    try {
      const [published, mine] = await Promise.all([
        api.get('/exams/published'),
        api.get('/submissions/mine'),
      ])
      setExams(published.data)
      setSubmissions(mine.data)
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudieron cargar los examenes.')
    }
  }, [])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  useEffect(() => {
    if (!current) return
    setAnswers(Object.fromEntries(current.questions.map((question) => [question.questionId, question.answerText || ''])))
    setDirty(false)
  }, [current?.id])

  useEffect(() => {
    if (!dirty || !current || current.status !== 'EN_PROGRESO') return
    const timer = setTimeout(() => saveAnswers(false), 900)
    return () => clearTimeout(timer)
  }, [answers, dirty, current?.id, current?.status])

  async function handleLogout() {
    try {
      await api.post('/auth/logout')
    } finally {
      clearSession()
      navigate('/login', { replace: true })
    }
  }

  async function startExam(examId) {
    setMessage('')
    try {
      const res = await api.post(`/submissions/exams/${examId}/start`)
      setCurrent(res.data)
      upsertSubmission(res.data)
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo iniciar el examen.')
    }
  }

  async function openSubmission(submission) {
    setCurrent(submission)
  }

  async function saveAnswers(showMessage = true) {
    if (!current || current.status !== 'EN_PROGRESO') return
    setSaving('Guardando...')
    try {
      const payload = {
        answers: current.questions.map((question) => ({
          questionId: question.questionId,
          answerText: answers[question.questionId] || '',
        })),
      }
      const res = await api.put(`/submissions/${current.id}/answers`, payload)
      setCurrent(res.data)
      upsertSubmission(res.data)
      setDirty(false)
      setSaving(showMessage ? 'Guardado' : 'Guardado automatico')
      setTimeout(() => setSaving(''), 1500)
    } catch (err) {
      setSaving('')
      setMessage(err.response?.data?.message || 'No se pudieron guardar las respuestas.')
    }
  }

  async function submitExam() {
    if (!current) return
    await saveAnswers(false)
    setMessage('')
    try {
      const res = await api.patch(`/submissions/${current.id}/submit`)
      setCurrent(res.data)
      upsertSubmission(res.data)
      setDirty(false)
      setMessage('Examen entregado correctamente.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo entregar el examen.')
    }
  }

  function upsertSubmission(updated) {
    setSubmissions((items) => {
      const exists = items.some((item) => item.id === updated.id)
      if (exists) return items.map((item) => item.id === updated.id ? updated : item)
      return [updated, ...items]
    })
  }

  function updateAnswer(questionId, value) {
    setAnswers((currentAnswers) => ({ ...currentAnswers, [questionId]: value }))
    setDirty(true)
  }

  const canAnswer = current?.status === 'EN_PROGRESO'

  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <div style={styles.brand}>
          <Logo dark size={36} />
          <div>
            <h1 style={styles.headerTitle}>Panel de Alumno</h1>
            <span style={styles.headerUser}>{user.fullName || user.email}</span>
          </div>
        </div>
        <button onClick={handleLogout} style={styles.logoutBtn}>Cerrar sesión</button>
      </header>

      <main style={styles.shell}>
        <aside style={styles.sidebar}>
          <h2 style={styles.panelTitle}>Examenes publicados</h2>
          {exams.length === 0 ? (
            <p style={styles.muted}>No hay examenes publicados.</p>
          ) : exams.map((exam) => {
            const submission = submissionsByExam[exam.id]
            return (
              <article key={exam.id} style={styles.examCard}>
                <div>
                  <h3 style={styles.examTitle}>{exam.title}</h3>
                  <p style={styles.muted}>{exam.courseName || 'Testing de Aplicaciones'} · {exam.durationMinutes || '-'} min · {exam.topics?.length || 0} tema(s)</p>
                </div>
                {submission ? (
                  <button onClick={() => openSubmission(submission)} style={styles.secondaryBtn}>
                    {submission.status === 'ENTREGADO' ? 'Ver entrega' : 'Continuar'}
                  </button>
                ) : (
                  <button onClick={() => startExam(exam.id)} style={styles.primaryBtn}>Iniciar</button>
                )}
              </article>
            )
          })}
        </aside>

        <section style={styles.workspace}>
          {message && <div style={styles.message}>{message}</div>}
          {!current ? (
            <div style={styles.empty}>Selecciona un examen para comenzar o continuar tu entrega.</div>
          ) : (
            <>
              <div style={styles.examHeader}>
                <div>
                  <h2 style={styles.currentTitle}>{current.examTitle}</h2>
                  <p style={styles.muted}>Tema asignado: <strong>{current.topicName}</strong></p>
                </div>
                <div style={styles.headerActions}>
                  <span style={current.status === 'ENTREGADO' ? styles.doneBadge : styles.progressBadge}>
                    {current.status === 'ENTREGADO' ? 'Entregado' : 'En progreso'}
                  </span>
                  {saving && <span style={styles.saving}>{saving}</span>}
                </div>
              </div>

              <div style={styles.questions}>
                {current.questions.map((question) => {
                  const treeQuestion = isDecisionTreeQuestion(question)
                  const tableQuestion = isDecisionTableQuestion(question)
                  return (
                    <article key={question.questionId} style={styles.questionCard}>
                      <div style={styles.questionHeader}>
                        <h3 style={styles.questionTitle}>{question.displayOrder}. {question.prompt || questionFallbackTitle(question)}</h3>
                        <span style={styles.points}>{question.points} pts</span>
                      </div>
                      {tableQuestion ? (
                        <DecisionTableEditor
                          value={answers[question.questionId] || emptyDecisionTableValue()}
                          onChange={(value) => updateAnswer(question.questionId, value)}
                          readOnly={!canAnswer}
                        />
                      ) : treeQuestion ? (
                        <DecisionTreeEditor
                          value={answers[question.questionId] || emptyDecisionTreeValue()}
                          onChange={(value) => updateAnswer(question.questionId, value)}
                          readOnly={!canAnswer}
                        />
                      ) : (
                        <AutoGrowTextarea
                          value={answers[question.questionId] || ''}
                          onChange={(e) => updateAnswer(question.questionId, e.target.value)}
                          disabled={!canAnswer}
                          style={canAnswer ? styles.answerBox : styles.answerBoxDisabled}
                          placeholder="Escribi tu respuesta..."
                          minHeight={320}
                          maxHeight={1400}
                        />
                      )}
                    </article>
                  )
                })}
              </div>

              <div style={styles.footerActions}>
                <button onClick={() => saveAnswers(true)} disabled={!canAnswer} style={canAnswer ? styles.secondaryBtn : styles.disabledBtn}>
                  Guardar ahora
                </button>
                <button onClick={submitExam} disabled={!canAnswer} style={canAnswer ? styles.primaryBtn : styles.disabledBtn}>
                  Entregar examen
                </button>
              </div>
            </>
          )}
        </section>
      </main>
    </div>
  )
}

function isDecisionTreeQuestion(question = {}) {
  return question.interactionType === 'DECISION_TREE' || isDecisionTreePrompt(question.prompt)
}

function isDecisionTableQuestion(question = {}) {
  return question.interactionType === 'DECISION_TABLE'
    || isDecisionTablePrompt(question.prompt)
    || (Number(question.points) === 2 && question.displayOrder === 7 && !isDecisionTreeQuestion(question))
}

function questionFallbackTitle(question) {
  if (isDecisionTableQuestion(question)) return 'Practico - Tabla de decision'
  if (isDecisionTreeQuestion(question)) return 'Practico - Arbol de decision'
  if (Number(question.points) === 1 && question.displayOrder <= 6) return `Teorica ${question.displayOrder}`
  return 'Pregunta sin enunciado'
}

const styles = {
  page: { minHeight: '100vh', background: '#F4F8FA', color: '#09222A' },
  header: { background: '#09222A', color: '#fff', padding: '16px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
  brand: { display: 'flex', alignItems: 'center', gap: 12 },
  headerTitle: { fontSize: 20, fontWeight: 700, margin: 0 },
  headerUser: { fontSize: 13, opacity: 0.72 },
  logoutBtn: { padding: '8px 18px', background: 'rgba(203,238,243,0.1)', border: '1px solid rgba(203,238,243,0.4)', color: '#CBEEF3', borderRadius: 8, fontWeight: 600, fontSize: 13, cursor: 'pointer' },
  shell: { display: 'grid', gridTemplateColumns: '340px 1fr', gap: 24, padding: 24, maxWidth: 1360, margin: '0 auto' },
  sidebar: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 16, alignSelf: 'start' },
  panelTitle: { fontSize: 16, fontWeight: 800, color: '#1956D8', margin: '0 0 12px' },
  examCard: { border: '1px solid #E7F0F3', borderRadius: 8, padding: 12, marginBottom: 10, display: 'flex', justifyContent: 'space-between', gap: 12, alignItems: 'center' },
  examTitle: { fontSize: 15, margin: '0 0 5px' },
  muted: { color: '#536B76', fontSize: 14, margin: 0 },
  primaryBtn: { minHeight: 38, padding: '8px 16px', background: '#1956D8', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  secondaryBtn: { minHeight: 38, padding: '8px 14px', background: '#fff', color: '#1956D8', border: '1px solid #1956D8', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  disabledBtn: { minHeight: 38, padding: '8px 16px', background: '#C9DDE3', color: '#536B76', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 700 },
  workspace: { minWidth: 0 },
  message: { background: '#FFF8DF', border: '1px solid #E7CE74', color: '#5D4700', padding: '10px 12px', borderRadius: 8, marginBottom: 14, fontSize: 14 },
  empty: { background: '#fff', border: '1px dashed #B9CDD3', borderRadius: 8, padding: 24, color: '#536B76', textAlign: 'center' },
  examHeader: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 20, display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', marginBottom: 16 },
  currentTitle: { fontSize: 24, margin: '0 0 6px' },
  headerActions: { display: 'flex', alignItems: 'center', gap: 10 },
  progressBadge: { background: '#E6EEFF', color: '#1956D8', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
  doneBadge: { background: '#DDF6EC', color: '#087A55', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
  saving: { color: '#536B76', fontSize: 13, fontWeight: 700 },
  questions: { display: 'flex', flexDirection: 'column', gap: 14 },
  questionCard: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 16 },
  questionHeader: { display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 10 },
  questionTitle: { fontSize: 17, margin: 0, lineHeight: 1.35 },
  points: { color: '#1956D8', fontWeight: 800, whiteSpace: 'nowrap' },
  templateBox: { display: 'flex', justifyContent: 'flex-end', marginBottom: 10 },
  answerBox: { width: '100%', minHeight: 300, boxSizing: 'border-box', border: '1px solid #C9DDE3', borderRadius: 6, padding: 12, fontSize: 15, lineHeight: 1.5, fontFamily: 'inherit', color: '#09222A', resize: 'vertical' },
  answerBoxDisabled: { width: '100%', minHeight: 300, boxSizing: 'border-box', border: '1px solid #D8E8EC', borderRadius: 6, padding: 12, fontSize: 15, lineHeight: 1.5, fontFamily: 'inherit', color: '#536B76', background: '#F4F8FA', resize: 'vertical' },
  footerActions: { marginTop: 16, display: 'flex', justifyContent: 'flex-end', gap: 10 },
}
