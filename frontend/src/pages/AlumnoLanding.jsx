import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'
import { clearSession, getCurrentUser } from '../auth/session.js'
import AutoGrowTextarea from '../components/AutoGrowTextarea.jsx'
import DecisionTableEditor, { emptyDecisionTableValue, isDecisionTablePrompt } from '../components/DecisionTableEditor.jsx'
import DecisionTreeEditor, { emptyDecisionTreeValue, isDecisionTreePrompt } from '../components/DecisionTreeEditor.jsx'
import Logo from '../components/Logo.jsx'

const TOPIC_PASTEL = {
  '#2563eb': '#BFDBFE',
  '#16a34a': '#BBF7D0',
  '#d97706': '#FDE68A',
  '#dc2626': '#FECACA',
  '#7c3aed': '#DDD6FE',
  '#ea580c': '#FDBA74',
  '#0891b2': '#A5F3FC',
  '#65a30d': '#D9F99D',
  '#db2777': '#FBCFE8',
  '#0d9488': '#99F6E4',
}

function topicPastelColor(exams, examId, topicId) {
  const exam = exams.find(e => e.id === examId)
  const topic = exam?.topics?.find(t => t.id === topicId)
  const hex = topic?.colorHex?.toLowerCase()
  return (hex && TOPIC_PASTEL[hex]) || '#F4F8FA'
}

export default function AlumnoLanding() {
  const navigate = useNavigate()
  const user = getCurrentUser() || {}

  const [exams, setExams] = useState([])
  const [submissions, setSubmissions] = useState([])
  // null = dashboard, { exam } = selección de tema, { submission } = resolviendo
  const [view, setView] = useState({ type: 'dashboard' })
  const [selectedTopicId, setSelectedTopicId] = useState(null)
  const [answers, setAnswers] = useState({})
  const [dirty, setDirty] = useState(false)
  const [saving, setSaving] = useState('')
  const [message, setMessage] = useState('')
  const [confirmSubmit, setConfirmSubmit] = useState(false)
  const [timeLeft, setTimeLeft] = useState(null)
  const timerRef = useRef(null)

  const current = view.type === 'exam' ? view.submission : null

  const submissionsByExam = useMemo(() => {
    const map = {}
    submissions.forEach((s) => { map[s.examId] = s })
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

  useEffect(() => { fetchData() }, [fetchData])

  // Inicializar respuestas al abrir entrega
  useEffect(() => {
    if (!current) return
    setAnswers(Object.fromEntries(current.questions.map((q) => [q.questionId, q.answerText || ''])))
    setDirty(false)
  }, [current?.id])

  // Autoguardado
  useEffect(() => {
    if (!dirty || !current || current.status !== 'EN_PROGRESO') return
    const t = setTimeout(() => saveAnswers(false), 900)
    return () => clearTimeout(t)
  }, [answers, dirty, current?.id, current?.status])

  // Timer: arranca cuando hay una entrega en progreso con duración y publishedAt del examen
  useEffect(() => {
    clearInterval(timerRef.current)
    if (!current || current.status !== 'EN_PROGRESO') { setTimeLeft(null); return }
    const exam = exams.find((e) => e.id === current.examId)
    if (!exam?.publishedAt || !exam?.durationMinutes) { setTimeLeft(null); return }

    const endMs = new Date(exam.publishedAt).getTime() + exam.durationMinutes * 60_000

    function tick() {
      const remaining = Math.floor((endMs - Date.now()) / 1000)
      setTimeLeft(remaining)
    }
    tick()
    timerRef.current = setInterval(tick, 1000)
    return () => clearInterval(timerRef.current)
  }, [current?.id, current?.status, exams])

  async function handleLogout() {
    try { await api.post('/auth/logout') } finally {
      clearSession()
      navigate('/login', { replace: true })
    }
  }

  function openTopicSelection(exam) {
    const existing = submissionsByExam[exam.id]
    if (existing) {
      setView({ type: 'exam', submission: existing })
      return
    }
    setSelectedTopicId(null)
    setMessage('')
    setView({ type: 'topicSelect', exam })
  }

  async function startExam() {
    const exam = view.exam
    if (!exam || !selectedTopicId) return
    setMessage('')
    try {
      const res = await api.post(`/submissions/exams/${exam.id}/start`, { topicId: selectedTopicId })
      upsertSubmission(res.data)
      setView({ type: 'exam', submission: res.data })
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo iniciar el examen.')
    }
  }

  async function saveAnswers(showMessage = true) {
    if (!current || current.status !== 'EN_PROGRESO') return
    setSaving('Guardando...')
    try {
      const payload = {
        answers: current.questions.map((q) => ({
          questionId: q.questionId,
          answerText: answers[q.questionId] || '',
        })),
      }
      const res = await api.put(`/submissions/${current.id}/answers`, payload)
      setView({ type: 'exam', submission: res.data })
      upsertSubmission(res.data)
      setDirty(false)
      setSaving(showMessage ? 'Guardado' : 'Guardado automático')
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
      upsertSubmission(res.data)
      setDirty(false)
      setConfirmSubmit(false)
      setView({ type: 'submitted', submission: res.data })
    } catch (err) {
      setConfirmSubmit(false)
      setMessage(err.response?.data?.message || 'No se pudo entregar el examen.')
    }
  }

  function upsertSubmission(updated) {
    setSubmissions((items) => {
      const exists = items.some((s) => s.id === updated.id)
      if (exists) return items.map((s) => s.id === updated.id ? updated : s)
      return [updated, ...items]
    })
  }

  function updateAnswer(questionId, value) {
    setAnswers((a) => ({ ...a, [questionId]: value }))
    setDirty(true)
  }

  // Ordena: EN_PROGRESO/disponible primero, ENTREGADO/CERRADO después
  const sortedExams = useMemo(() => {
    return [...exams].sort((a, b) => {
      const sub_a = submissionsByExam[a.id]
      const sub_b = submissionsByExam[b.id]
      const weight = (exam, sub) => {
        if (sub?.status === 'EN_PROGRESO') return 0
        if (!sub && exam.status === 'PUBLICADO') return 1
        return 2
      }
      return weight(a, sub_a) - weight(b, sub_b)
    })
  }, [exams, submissionsByExam])

  const canAnswer = current?.status === 'EN_PROGRESO' && !timedOut

  // Polling cada 15s cuando el timer vence — detecta tiempo extra o cierre del examen
  useEffect(() => {
    if (!timedOut || !current) return
    const interval = setInterval(fetchData, 15000)
    return () => clearInterval(interval)
  }, [timedOut, current?.id, fetchData])
  const timedOut = timeLeft !== null && timeLeft <= 0

  // ── VISTA: SELECCIÓN DE TEMA ──────────────────────────────────────────────
  if (view.type === 'topicSelect') {
    const exam = view.exam
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

        <main style={styles.topicSelectMain}>
          <div style={styles.topicSelectCard}>
            <button onClick={() => setView({ type: 'dashboard' })} style={styles.backBtn}>← Volver</button>
            <h2 style={styles.topicSelectTitle}>{exam.title}</h2>
            <p style={styles.topicSelectMeta}>{exam.courseName || 'Testing de Aplicaciones'} · {exam.durationMinutes || '-'} min</p>
            <p style={styles.topicSelectInstr}>Seleccioná el tema que te indicó el docente:</p>
            {message && <div style={styles.message}>{message}</div>}
            <div style={styles.topicBtns}>
              {(exam.topics || []).map((topic) => (
                <button
                  key={topic.id}
                  onClick={() => setSelectedTopicId(topic.id)}
                  style={{
                    ...styles.topicBtn,
                    borderColor: selectedTopicId === topic.id ? (topic.colorHex || '#1956D8') : '#D8E8EC',
                    background: selectedTopicId === topic.id ? (topic.colorHex || '#1956D8') : '#fff',
                    color: selectedTopicId === topic.id ? '#fff' : '#09222A',
                  }}
                >
                  {topic.name}
                </button>
              ))}
            </div>
            <button
              onClick={startExam}
              disabled={!selectedTopicId}
              style={selectedTopicId ? styles.primaryBtn : styles.disabledBtn}
            >
              Comenzar examen
            </button>
          </div>
        </main>
      </div>
    )
  }

  // ── VISTA: ENTREGA EXITOSA ────────────────────────────────────────────────
  if (view.type === 'submitted') {
    const sub = view.submission
    return (
      <div style={styles.page}>
        <header style={styles.header}>
          <div style={styles.brand}><Logo dark size={36} /><h1 style={styles.headerTitle}>Panel de Alumno</h1></div>
          <button onClick={handleLogout} style={styles.logoutBtn}>Cerrar sesión</button>
        </header>
        <main style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', minHeight: 'calc(100vh - 68px)', padding: 24 }}>
          <div style={{ background: '#fff', border: '1px solid #D8E8EC', borderRadius: 12, padding: '40px 48px', maxWidth: 480, textAlign: 'center' }}>
            <div style={{ fontSize: 56, marginBottom: 16 }}>✅</div>
            <h2 style={{ fontSize: 24, fontWeight: 800, margin: '0 0 8px' }}>¡Examen entregado!</h2>
            <p style={{ color: '#536B76', marginBottom: 24 }}>{sub?.examTitle} · Tema: {sub?.topicName}</p>
            <div style={{ display: 'flex', gap: 12, justifyContent: 'center', flexWrap: 'wrap' }}>
              <button onClick={() => setView({ type: 'dashboard' })} style={styles.primaryBtn}>Ir al inicio</button>
              <button onClick={() => setView({ type: 'exam', submission: sub })} style={styles.secondaryBtn}>Ver mis respuestas</button>
            </div>
          </div>
        </main>
      </div>
    )
  }

  // ── VISTA: RESOLUCIÓN ─────────────────────────────────────────────────────
  if (view.type === 'exam' && current) {
    const bgColor = topicPastelColor(exams, current.examId, current.topicId)
    const topicColor = exams.find(e => e.id === current.examId)?.topics?.find(t => t.id === current.topicId)?.colorHex || '#1956D8'
    return (
      <div style={{ ...styles.page, background: bgColor }}>
        <header style={styles.header}>
          <div style={styles.brand}>
            <Logo dark size={36} />
            <div>
              <h1 style={styles.headerTitle}>Panel de Alumno</h1>
              <span style={styles.headerUser}>{user.fullName || user.email}</span>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            {timeLeft !== null && (
              <span style={timedOut ? styles.timerExpired : styles.timer}>
                {timedOut ? '⏰ Tiempo vencido' : `⏱ ${formatTime(timeLeft)}`}
              </span>
            )}
            <button onClick={handleLogout} style={styles.logoutBtn}>Cerrar sesión</button>
          </div>
        </header>

        <main style={{ ...styles.shell, gridTemplateColumns: '1fr' }}>
          <section style={styles.workspace}>
            {message && <div style={styles.message}>{message}</div>}
            <div style={styles.examHeader}>
              <div>
                <h2 style={styles.currentTitle}>{current.examTitle}</h2>
                <p style={styles.muted}>Tema: <strong>{current.topicName}</strong></p>
              </div>
              <div style={styles.headerActions}>
                <span style={current.status === 'ENTREGADO' ? styles.doneBadge : styles.progressBadge}>
                  {current.status === 'ENTREGADO' ? 'Entregado' : 'En progreso'}
                </span>
                {saving && <span style={styles.saving}>{saving}</span>}
                {canAnswer && <button onClick={() => setView({ type: 'dashboard' })} style={styles.secondaryBtn}>← Dashboard</button>}
              </div>
            </div>

            {timedOut && current.status === 'EN_PROGRESO' && (
              <div style={styles.waitingBanner}>
                ⏸ Tiempo finalizado — aguardá la decisión del profesor. Tus respuestas están guardadas.
              </div>
            )}

            <div style={styles.questions}>
              {current.questions.map((question) => {
                const treeQuestion = isDecisionTreeQuestion(question)
                const tableQuestion = isDecisionTableQuestion(question)
                return (
                  <article key={question.questionId} style={{ ...styles.questionCard, borderLeft: `4px solid ${topicColor}` }}>
                    {current.feedbackPublished && question.score != null && (
                      <div style={styles.feedbackBox}>
                        <span style={styles.feedbackScore}>{question.score} / {question.points} pts</span>
                        {question.comment && <p style={styles.feedbackComment}>💬 {question.comment}</p>}
                      </div>
                    )}
                    <div style={styles.questionHeader}>
                      <h3 style={styles.questionTitle}>{question.displayOrder}. {question.prompt || questionFallbackTitle(question)}</h3>
                      <span style={styles.points}>{question.points} pts</span>
                    </div>
                    {tableQuestion ? (
                      <div style={styles.practicalAnswerContainer}>
                        <DecisionTableEditor
                          value={answers[question.questionId] || emptyDecisionTableValue()}
                          onChange={(value) => updateAnswer(question.questionId, value)}
                          readOnly={!canAnswer}
                          compact
                        />
                      </div>
                    ) : treeQuestion ? (
                      <div style={styles.practicalAnswerContainer}>
                        <DecisionTreeEditor
                          value={answers[question.questionId] || emptyDecisionTreeValue()}
                          onChange={(value) => updateAnswer(question.questionId, value)}
                          readOnly={!canAnswer}
                          compact
                        />
                      </div>
                    ) : (
                      <AutoGrowTextarea
                        value={answers[question.questionId] || ''}
                        onChange={(e) => updateAnswer(question.questionId, e.target.value)}
                        disabled={!canAnswer}
                        style={canAnswer ? styles.answerBox : styles.answerBoxDisabled}
                        placeholder="Escribí tu respuesta..."
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
              <button onClick={() => setConfirmSubmit(true)} disabled={!canAnswer} style={canAnswer ? styles.primaryBtn : styles.disabledBtn}>
                Entregar examen
              </button>
            </div>
          </section>
        </main>

        {/* Modal de confirmación de entrega */}
        {confirmSubmit && (
          <div style={styles.modalOverlay} onClick={() => setConfirmSubmit(false)}>
            <div style={styles.modalBox} onClick={(e) => e.stopPropagation()}>
              <h3 style={styles.modalTitle}>Entregar examen</h3>
              <p style={styles.modalText}>
                Una vez que entregues, <strong>no vas a poder modificar tus respuestas</strong>. ¿Confirmás la entrega?
              </p>
              <div style={styles.modalActions}>
                <button onClick={() => setConfirmSubmit(false)} style={styles.secondaryBtn}>Cancelar</button>
                <button onClick={submitExam} style={styles.primaryBtn}>Sí, entregar</button>
              </div>
            </div>
          </div>
        )}
      </div>
    )
  }

  // ── VISTA: DASHBOARD ──────────────────────────────────────────────────────
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
          <h2 style={styles.panelTitle}>Mis exámenes</h2>
          {message && <div style={styles.message}>{message}</div>}
          {sortedExams.length === 0 ? (
            <p style={styles.muted}>No hay exámenes publicados.</p>
          ) : sortedExams.map((exam) => {
            const submission = submissionsByExam[exam.id]
            const isClosed = exam.status === 'CERRADO'
            const isDelivered = submission?.status === 'ENTREGADO'
            const isInProgress = submission?.status === 'EN_PROGRESO'
            return (
              <article key={exam.id} style={styles.examCard}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <h3 style={styles.examTitle}>{exam.title}</h3>
                  <p style={styles.muted}>{exam.courseName || 'Testing de Aplicaciones'} · {exam.durationMinutes || '-'} min</p>
                  <span style={examStatusBadgeStyle(submission, exam)}>
                    {examStatusLabel(submission, exam)}
                  </span>
                </div>
                <div style={{ flexShrink: 0 }}>
                  {isDelivered || isClosed ? (
                    submission ? (
                      <button onClick={() => setView({ type: 'exam', submission })} style={styles.secondaryBtn}>
                        Ver entrega
                      </button>
                    ) : (
                      <span style={styles.closedLabel}>Cerrado</span>
                    )
                  ) : isInProgress ? (
                    <button onClick={() => setView({ type: 'exam', submission })} style={styles.primaryBtn}>
                      Continuar
                    </button>
                  ) : (
                    <button onClick={() => openTopicSelection(exam)} style={styles.primaryBtn}>
                      Seleccionar tema
                    </button>
                  )}
                </div>
              </article>
            )
          })}
        </aside>

        <section style={styles.workspace}>
          <div style={styles.empty}>Seleccioná un examen del panel izquierdo para comenzar o continuar tu entrega.</div>
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
  if (isDecisionTableQuestion(question)) return 'Práctico - Tabla de decisión'
  if (isDecisionTreeQuestion(question)) return 'Práctico - Árbol de decisión'
  if (Number(question.points) === 1 && question.displayOrder <= 6) return `Teórica ${question.displayOrder}`
  return 'Pregunta sin enunciado'
}

function examStatusLabel(submission, exam) {
  if (submission?.status === 'ENTREGADO') return 'Entregado'
  if (submission?.status === 'EN_PROGRESO') return 'En progreso'
  if (exam.status === 'CERRADO') return 'Cerrado'
  return 'Disponible'
}

function examStatusBadgeStyle(submission, exam) {
  const base = { display: 'inline-block', marginTop: 4, padding: '2px 8px', borderRadius: 999, fontSize: 11, fontWeight: 800 }
  if (submission?.status === 'ENTREGADO') return { ...base, background: '#DDF6EC', color: '#087A55' }
  if (submission?.status === 'EN_PROGRESO') return { ...base, background: '#E6EEFF', color: '#1956D8' }
  if (exam.status === 'CERRADO') return { ...base, background: '#ECEFF3', color: '#4A5565' }
  return { ...base, background: '#FFF3CC', color: '#7A5C00' }
}

function formatTime(seconds) {
  const s = Math.max(0, seconds)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
  return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`
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
  examTitle: { fontSize: 15, margin: '0 0 3px', fontWeight: 700 },
  muted: { color: '#536B76', fontSize: 13, margin: 0 },
  closedLabel: { color: '#4A5565', fontSize: 13, fontWeight: 600 },
  primaryBtn: { minHeight: 38, padding: '8px 16px', background: '#1956D8', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  secondaryBtn: { minHeight: 38, padding: '8px 14px', background: '#fff', color: '#1956D8', border: '1px solid #1956D8', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  disabledBtn: { minHeight: 38, padding: '8px 16px', background: '#C9DDE3', color: '#536B76', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'default' },
  workspace: { minWidth: 0, maxWidth: '100%', overflow: 'hidden' },
  message: { background: '#FFF8DF', border: '1px solid #E7CE74', color: '#5D4700', padding: '10px 12px', borderRadius: 8, marginBottom: 14, fontSize: 14 },
  empty: { background: '#fff', border: '1px dashed #B9CDD3', borderRadius: 8, padding: 24, color: '#536B76', textAlign: 'center' },
  // Pantalla de selección de tema
  topicSelectMain: { display: 'flex', justifyContent: 'center', alignItems: 'flex-start', padding: '48px 24px' },
  topicSelectCard: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 12, padding: '32px 40px', maxWidth: 560, width: '100%', display: 'flex', flexDirection: 'column', gap: 16 },
  topicSelectTitle: { fontSize: 26, fontWeight: 800, margin: 0 },
  topicSelectMeta: { color: '#536B76', fontSize: 14, margin: 0 },
  topicSelectInstr: { fontSize: 15, color: '#304653', margin: 0 },
  topicBtns: { display: 'flex', flexWrap: 'wrap', gap: 12 },
  topicBtn: { minWidth: 130, minHeight: 56, padding: '12px 24px', border: '2px solid #D8E8EC', borderRadius: 10, fontSize: 16, fontWeight: 700, cursor: 'pointer', transition: 'all .15s' },
  backBtn: { background: 'none', border: 'none', color: '#1956D8', fontWeight: 700, fontSize: 14, cursor: 'pointer', padding: 0 },
  // Vista de resolución
  examHeader: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 20, display: 'flex', justifyContent: 'space-between', gap: 16, alignItems: 'flex-start', marginBottom: 16 },
  currentTitle: { fontSize: 24, margin: '0 0 6px' },
  headerActions: { display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap', justifyContent: 'flex-end' },
  progressBadge: { background: '#E6EEFF', color: '#1956D8', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
  doneBadge: { background: '#DDF6EC', color: '#087A55', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
  saving: { color: '#536B76', fontSize: 13, fontWeight: 700 },
  timer: { background: '#E6EEFF', color: '#1956D8', padding: '6px 14px', borderRadius: 8, fontSize: 14, fontWeight: 800 },
  timerExpired: { background: '#FDECEA', color: '#9B2C2C', padding: '6px 14px', borderRadius: 8, fontSize: 14, fontWeight: 800 },
  questions: { display: 'flex', flexDirection: 'column', gap: 14 },
  questionCard: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 16, maxWidth: '100%', overflow: 'hidden' },
  questionHeader: { display: 'flex', justifyContent: 'space-between', gap: 12, marginBottom: 10 },
  questionTitle: { fontSize: 17, margin: 0, lineHeight: 1.35 },
  points: { color: '#1956D8', fontWeight: 800, whiteSpace: 'nowrap' },
  answerBox: { width: '100%', minHeight: 300, boxSizing: 'border-box', border: '1px solid #C9DDE3', borderRadius: 6, padding: 12, fontSize: 15, lineHeight: 1.5, fontFamily: 'inherit', color: '#09222A', resize: 'vertical' },
  answerBoxDisabled: { width: '100%', minHeight: 300, boxSizing: 'border-box', border: '1px solid #D8E8EC', borderRadius: 6, padding: 12, fontSize: 15, lineHeight: 1.5, fontFamily: 'inherit', color: '#536B76', background: '#F4F8FA', resize: 'vertical' },
  footerActions: { marginTop: 16, display: 'flex', justifyContent: 'flex-end', gap: 10 },
  practicalAnswerContainer: { width: '100%', maxWidth: '100%', height: 420, overflow: 'auto', boxSizing: 'border-box', border: '1px solid #D8E8EC', borderRadius: 8, background: '#EEF5F7' },
  waitingBanner: { background: '#FFF3CC', border: '1px solid #F9A825', borderRadius: 8, padding: '12px 16px', marginBottom: 14, fontSize: 14, fontWeight: 600, color: '#7A5C00', textAlign: 'center' },
  feedbackBox: { background: '#F0FDF4', border: '1px solid #BBF7D0', borderRadius: 6, padding: '8px 12px', marginBottom: 10 },
  feedbackScore: { fontWeight: 800, color: '#087A55', fontSize: 14 },
  feedbackComment: { margin: '4px 0 0', color: '#304653', fontSize: 13, lineHeight: 1.4 },
  // Modal
  modalOverlay: { position: 'fixed', inset: 0, background: 'rgba(9,34,42,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 },
  modalBox: { background: '#fff', borderRadius: 12, padding: '28px 32px', maxWidth: 460, width: '90%', boxShadow: '0 8px 40px rgba(9,34,42,0.22)' },
  modalTitle: { fontSize: 18, fontWeight: 800, margin: '0 0 10px', color: '#09222A' },
  modalText: { fontSize: 14, color: '#304653', margin: '0 0 20px' },
  modalActions: { display: 'flex', gap: 10, justifyContent: 'flex-end' },
}
