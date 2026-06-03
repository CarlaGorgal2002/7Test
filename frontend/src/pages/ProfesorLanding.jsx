import { useCallback, useEffect, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../api/client.js'
import { clearSession, getCurrentUser } from '../auth/session.js'
import AutoGrowTextarea from '../components/AutoGrowTextarea.jsx'
import DecisionTableEditor, {
  DECISION_TABLE_PREFIX,
  emptyDecisionTableValue,
  isDecisionTablePrompt,
  isDecisionTableValue,
} from '../components/DecisionTableEditor.jsx'
import DecisionTreeEditor, {
  DECISION_TREE_PREFIX,
  emptyDecisionTreeValue,
  isDecisionTreePrompt,
  isDecisionTreeValue,
} from '../components/DecisionTreeEditor.jsx'
import Logo from '../components/Logo.jsx'

const _d = (a) => a.map((c) => String.fromCharCode(c)).join('')

const emptyExam = { title: '', description: '', courseName: 'Testing de Aplicaciones', durationMinutes: 120, availableDate: '', availableTime: '' }
const emptyQuestion = { prompt: '', modelAnswer: '', points: '1' }

const theoryTemplate = {
  prompt: '',
  modelAnswer: '',
  points: '1',
}

const decisionTableTemplate = {
  prompt: '',
  modelAnswer: emptyDecisionTableValue(),
  points: '2',
}

const decisionTreeTemplate = {
  prompt: '',
  modelAnswer: emptyDecisionTreeValue(),
  points: '2',
}

const defaultExamTemplate = [
  theoryTemplate,
  theoryTemplate,
  theoryTemplate,
  theoryTemplate,
  theoryTemplate,
  theoryTemplate,
  decisionTableTemplate,
  decisionTreeTemplate,
]

export default function ProfesorLanding() {
  const navigate = useNavigate()
  const user = getCurrentUser() || {}

  const [exams, setExams] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [examForm, setExamForm] = useState(emptyExam)
  const [editExamForm, setEditExamForm] = useState(emptyExam)
  const [topicAdding, setTopicAdding] = useState(false)
  const [selectedTopicId, setSelectedTopicId] = useState(null)
  const [questionForms, setQuestionForms] = useState({})
  const [editingQuestionForms, setEditingQuestionForms] = useState({})
  const [editingTopicId, setEditingTopicId] = useState(null)
  const [editingTopicName, setEditingTopicName] = useState('')
  const [templateLoading, setTemplateLoading] = useState('')
  const [modal, setModal] = useState(null)
  const [submissions, setSubmissions] = useState([])
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(false)
  const [pageMode, setPageMode] = useState('landing') // 'landing' | 'detail'
  const [profNow, setProfNow] = useState(Date.now())
  const [gradingSubmission, setGradingSubmission] = useState(null)
  const [gradeData, setGradeData] = useState({})
  const [gradeSaving, setGradeSaving] = useState(false)
  const [timeoutPopup, setTimeoutPopup] = useState(null)
  const [popupExtraMinutes, setPopupExtraMinutes] = useState(15)
  const [popupCountdown, setPopupCountdown] = useState(600)

  const selectedExam = useMemo(
    () => exams.find((exam) => exam.id === selectedId) || null,
    [exams, selectedId]
  )

  const activeTopic = useMemo(() => {
    if (!selectedExam?.topics?.length) return null
    return selectedExam.topics.find(t => t.id === selectedTopicId) || selectedExam.topics[0]
  }, [selectedExam, selectedTopicId])

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

  useEffect(() => {
    if (selectedExam?.status !== 'PUBLICADO') return
    const interval = setInterval(() => setProfNow(Date.now()), 1000)
    return () => clearInterval(interval)
  }, [selectedExam?.status])

  useEffect(() => {
    if (!selectedExam) return
    setEditExamForm({
      title: selectedExam.title || '',
      description: selectedExam.description || '',
      courseName: selectedExam.courseName || 'Testing de Aplicaciones',
      durationMinutes: selectedExam.durationMinutes || 120,
      availableDate: selectedExam.availableFrom ? isoToDate(selectedExam.availableFrom) : '',
      availableTime: selectedExam.availableFrom ? isoToTime(selectedExam.availableFrom) : '',
    })
    setSelectedTopicId(selectedExam.topics?.[0]?.id ?? null)
  }, [selectedExam?.id])

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
    let availableFrom
    try {
      availableFrom = scheduleIsoFromForm(examForm)
    } catch (err) {
      setMessage(err.message)
      return
    }
    if (availableFrom && new Date(availableFrom) < new Date()) {
      setMessage('La fecha y hora de inicio no puede ser anterior al momento actual.')
      return
    }
    try {
      const res = await api.post('/exams', {
        title: examForm.title,
        description: examForm.description,
        courseName: examForm.courseName,
        durationMinutes: Number(examForm.durationMinutes) || null,
        availableFrom,
      })
      setExamForm(emptyExam)
      let newExam = res.data

      // Crear Tema A automáticamente con la plantilla base
      try {
        const topicRes = await api.post(`/exams/${newExam.id}/topics`, { name: 'Tema A' })
        newExam = topicRes.data
        const createdTopic = newExam.topics?.[0]
        if (createdTopic) {
          setTemplateLoading(createdTopic.id)
          newExam = await appendDefaultTemplate(newExam, createdTopic.id)
          setSelectedTopicId(createdTopic.id)
        }
      } catch {
        // si falla la plantilla el examen igual queda creado
      } finally {
        setTemplateLoading('')
      }

      setExams((current) => [newExam, ...current])
      setSelectedId(newExam.id)
      setPageMode('detail')
      window.history.pushState({ profe: 'detail' }, '', '/profesor')
      setMessage('Examen creado en borrador con Tema A.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo crear el examen.')
    }
  }

  async function addTopic() {
    if (!selectedExam || topicAdding) return
    setTopicAdding(true)
    setMessage('')
    const nextLetter = nextTopicLetter(selectedExam.topics || [])
    const name = `Tema ${nextLetter}`
    try {
      const previousTopicIds = new Set((selectedExam.topics || []).map((t) => t.id))
      const res = await api.post(`/exams/${selectedExam.id}/topics`, { name })
      let updated = res.data
      const createdTopic = updated.topics?.find((t) => !previousTopicIds.has(t.id))
      replaceExam(updated)
      if (createdTopic) {
        setTemplateLoading(createdTopic.id)
        updated = await appendDefaultTemplate(updated, createdTopic.id)
        replaceExam(updated)
        setSelectedTopicId(createdTopic.id)
      }
      setMessage(`Tema ${nextLetter} agregado.`)
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo agregar el tema.')
    } finally {
      setTemplateLoading('')
      setTopicAdding(false)
    }
  }

  async function removeTopic(topicId) {
    if (!selectedExam || !topicId) return
    if (!window.confirm('¿Seguro que querés eliminar este tema? Se van a borrar también sus preguntas.')) return
    setMessage('')
    try {
      const res = await api.delete(`/exams/${selectedExam.id}/topics/${topicId}`)
      let updated = res.data
      // Renombrar temas restantes para mantener A, B, C... sin saltearse letras
      const remaining = [...(updated.topics || [])].sort((a, b) => a.name.localeCompare(b.name))
      for (let i = 0; i < remaining.length; i++) {
        const expected = `Tema ${String.fromCharCode(65 + i)}`
        if (remaining[i].name !== expected) {
          const r = await api.put(`/exams/${updated.id}/topics/${remaining[i].id}`, { name: expected })
          updated = r.data
        }
      }
      replaceExam(updated)
      setSelectedTopicId((updated.topics || [])[0]?.id ?? null)
      setMessage('Tema eliminado.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo eliminar el tema.')
    }
  }
async function renameTopic(topicId) {
  if (!selectedExam || !editingTopicName.trim()) return
  setMessage('')
  try {
    const res = await api.put(`/exams/${selectedExam.id}/topics/${topicId}`, {
      name: editingTopicName.trim(),
    })
    replaceExam(res.data)
    setEditingTopicId(null)
    setEditingTopicName('')
    setMessage('Nombre del tema actualizado.')
  } catch (err) {
    setMessage(err.response?.data?.message || 'No se pudo renombrar el tema.')
  }
}

  async function updateExam(e) {
    e.preventDefault()
    if (!selectedExam || selectedExam.status !== 'BORRADOR') return
    setMessage('')
    let af
    try {
      af = scheduleIsoFromForm(editExamForm)
    } catch (err) {
      setMessage(err.message)
      return
    }
    if (af && new Date(af) < new Date()) {
      setMessage('La fecha y hora de inicio no puede ser anterior al momento actual.')
      return
    }
    try {
      const res = await api.put(`/exams/${selectedExam.id}`, {
        title: editExamForm.title,
        description: editExamForm.description,
        courseName: editExamForm.courseName || selectedExam.courseName || 'Testing de Aplicaciones',
        durationMinutes: Number(editExamForm.durationMinutes || selectedExam.durationMinutes) || null,
        availableFrom: af,
      })
      replaceExam(res.data)
      setMessage('Datos del borrador actualizados.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudieron actualizar los datos del borrador.')
    }
  }

  async function addQuestion(e, topicId) {
    e.preventDefault()
    if (!selectedExam) return
    const form = questionForms[topicId] || emptyQuestion
    setMessage('')
    try {
      const modelAnswer = normalizedModelAnswer(form)
      const res = await api.post(`/exams/${selectedExam.id}/topics/${topicId}/questions`, {
        prompt: form.prompt,
        modelAnswer,
        points: Number(form.points),
      })
      replaceExam(res.data)
      setQuestionForms((current) => ({ ...current, [topicId]: emptyQuestion }))
      setMessage('Pregunta agregada.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo agregar la pregunta.')
    }
  }

  async function updateQuestion(e, topic, question) {
    e.preventDefault()
    if (!selectedExam || !topic || !question) return
    const form = editingQuestionForms[question.id]
    if (!form) return
    setMessage('')
    try {
      const modelAnswer = normalizedModelAnswer(form)
      const res = await api.put(`/exams/${selectedExam.id}/topics/${topic.id}/questions/${question.id}`, {
        prompt: form.prompt,
        modelAnswer,
        points: Number(form.points),
      })
      replaceExam(res.data)
      setEditingQuestionForms((current) => {
        const next = { ...current }
        delete next[question.id]
        return next
      })
      setMessage('Pregunta actualizada.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo actualizar la pregunta.')
    }
  }

  async function loadDefaultTemplate(topic) {
    if (!selectedExam || !topic) return
    if (Number(topic.totalPoints || 0) > 0 || topic.questions.length > 0) {
      setMessage('La plantilla completa se carga sobre un tema vacio.')
      return
    }
    setTemplateLoading(topic.id)
    setMessage('')
    try {
      const updated = await appendDefaultTemplate(selectedExam, topic.id)
      replaceExam(updated)
      setMessage('Plantilla cargada: 6 teoricas vacias de 1 punto, tabla vacia de 2 puntos y arbol vacio de 2 puntos.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo cargar la plantilla.')
    } finally {
      setTemplateLoading('')
    }
  }

  async function appendDefaultTemplate(exam, topicId) {
    let updated = exam
    for (const question of defaultExamTemplate) {
      const res = await api.post(`/exams/${updated.id}/topics/${topicId}/questions`, {
        prompt: question.prompt,
        modelAnswer: question.modelAnswer,
        points: Number(question.points),
      })
      updated = res.data
    }
    return updated
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
      const examToPublish = await saveDraftBeforePublish(selectedExam)
      const res = await api.patch(`/exams/${examToPublish.id}/publish`)
      replaceExam(res.data)
      setMessage(publishSuccessMessage(res.data))
    } catch (err) {
      setMessage(err.response?.data?.message || err.message || 'No se pudo publicar el examen.')
    }
  }

  async function saveDraftBeforePublish(exam) {
    if (!exam || exam.status !== 'BORRADOR') return exam
    const availableFrom = scheduleIsoFromForm(editExamForm)
    if (availableFrom && new Date(availableFrom) < new Date()) {
      throw new Error('La fecha y hora de inicio ya paso. Corregila en "Datos del borrador" antes de publicar.')
    }
    const res = await api.put(`/exams/${exam.id}`, {
      title: editExamForm.title,
      description: editExamForm.description,
      courseName: editExamForm.courseName || exam.courseName || 'Testing de Aplicaciones',
      durationMinutes: Number(editExamForm.durationMinutes || exam.durationMinutes) || null,
      availableFrom,
    })
    replaceExam(res.data)
    return res.data
  }

  function handlePublishClick() {
    if (!selectedExam) return

    let unsaved
    try {
      unsaved = scheduleIsoFromForm(editExamForm)
    } catch (err) {
      setMessage(err.message)
      return
    }
    if (unsaved && new Date(unsaved) < new Date()) {
      setMessage('La fecha y hora de inicio ya pasó. Corregila en "Datos del borrador" antes de publicar.')
      return
    }

    const missingAnswers = findMissingAnswers(selectedExam)
    if (missingAnswers.length > 0) {
      setModal({ type: 'missingAnswers', items: missingAnswers })
      return
    }
    const badTopics = (selectedExam.topics || []).filter((t) => Number(t.totalPoints) !== 10)
    if (badTopics.length > 0) {
      setModal({ type: 'badPoints', topics: badTopics })
    } else {
      setModal({ type: 'confirmPublish' })
    }
  }

  function handleCloseClick() {
    if (!selectedExam) return
    setModal({ type: 'confirmClose' })
  }

  async function redistributeAndPublish() {
    const topicsToFix = modal?.topics || []
    setModal(null)
    setMessage('')
    try {
      let exam = selectedExam
      exam = await saveDraftBeforePublish(exam)
      for (const topic of exam.topics.filter((t) => topicsToFix.some((bt) => bt.id === t.id))) {
        exam = await redistributeTopicPoints(exam, topic)
        replaceExam(exam)
      }
      const res = await api.patch(`/exams/${exam.id}/publish`)
      replaceExam(res.data)
      setMessage(publishSuccessMessage(res.data))
    } catch (err) {
      setMessage(err.response?.data?.message || err.message || 'No se pudo publicar el examen.')
    }
  }

  async function redistributeTopicPoints(exam, topic) {
    const n = topic.questions.length
    if (n === 0) return exam
    const base = Math.floor((10 / n) / 0.25) * 0.25
    const remainder = parseFloat((10 - base * n).toFixed(~-2))
    const targets = topic.questions.map((q, i) => ({
      ...q,
      targetPoints: i === n - 1 ? parseFloat((base + remainder).toFixed(2)) : base,
    }))
    const sorted = [...targets].sort(
      (a, b) => (a.targetPoints - Number(a.points)) - (b.targetPoints - Number(b.points))
    )
    let updated = exam
    for (const q of sorted) {
      const res = await api.put(`/exams/${updated.id}/topics/${topic.id}/questions/${q.id}`, {
        prompt: q.prompt,
        modelAnswer: q.modelAnswer,
        points: q.targetPoints,
      })
      updated = res.data
    }
    return updated
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

  async function deleteExam(examId) {
    setMessage('')
    try {
      await api.delete(`/exams/${examId}`)
      setExams((prev) => prev.filter((e) => e.id !== examId))
      if (selectedExam?.id === examId) setSelectedId(null)
      setModal(null)
      setMessage('Examen eliminado.')
    } catch (err) {
      setModal(null)
      setMessage(err.response?.data?.message || 'No se pudo eliminar el examen.')
    }
  }

  async function openGrading(submission) {
    setMessage('')
    try {
      const res = await api.get(`/submissions/${submission.id}`)
      const detail = res.data
      setGradingSubmission(detail)
      const initial = {}
      detail.questions.forEach(q => {
        initial[q.questionId] = { score: q.score ?? '', comment: q.comment ?? '' }
      })
      setGradeData(initial)
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo cargar la entrega.')
    }
  }

  async function saveGrade() {
    if (!gradingSubmission) return
    setGradeSaving(true)
    try {
      const answers = gradingSubmission.questions.map(q => ({
        questionId: q.questionId,
        score: gradeData[q.questionId]?.score === '' ? null : Number(gradeData[q.questionId]?.score ?? null),
        comment: gradeData[q.questionId]?.comment || '',
      }))
      const res = await api.put(`/submissions/${gradingSubmission.id}/grade`, { answers })
      setGradingSubmission(res.data)
      setMessage('Calificación guardada.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo guardar la calificación.')
    } finally {
      setGradeSaving(false)
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

  function applyQuestionTemplate(topicId, template) {
    setQuestionForms((current) => ({
      ...current,
      [topicId]: { ...template },
    }))
  }

  function startEditQuestion(question) {
    setEditingQuestionForms((current) => ({
      ...current,
      [question.id]: {
        prompt: question.prompt,
        modelAnswer: initialEditorValue(question),
        points: String(question.points),
      },
    }))
  }

  function updateEditingQuestionForm(questionId, field, value) {
    setEditingQuestionForms((current) => ({
      ...current,
      [questionId]: { ...current[questionId], [field]: value },
    }))
  }

  function cancelEditQuestion(questionId) {
    setEditingQuestionForms((current) => {
      const next = { ...current }
      delete next[questionId]
      return next
    })
  }

  const _isLocked = (status) => status !== _d([66,79,82,82,65,68,79,82])
  const canEdit = selectedExam != null && !_isLocked(selectedExam.status)

  const profIsScheduled = selectedExam ? derivedStatus(selectedExam) === 'PROGRAMADO' : false
  const profStartRef = profIsScheduled ? null : (selectedExam?.availableFrom || selectedExam?.publishedAt)
  const profExamEndMs = profStartRef && selectedExam?.durationMinutes
    ? new Date(profStartRef).getTime() + selectedExam.durationMinutes * 60_000
    : null

  // Dispara el popup cuando el timer del examen PUBLICADO llega a 0 (solo una vez)
  useEffect(() => {
    if (!selectedExam || selectedExam.status !== 'PUBLICADO') return
    if (selectedExam.extraTimeUsed) return
    if (!profExamEndMs || profNow <= profExamEndMs) return
    if (timeoutPopup) return
    setTimeoutPopup({ examId: selectedExam.id, examTitle: selectedExam.title })
    setPopupCountdown(600)
    setPopupExtraMinutes(15)
  }, [profNow, selectedExam?.id, selectedExam?.status, selectedExam?.extraTimeUsed, profExamEndMs])

  // Cuenta regresiva del popup — si llega a 0 cierra el examen automáticamente
  useEffect(() => {
    if (!timeoutPopup) return
    if (popupCountdown <= 0) {
      api.patch(`/exams/${timeoutPopup.examId}/close`)
        .then(res => replaceExam(res.data))
        .catch(() => {})
      setTimeoutPopup(null)
      return
    }
    const t = setTimeout(() => setPopupCountdown(c => c - 1), 1000)
    return () => clearTimeout(t)
  }, [timeoutPopup, popupCountdown])

  function openExamDetail(examId) {
    setSelectedId(examId)
    const exam = exams.find(e => e.id === examId)
    const mode = exam?.status === 'PUBLICADO' ? 'running' : 'detail'
    setPageMode(mode)
    setMessage('')
    window.history.pushState({ profe: mode }, '', '/profesor')
  }

  function goToExamList() {
    setPageMode('examList')
    setMessage('')
    window.history.pushState({ profe: 'examList' }, '', '/profesor')
  }

  useEffect(() => {
    function onPopState() {
      setPageMode('landing')
      setMessage('')
    }
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  const figmaHeader = (backBtn = null) => (
    <header style={styles.header}>
      <div style={styles.brand}>
        <Logo dark size={36} />
        <div>
          <span style={{ fontSize: 11, color: 'rgba(203,238,243,0.6)', fontWeight: 600, letterSpacing: '0.05em' }}>Panel docente</span>
          <h1 style={{ ...styles.headerTitle, fontSize: 22, margin: 0 }}>Hola, {user.fullName?.split(' ')[0] || user.email}</h1>
        </div>
      </div>
      <button onClick={handleLogout} style={styles.logoutBtn}>Cerrar sesión</button>
    </header>
  )

  // ── EXAM LIST (Crear / Editar) ────────────────────────────────────────────
  if (pageMode === 'examList') {
    return (
      <div style={styles.page}>
        {figmaHeader()}
        <main style={{ ...lStyles.detailMain, maxWidth: 1000 }}>
          <button onClick={() => setPageMode('landing')} style={fStyles.backLink}>← Volver</button>
          <div style={fStyles.pageHeaderRow}>
            <div>
              <h2 style={fStyles.pageTitle}>Creación y edición de exámenes</h2>
              <p style={fStyles.pageSubtitle}>Creá uno nuevo o editá un examen del listado.</p>
            </div>
            <button onClick={() => { setPageMode('newExam'); window.history.pushState({}, '', '/profesor') }} style={fStyles.createBtn}>
              &#x2795; Crear examen
            </button>
          </div>
          {message && <div style={styles.message}>{message}</div>}
          <div style={fStyles.tableCard}>
            {exams.length === 0 && !loading && (
              <div style={fStyles.emptyBox}>
                <p style={{ color: '#9CA3AF', margin: 0 }}>📋 Todavía no hay exámenes. Creá el primero.</p>
              </div>
            )}
            {exams.length > 0 && (
              <table style={fStyles.table}>
                <thead>
                  <tr>
                    <th style={fStyles.th}>Nombre de examen</th>
                    <th style={fStyles.th}>Estado</th>
                    <th style={fStyles.th}>Duración</th>
                    <th style={fStyles.th}></th>
                    <th style={fStyles.th}></th>
                  </tr>
                </thead>
                <tbody>
                  {exams.map(exam => (
                    <tr key={exam.id} style={fStyles.tr}>
                      <td style={fStyles.td}>{exam.title}</td>
                      <td style={fStyles.td}><span style={statusStyle(exam)}>{labelStatus(exam)}</span></td>
                      <td style={{ ...fStyles.td, color: '#6B7280' }}>{exam.durationMinutes || '-'} min</td>
                      <td style={fStyles.td}>
                        <button
                          onClick={() => { setSelectedId(exam.id); setPageMode(exam.status === 'PUBLICADO' ? 'running' : 'detail'); window.history.pushState({}, '', '/profesor') }}
                          style={fStyles.editBtn}
                        >
                          {exam.status === 'PUBLICADO' ? 'Monitorear' : '✎ Editar'}
                        </button>
                      </td>
                      <td style={fStyles.td}>
                        {(exam.status === 'BORRADOR' || exam.status === 'CERRADO') && (
                          <button onClick={() => setModal({ type: 'confirmDelete', examId: exam.id, examTitle: exam.title })} style={fStyles.deleteBtn}>🗑</button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </div>
        </main>
      </div>
    )
  }

  // ── NEW EXAM FORM ─────────────────────────────────────────────────────────
  if (pageMode === 'newExam') {
    return (
      <div style={styles.page}>
        {figmaHeader()}
        <main style={{ ...lStyles.detailMain, maxWidth: 600 }}>
          <button onClick={() => { setPageMode('examList'); setMessage('') }} style={fStyles.backLink}>← Volver</button>
          <h2 style={fStyles.pageTitle}>Nuevo examen</h2>
          {message && <div style={styles.message}>{message}</div>}
          <div style={fStyles.formCard}>
            <form onSubmit={createExam} style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              <label style={styles.label}>Título</label>
              <input value={examForm.title} onChange={e => setExamForm({ ...examForm, title: e.target.value })} style={styles.input} placeholder="Primer parcial" required />
              <label style={styles.label}>Descripción</label>
              <textarea value={examForm.description} onChange={e => setExamForm({ ...examForm, description: e.target.value })} style={styles.textarea} rows={2} placeholder="Evaluación de Testing de Aplicaciones" />
              <label style={styles.label}>Materia</label>
              <input value={examForm.courseName} onChange={e => setExamForm({ ...examForm, courseName: e.target.value })} style={styles.input} placeholder="Testing de Aplicaciones" />
              <label style={styles.label}>Duración estimada (min)</label>
              <input type="number" min="1" value={examForm.durationMinutes} onChange={e => setExamForm({ ...examForm, durationMinutes: e.target.value })} style={styles.input} />
              <label style={styles.label}>Fecha de inicio (opcional)</label>
              <input type="date" min={todayStr()} value={examForm.availableDate} onChange={e => setExamForm({ ...examForm, availableDate: e.target.value })} style={styles.input} />
              {examForm.availableDate && (
                <>
                  <label style={styles.label}>Hora — 24h (HH:MM)</label>
                  <input type="text" value={examForm.availableTime} onChange={e => setExamForm({ ...examForm, availableTime: e.target.value })} style={styles.input} placeholder="14:00" maxLength={5} />
                  {examForm.availableTime && !isValidTime(examForm.availableTime) && <span style={styles.fieldError}>Hora inválida</span>}
                </>
              )}
              <button type="submit" style={{ ...styles.primaryBtn, marginTop: 8 }}>Crear borrador</button>
            </form>
          </div>
        </main>
      </div>
    )
  }

  // ── RUNNING EXAM (vista estilo Figma) ────────────────────────────────────
  if (pageMode === 'running' && selectedExam) {
    const topicColorMap = {}
    selectedExam.topics?.forEach(t => { topicColorMap[t.id] = t.colorHex || '#333' })
    const isBeforeStart = derivedStatus(selectedExam) === 'PROGRAMADO'
    const startRef = selectedExam.availableFrom || selectedExam.publishedAt
    const examEndMs = !isBeforeStart && startRef && selectedExam.durationMinutes
      ? new Date(startRef).getTime() + selectedExam.durationMinutes * 60_000 : null
    const secondsLeft = examEndMs ? Math.max(0, Math.floor((examEndMs - profNow) / 1000)) : null
    const isExpired = secondsLeft !== null && secondsLeft === 0

    const statusLabelRunning = (s) => {
      if (s.status === 'ENTREGADO') return { label: 'Entregado', bg: '#D1FAE5', color: '#065F46' }
      return { label: 'En proceso', bg: '#FEF3C7', color: '#92400E' }
    }

    return (
      <div style={styles.page}>
        {figmaHeader()}
        <main style={fStyles.runningMain}>
          <div style={fStyles.runningLeft}>
            <button onClick={() => setPageMode('landing')} style={fStyles.backLink}>← Volver</button>
            <h2 style={fStyles.pageTitle}>{isBeforeStart ? 'Examen programado' : 'Examen en curso'}</h2>
            <p style={fStyles.pageSubtitle}>{selectedExam.courseName} · {selectedExam.title}</p>
            {message && <div style={styles.message}>{message}</div>}
            <div style={fStyles.tableCard}>
              {submissions.length === 0 ? (
                <p style={{ color: '#6B7280', padding: '24px 0', textAlign: 'center', margin: 0 }}>Todavía no hay alumnos que hayan iniciado este examen.</p>
              ) : (
                <table style={fStyles.table}>
                  <thead>
                    <tr>
                      <th style={fStyles.th}>Alumnos</th>
                      <th style={{ ...fStyles.th, textAlign: 'center' }}>Tema</th>
                      <th style={{ ...fStyles.th, textAlign: 'center' }}>Estado</th>
                    </tr>
                  </thead>
                  <tbody>
                    {submissions.map(s => {
                      const badge = statusLabelRunning(s)
                      const color = topicColorMap[s.topicId] || '#1956D8'
                      const letter = s.topicName?.replace('Tema ', '') || '?'
                      const excedido = examEndMs && s.status !== 'ENTREGADO' && profNow > examEndMs
                      return (
                        <tr key={s.id} style={fStyles.tr}>
                          <td style={fStyles.td}>{s.studentName}</td>
                          <td style={{ ...fStyles.td, textAlign: 'center' }}>
                            <span style={{ background: color, color: '#fff', padding: '4px 18px', borderRadius: 6, fontWeight: 800, fontSize: 15, display: 'inline-block' }}>{letter}</span>
                          </td>
                          <td style={{ ...fStyles.td, textAlign: 'center' }}>
                            <span style={{ background: badge.bg, color: badge.color, padding: '4px 12px', borderRadius: 999, fontSize: 13, fontWeight: 700 }}>{badge.label}</span>
                            {excedido && <div style={{ fontSize: 11, color: '#9B2C2C', marginTop: 4 }}>Excedido de tiempo</div>}
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              )}
            </div>
          </div>

          <div style={fStyles.runningRight}>
            {isExpired ? (
              <div style={fStyles.examFinishedBtn}>Examen finalizado</div>
            ) : isBeforeStart ? (
              <div style={fStyles.examScheduledBtn}>
                Programado
              </div>
            ) : (
              <button onClick={handleCloseClick} style={fStyles.finalizeBtn}>
                ⏸ Finalizar examen
              </button>
            )}
            {isBeforeStart && (
              <div style={fStyles.timerWidget}>
                <p style={fStyles.timerLabel}>INICIO PROGRAMADO:</p>
                <p style={fStyles.timerValue}>{formatScheduleDateTime(selectedExam.availableFrom)}</p>
              </div>
            )}
            {secondsLeft !== null && (
              <div style={fStyles.timerWidget}>
                <p style={fStyles.timerLabel}>TIEMPO DE EXAMEN RESTANTE:</p>
                <p style={{ ...fStyles.timerValue, color: isExpired ? '#9CA3AF' : secondsLeft < 600 ? '#DC2626' : '#1956D8' }}>
                  ⏰ {formatProfTime(secondsLeft)}
                </p>
              </div>
            )}
            {/* Contadores */}
            {submissions.length > 0 && (
              <div style={fStyles.countersWidget}>
                <div style={fStyles.counterItem}><span style={{ fontSize: 28, fontWeight: 800, color: '#1956D8' }}>{submissions.filter(s => s.status !== 'ENTREGADO').length}</span><span style={{ fontSize: 13, color: '#6B7280' }}>En proceso</span></div>
                <div style={fStyles.counterItem}><span style={{ fontSize: 28, fontWeight: 800, color: '#065F46' }}>{submissions.filter(s => s.status === 'ENTREGADO').length}</span><span style={{ fontSize: 13, color: '#6B7280' }}>Entregados</span></div>
                <div style={fStyles.counterItem}><span style={{ fontSize: 28, fontWeight: 800, color: '#374151' }}>{submissions.length}</span><span style={{ fontSize: 13, color: '#6B7280' }}>Total</span></div>
              </div>
            )}
          </div>
        </main>
        {/* Timeout popup */}
        {timeoutPopup && (
          <div style={styles.modalOverlay}>
            <div style={{ ...styles.modalBox, maxWidth: 480 }}>
              <h3 style={styles.modalTitle}>⏰ Terminó el tiempo</h3>
              <p style={styles.modalText}>El tiempo del examen <strong>{timeoutPopup.examTitle}</strong> llegó a su fin. ¿Deseás agregar tiempo extra?</p>
              <p style={{ color: '#DC2626', fontWeight: 700, fontSize: 14 }}>El examen se cerrará automáticamente en {formatProfTime(popupCountdown)}</p>
              <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap', marginTop: 8 }}>
                <button onClick={async () => { const mins = Math.min(60, Math.max(1, Number(popupExtraMinutes) || 15)); try { const res = await api.patch(`/exams/${timeoutPopup.examId}/add-extra-time`, { extraMinutes: mins }); replaceExam(res.data) } catch (err) { setMessage(err.response?.data?.message || 'No se pudo agregar tiempo.') } setTimeoutPopup(null) }} style={styles.primaryBtn}>
                  Sí, agregar
                </button>
                <input type="number" min="1" max="60" value={popupExtraMinutes} onChange={e => setPopupExtraMinutes(e.target.value)} style={{ width: 70, ...styles.input }} />
                <span style={{ fontSize: 13, color: '#6B7280' }}>min (máx. 60)</span>
              </div>
              <div style={{ marginTop: 16 }}>
                <button onClick={async () => { try { const res = await api.patch(`/exams/${timeoutPopup.examId}/close`); replaceExam(res.data) } catch {} setTimeoutPopup(null) }} style={styles.closeBtn}>No, cerrar examen</button>
              </div>
            </div>
          </div>
        )}
      </div>
    )
  }

  // ── LANDING PAGE (4 cards) ─────────────────────────────────────────────
  if (pageMode === 'landing') {
    const enCurso = exams.filter(e => derivedStatus(e) === 'EN_CURSO')
    const programados = exams.filter(e => derivedStatus(e) === 'PROGRAMADO')
    const borradores = exams.filter(e => derivedStatus(e) === 'BORRADOR')
    const porDevolver = exams.filter(e => e.status === 'CERRADO' && !e.feedbackPublished)
    const devueltos = exams.filter(e => e.status === 'CERRADO' && e.feedbackPublished)

    const cards = [
      {
        title: 'Crear / Editar exámenes',
        subtitle: 'Gestioná el contenido de tus parciales',
        stats: [{ n: borradores.length, label: 'borradores' }, { n: exams.length, label: 'exámenes totales' }],
        action: goToExamList,
        color: '#1956D8',
      },
      {
        title: 'Exámenes activos',
        subtitle: 'Monitoreá los exámenes en curso o programados',
        stats: [{ n: enCurso.length, label: 'en curso' }, { n: programados.length, label: 'programados' }],
        action: () => { const first = [...enCurso, ...programados][0]; if (first) openExamDetail(first.id) },
        color: '#087A55',
        disabled: enCurso.length + programados.length === 0,
      },
      {
        title: 'Revisión pendiente',
        subtitle: 'Calificá las entregas de los exámenes cerrados',
        stats: [{ n: porDevolver.length, label: 'por calificar' }],
        action: () => { const first = porDevolver[0]; if (first) { setSelectedId(first.id); setPageMode('detail'); window.history.pushState({}, '', '/profesor') } },
        color: '#9B2C2C',
        disabled: porDevolver.length === 0,
      },
      {
        title: 'Historial',
        subtitle: 'Exámenes cerrados ya devueltos',
        stats: [{ n: devueltos.length, label: 'devueltos' }],
        action: goToExamList,
        color: '#4A5565',
      },
    ]

    return (
      <div style={styles.page}>
        {figmaHeader()}
        <main style={fStyles.landingCards}>
          {message && <div style={{ ...styles.message, gridColumn: '1/-1', marginBottom: 0 }}>{message}</div>}
          {cards.map((card, i) => (
            <div
              key={i}
              onClick={!card.disabled ? card.action : undefined}
              style={{ ...fStyles.navCard, opacity: card.disabled ? 0.5 : 1, cursor: card.disabled ? 'not-allowed' : 'pointer', borderTop: `4px solid ${card.color}` }}
            >
              <h3 style={{ ...fStyles.navCardTitle, color: card.color }}>{card.title}</h3>
              <p style={fStyles.navCardSub}>{card.subtitle}</p>
              <div style={fStyles.navCardDivider} />
              <div style={fStyles.navCardStats}>
                {card.stats.map((s, j) => (
                  <div key={j} style={fStyles.navStat}>
                    <span style={{ fontSize: 26, fontWeight: 800, color: card.color }}>{s.n}</span>
                    <span style={{ fontSize: 12, color: '#6B7280' }}>{s.label}</span>
                  </div>
                ))}
              </div>
              <div style={fStyles.navCardArrow}>→</div>
            </div>
          ))}
        </main>
      </div>
    )
  }

  // ── DETAIL PAGE ───────────────────────────────────────────────────────────
  return (
    <div style={styles.page}>
      <header style={styles.header}>
        <div style={styles.brand}>
          <button onClick={() => setPageMode('landing')} style={lStyles.backBtn}>← Volver</button>
          <Logo dark size={32} />
          <div>
            <span style={{ fontSize: 11, color: 'rgba(203,238,243,0.6)', fontWeight: 600, letterSpacing: '0.05em' }}>Panel docente</span>
            <h1 style={{ ...styles.headerTitle, fontSize: 18, margin: 0, maxWidth: 400, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{selectedExam?.title || 'Detalle de examen'}</h1>
          </div>
        </div>
        <button onClick={handleLogout} style={styles.logoutBtn}>Cerrar sesión</button>
      </header>

      <main style={lStyles.detailMain}>

        <section style={{ ...styles.workspace, maxWidth: 960, margin: '0 auto', width: '100%' }}>
          {message && <div style={styles.message}>{message}</div>}

          {!selectedExam ? (
            <div style={styles.emptyState}>Seleccioná un examen desde "Mis exámenes" para editarlo.</div>
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
                  <span style={statusStyle(selectedExam)}>{labelStatus(selectedExam)}</span>
                  {canEdit && (
                    <button onClick={handlePublishClick} style={styles.primaryBtn}>Publicar</button>
                  )}
                  {selectedExam.status === 'PUBLICADO' && (
                    <button onClick={handleCloseClick} style={styles.closeBtn}>Cerrar examen</button>
                  )}
                </div>
              </div>


              {canEdit && (
                <>
                  <form onSubmit={updateExam} style={styles.editBox}>
                    <h3 style={styles.editTitle}>Datos del borrador</h3>
                    <div style={styles.editGrid}>
                      <div style={styles.fieldBlock}>
                        <label style={styles.label}>Titulo</label>
                        <input
                          value={editExamForm.title}
                          onChange={(e) => setEditExamForm({ ...editExamForm, title: e.target.value })}
                          style={styles.input}
                          required
                        />
                      </div>
                      <div style={styles.fieldBlock}>
                        <label style={styles.label}>Descripcion</label>
                        <textarea
                          value={editExamForm.description}
                          onChange={(e) => setEditExamForm({ ...editExamForm, description: e.target.value })}
                          style={styles.textarea}
                          rows={3}
                        />
                      </div>
                      <div style={styles.fieldBlock}>
                        <label style={styles.label}>Fecha de inicio (opcional)</label>
                        <input
                          type="date"
                          min={todayStr()}
                          value={editExamForm.availableDate || ''}
                          onChange={(e) => setEditExamForm({ ...editExamForm, availableDate: e.target.value })}
                          style={styles.input}
                        />
                        {editExamForm.availableDate && (
                          <>
                            <label style={{ ...styles.label, marginTop: 6 }}>Hora — formato 24h (HH:MM)</label>
                            <input
                              type="text"
                              value={editExamForm.availableTime || ''}
                              onChange={(e) => setEditExamForm({ ...editExamForm, availableTime: e.target.value })}
                              style={styles.input}
                              placeholder="14:00"
                              maxLength={5}
                            />
                            {editExamForm.availableTime && !isValidTime(editExamForm.availableTime) && (
                              <span style={styles.fieldError}>Hora inválida. Usá formato HH:MM (ej: 14:00)</span>
                            )}
                          </>
                        )}
                      </div>
                    </div>
                    <div style={styles.editActions}>
                      <button type="submit" style={styles.secondaryBtn}>Guardar cambios</button>
                    </div>
                  </form>

                  <div style={styles.topicForm}>
                    <span style={styles.muted}>Siguiente: <strong>Tema {nextTopicLetter(selectedExam.topics || [])}</strong></span>
                    <button type="button" onClick={addTopic} disabled={topicAdding} style={topicAdding ? styles.disabledBtn : styles.secondaryBtn}>
                      {topicAdding ? 'Agregando...' : 'Agregar tema'}
                    </button>
                  </div>
                </>
              )}

              {!canEdit && (() => {
                const isScheduled = derivedStatus(selectedExam) === 'PROGRAMADO'
                const examEndMs = profExamEndMs
                const secondsLeft = !isScheduled && examEndMs ? Math.floor((examEndMs - profNow) / 1000) : null
                const enProgreso = submissions.filter(s => s.status !== 'ENTREGADO').length
                const entregados = submissions.filter(s => s.status === 'ENTREGADO').length
                return (
                  <section style={styles.submissionPanel}>
                    <div style={styles.submissionHeader}>
                      <h3 style={styles.submissionTitle}>Entregas de alumnos</h3>
                      <div style={{ display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap' }}>
                        {isScheduled && (
                          <span style={styles.timerProf}>Programado para {formatScheduleDateTime(selectedExam.availableFrom)}</span>
                        )}
                        {secondsLeft !== null && (
                          <span style={secondsLeft <= 0 ? styles.timerExpiredProf : styles.timerProf}>
                            {secondsLeft <= 0 ? '⏰ Tiempo vencido' : `⏱ ${formatProfTime(secondsLeft)} restantes`}
                          </span>
                        )}
                      </div>
                    </div>
                    {submissions.length > 0 && (
                      <div style={styles.submissionCounters}>
                        <span style={styles.counterBadgeProgress}>En progreso: {enProgreso}</span>
                        <span style={styles.counterBadgeDone}>Entregado: {entregados}</span>
                        <span style={styles.counterBadgeTotal}>Total: {submissions.length}</span>
                        {(() => {
                          const allGraded = submissions.length > 0
                            && submissions.every(s => s.status === 'ENTREGADO' && isGraded(s))
                          if (selectedExam.feedbackPublished) {
                            return <span style={styles.feedbackPublishedBadge}>✓ Devoluciones entregadas</span>
                          }
                          if (allGraded) {
                            return (
                              <button
                                onClick={async () => {
                                  try {
                                    const res = await api.patch(`/exams/${selectedExam.id}/publish-feedback`)
                                    replaceExam(res.data)
                                  } catch (err) {
                                    setMessage(err.response?.data?.message || 'No se pudieron entregar las devoluciones.')
                                  }
                                }}
                                style={styles.publishFeedbackBtn}
                              >
                                Entregar devoluciones
                              </button>
                            )
                          }
                          return null
                        })()}
                      </div>
                    )}
                    {submissions.length === 0 ? (
                      <p style={styles.muted}>Todavia no hay alumnos que hayan iniciado este examen.</p>
                    ) : (
                      <div style={styles.submissionList}>
                        {submissions.map((submission) => {
                          const excedido = examEndMs && submission.status !== 'ENTREGADO' && profNow > examEndMs
                          return (
                            <div key={submission.id} style={styles.submissionRow}>
                              <div style={{ flex: 1, minWidth: 0 }}>
                                <strong>{submission.studentName}</strong>
                                <p style={styles.answer}>Tema: {submission.topicName}</p>
                              </div>
                              <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 4 }}>
                                {submission.status === 'ENTREGADO' ? (
                                  isGraded(submission)
                                    ? <span style={styles.gradedBadge}>Calificado</span>
                                    : <span style={styles.submittedBadge}>Entregado</span>
                                ) : (
                                  <span style={styles.progressBadge}>En progreso</span>
                                )}
                                {excedido && <span style={styles.exceededBadge}>Excedido de tiempo</span>}
                                {submission.status === 'ENTREGADO' && (
                                  <button onClick={() => openGrading(submission)} style={styles.gradeBtn}>
                                    {isGraded(submission) ? 'Volver a calificar' : 'Calificar'}
                                  </button>
                                )}
                              </div>
                            </div>
                          )
                        })}
                      </div>
                    )}
                  </section>
                )
              })()}

              {selectedExam.topics?.length === 0 && (
                <div style={styles.emptyState}>Agrega al menos un tema. Para publicar, cada tema debe sumar 10 puntos.</div>
              )}

              {selectedExam.topics?.length > 0 && (
                <div style={styles.topicTabs}>
                  {selectedExam.topics.map((t) => {
                    const isActive = t.id === activeTopic?.id
                    const ok = Number(t.totalPoints) === 10
                    return (
                      <button
                        key={t.id}
                        onClick={() => setSelectedTopicId(t.id)}
                        style={{
                          ...styles.topicTab,
                          borderBottom: isActive ? `3px solid ${t.colorHex || '#1956D8'}` : '3px solid transparent',
                          color: isActive ? (t.colorHex || '#1956D8') : '#555',
                          fontWeight: isActive ? 700 : 400,
                          background: isActive ? '#f0f8ff' : 'transparent',
                        }}
                      >
                        <span style={{ display: 'inline-block', width: 10, height: 10, borderRadius: '50%', background: t.colorHex || '#1956D8', marginRight: 6, flexShrink: 0 }} />
                        {t.name}
                        <span style={{ marginLeft: 6, fontSize: 11, color: ok ? '#03BB83' : '#e74c3c', fontWeight: 700 }}>
                          {Number(t.totalPoints)}/10
                        </span>
                      </button>
                    )
                  })}
                </div>
              )}

              {activeTopic && (() => {
                  const topic = activeTopic
                  const totalOk = Number(topic.totalPoints) === 10
                  const form = questionForms[topic.id] || emptyQuestion
                  const treeForm = isDecisionTreeForm(form)
                  const tableForm = isDecisionTableForm(form)
                  return (
                    <article key={topic.id} style={{ ...styles.topicCard, borderTop: `4px solid ${topic.colorHex || '#1956D8'}`, marginTop: 0, borderRadius: '0 0 12px 12px' }}>
                      <div style={styles.topicHeader}>
                        <div>
                          <div style={styles.topicTitleRow}>
                            <span style={{ ...styles.topicSwatch, background: topic.colorHex || '#1956D8' }} />
                            {canEdit && editingTopicId === topic.id ? (
                              <form onSubmit={(e) => { e.preventDefault(); renameTopic(topic.id) }} style={styles.topicRenameForm}>
                                <input
                                  value={editingTopicName}
                                  onChange={(e) => setEditingTopicName(e.target.value)}
                                  style={styles.topicRenameInput}
                                  autoFocus
                                  required
                                />
                                <button type="submit" style={styles.topicRenameConfirm}>✓</button>
                                <button type="button" onClick={() => { setEditingTopicId(null); setEditingTopicName('') }} style={styles.topicRenameCancel}>✕</button>
                              </form>
                            ) : (
                              <div style={styles.topicTitleRow}>
                                <h3 style={styles.topicTitle}>{topic.name}</h3>
                                  {canEdit && (
                                    <>
                                      <button
                                        type="button"
                                        onClick={() => { setEditingTopicId(topic.id); setEditingTopicName(topic.name) }}
                                        style={styles.topicRenameBtn}
                                        title="Renombrar tema"
                                      >
                                        ✎
                                      </button>

                                      <button
                                        type="button"
                                        onClick={() => removeTopic(topic.id)}
                                        style={styles.deleteTopicBtn}
                                        title="Eliminar tema"
                                      >
                                        Eliminar tema
                                      </button>
                                    </>
                                  )}
                              </div>
                            )}
                          </div>
                          <span style={totalOk ? styles.totalOk : styles.totalPending}>
                            Total: {topic.totalPoints} / 10
                          </span>
                        </div>
                      </div>

                      <div style={styles.questions}>
                        {topic.questions.length === 0 && <p style={styles.muted}>Sin preguntas cargadas.</p>}
                        {topic.questions.map((question) => {
                          const editForm = editingQuestionForms[question.id]
                          const treeQuestion = isDecisionTreeQuestion(question)
                          const tableQuestion = isDecisionTableQuestion(question)
                          return (
                            <div key={question.id} style={styles.questionRow}>
                              {editForm ? (
                                <form onSubmit={(e) => updateQuestion(e, topic, question)} style={styles.editQuestionForm}>
                                  <div style={styles.inlineFields}>
                                    <label style={styles.label}>Puntos</label>
                                    <input
                                      type="number"
                                      step="0.25"
                                      min="0.25"
                                      max="10"
                                      value={editForm.points}
                                      onChange={(e) => updateEditingQuestionForm(question.id, 'points', e.target.value)}
                                      onInput={clearNumberInputValidity}
                                      onInvalid={setNumberInputValidity}
                                      style={styles.smallInput}
                                      required
                                    />
                                  </div>
                                  <label style={styles.label}>Enunciado</label>
                                  <AutoGrowTextarea
                                    value={editForm.prompt}
                                    onChange={(e) => updateEditingQuestionForm(question.id, 'prompt', e.target.value)}
                                    style={styles.promptTextarea}
                                    placeholder={questionDisplayTitle(question)}
                                    minHeight={220}
                                    maxHeight={1200}
                                  />
                                  <label style={styles.label}>Respuesta modelo</label>
                                  {isDecisionTableForm(editForm) ? (
                                        <div style={styles.practicalAnswerContainerLarge}>
                                          <DecisionTableEditor
                                            value={editForm.modelAnswer || emptyDecisionTableValue()}
                                            onChange={(value) => updateEditingQuestionForm(question.id, 'modelAnswer', value)}
                                            compact
                                          />
                                        </div>
                                      ) : isDecisionTreeForm(editForm) ? (
                                        <div style={styles.practicalAnswerContainerLarge}>
                                          <DecisionTreeEditor
                                            value={editForm.modelAnswer || emptyDecisionTreeValue()}
                                            onChange={(value) => updateEditingQuestionForm(question.id, 'modelAnswer', value)}
                                            compact
                                          />
                                        </div>
                                      ) : (
                                    <AutoGrowTextarea
                                      value={editForm.modelAnswer}
                                      onChange={(e) => updateEditingQuestionForm(question.id, 'modelAnswer', e.target.value)}
                                      style={styles.modelTextarea}
                                      placeholder="Respuesta modelo"
                                      minHeight={260}
                                      maxHeight={1400}
                                    />
                                  )}
                                  <div style={styles.editQuestionActions}>
                                    <button type="button" onClick={() => { cancelEditQuestion(question.id); removeQuestion(topic.id, question.id) }} style={styles.linkBtn}>Eliminar</button>
                                    <button type="button" onClick={() => cancelEditQuestion(question.id)} style={styles.secondaryBtn}>Cancelar</button>
                                    <button type="submit" style={styles.primaryBtn}>Guardar pregunta</button>
                                  </div>
                                </form>
                              ) : (
                                <>
                                  <div>
                                    <strong>{question.displayOrder}. {questionDisplayTitle(question)}</strong>
                                    {tableQuestion ? (
                                        <div style={styles.practicalAnswerContainer}>
                                          <DecisionTableEditor value={question.modelAnswer} readOnly compact />
                                        </div>
                                      ) : treeQuestion ? (
                                        <div style={styles.practicalAnswerContainer}>
                                          <DecisionTreeEditor value={question.modelAnswer} readOnly compact />
                                        </div>
                                      ) : (
                                      <p style={styles.answer}>Modelo: {question.modelAnswer || 'Sin completar'}</p>
                                    )}
                                  </div>
                                  <div style={styles.questionActions}>
                                    <span style={styles.points}>{question.points} pts</span>
                                    {canEdit && (
                                      <>
                                        <button onClick={() => startEditQuestion(question)} style={styles.editLinkBtn}>
                                          Editar
                                        </button>
                                        <button onClick={() => removeQuestion(topic.id, question.id)} style={styles.linkBtn}>
                                          Eliminar
                                        </button>
                                      </>
                                    )}
                                  </div>
                                </>
                              )}
                            </div>
                          )
                        })}
                      </div>

                      {canEdit && (
                        <form onSubmit={(e) => addQuestion(e, topic.id)} style={styles.questionForm}>
                          {topic.questions.length === 0 && (
                            <button
                              type="button"
                              onClick={() => loadDefaultTemplate(topic)}
                              style={styles.primaryBtn}
                              disabled={templateLoading === topic.id}
                            >
                              {templateLoading === topic.id ? 'Cargando plantilla...' : 'Cargar plantilla base 10 pts'}
                            </button>
                          )}
                          <div style={styles.templateActions}>
                            <button type="button" onClick={() => applyQuestionTemplate(topic.id, theoryTemplate)} style={styles.secondaryBtn}>Teorica 1 pto</button>
                            <button type="button" onClick={() => applyQuestionTemplate(topic.id, decisionTableTemplate)} style={styles.secondaryBtn}>Tabla 2 pts</button>
                            <button type="button" onClick={() => applyQuestionTemplate(topic.id, decisionTreeTemplate)} style={styles.secondaryBtn}>Arbol 2 pts</button>
                          </div>
                          <label style={styles.label}>Enunciado</label>
                          <AutoGrowTextarea
                            value={form.prompt}
                            onChange={(e) => updateQuestionForm(topic.id, 'prompt', e.target.value)}
                            style={styles.promptTextarea}
                            placeholder="Enunciado"
                            minHeight={220}
                            maxHeight={1200}
                          />
                          <label style={styles.label}>Respuesta modelo</label>
                          {tableForm ? (
                                <div style={styles.practicalAnswerContainerLarge}>
                                  <DecisionTableEditor
                                    value={form.modelAnswer || emptyDecisionTableValue()}
                                    onChange={(value) => updateQuestionForm(topic.id, 'modelAnswer', value)}
                                    compact
                                  />
                                </div>
                              ) : treeForm ? (
                                <div style={styles.practicalAnswerContainerLarge}>
                                  <DecisionTreeEditor
                                    value={form.modelAnswer || emptyDecisionTreeValue()}
                                    onChange={(value) => updateQuestionForm(topic.id, 'modelAnswer', value)}
                                    compact
                                  />
                                </div>
                              ) : (
                            <AutoGrowTextarea
                              value={form.modelAnswer}
                              onChange={(e) => updateQuestionForm(topic.id, 'modelAnswer', e.target.value)}
                              style={styles.modelTextarea}
                              placeholder="Respuesta modelo"
                              minHeight={260}
                              maxHeight={1400}
                            />
                          )}
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
                                onInput={clearNumberInputValidity}
                                onInvalid={setNumberInputValidity}
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
                })()}
            </>
          )}
        </section>
      </main>

      {timeoutPopup && (
        <div style={styles.modalOverlay} onClick={e => e.stopPropagation()}>
          <div style={{ ...styles.modalBox, maxWidth: 480 }}>
            <h3 style={styles.modalTitle}>⏰ Terminó el tiempo</h3>
            <p style={styles.modalText}>
              El tiempo del examen <strong>{timeoutPopup.examTitle}</strong> llegó a su fin.
              ¿Deseás agregar tiempo extra?
            </p>
            <p style={{ ...styles.modalText, color: '#9B2C2C', fontWeight: 700 }}>
              El examen se cerrará automáticamente en {Math.floor(popupCountdown / 60)}:{String(popupCountdown % 60).padStart(2, '0')}
            </p>
            <div style={{ display: 'flex', gap: 12, alignItems: 'center', flexWrap: 'wrap', marginTop: 8 }}>
              <button
                onClick={async () => {
                  const mins = Math.min(60, Math.max(1, Number(popupExtraMinutes) || 15))
                  try {
                    const res = await api.patch(`/exams/${timeoutPopup.examId}/add-extra-time`, { extraMinutes: mins })
                    replaceExam(res.data)
                  } catch (err) {
                    setMessage(err.response?.data?.message || 'No se pudo agregar tiempo.')
                  }
                  setTimeoutPopup(null)
                }}
                style={styles.primaryBtn}
              >
                Sí, agregar
              </button>
              <input
                type="number"
                min="1"
                max="60"
                value={popupExtraMinutes}
                onChange={e => setPopupExtraMinutes(e.target.value)}
                style={{ ...styles.smallInput, width: 70 }}
              />
              <span style={styles.muted}>min (máx. 60)</span>
            </div>
            <div style={{ marginTop: 16 }}>
              <button
                onClick={async () => {
                  try {
                    const res = await api.patch(`/exams/${timeoutPopup.examId}/close`)
                    replaceExam(res.data)
                  } catch {}
                  setTimeoutPopup(null)
                }}
                style={styles.closeBtn}
              >
                No, cerrar examen
              </button>
            </div>
          </div>
        </div>
      )}

      {gradingSubmission && (() => {
        const PROF_TOPIC_PASTEL = {
          '#2563eb': '#BFDBFE', '#16a34a': '#BBF7D0', '#d97706': '#FDE68A',
          '#dc2626': '#FECACA', '#7c3aed': '#DDD6FE', '#ea580c': '#FDBA74',
          '#0891b2': '#A5F3FC', '#65a30d': '#D9F99D', '#db2777': '#FBCFE8', '#0d9488': '#99F6E4',
        }
        const gradingExam = exams.find(e => e.id === gradingSubmission.examId)
        const gradingTopic = gradingExam?.topics?.find(t => t.id === gradingSubmission.topicId)
        const gradingPastel = PROF_TOPIC_PASTEL[gradingTopic?.colorHex?.toLowerCase()] || '#F4F8FA'
        return (
        <div style={styles.gradingOverlay}>
          <div style={styles.gradingPanel}>
            <div style={styles.gradingHeader}>
              <div>
                <h2 style={styles.gradingTitle}>{gradingSubmission.examTitle}</h2>
                <p style={styles.muted}>{gradingSubmission.studentName} · Tema: {gradingSubmission.topicName}</p>
              </div>
              <div style={{ display: 'flex', gap: 10, alignItems: 'center' }}>
                {message && <span style={{ color: '#087A55', fontSize: 13, fontWeight: 700 }}>{message}</span>}
                <button onClick={saveGrade} disabled={gradeSaving} style={gradeSaving ? styles.disabledBtn : styles.primaryBtn}>
                  {gradeSaving ? 'Guardando...' : 'Guardar'}
                </button>
                <button onClick={async () => { await saveGrade(); setGradingSubmission(null); setMessage('') }} style={styles.closeXBtn} title="Guardar y cerrar">✕</button>
              </div>
            </div>
            <div style={{ ...styles.gradingBody, background: gradingPastel }}>
              {gradingSubmission.questions.map((q, i) => {
                const gd = gradeData[q.questionId] || { score: '', comment: '' }
                const maxPts = Number(q.points)
                const isTree = q.interactionType === 'DECISION_TREE'
                const isTable = q.interactionType === 'DECISION_TABLE'
                return (
                  <div key={q.questionId} style={styles.gradeCard}>
                    <div style={styles.gradeCardHeader}>
                      <span style={styles.gradeQuestionNum}>Pregunta {q.displayOrder}</span>
                      <span style={{ color: '#1956D8', fontWeight: 800, fontSize: 13 }}>{maxPts} pts</span>
                    </div>
                    {q.prompt && <p style={styles.gradePrompt}>{q.prompt}</p>}
                    <div style={styles.gradeAnswerBox}>
                      <p style={styles.gradeAnswerLabel}>Respuesta del alumno:</p>
                      {isTable || isTree ? (
                        <p style={{ color: '#536B76', fontSize: 13, fontStyle: 'italic' }}>
                          {isTable ? '[Tabla de decisión — ver en vista del alumno]' : '[Árbol de decisión — ver en vista del alumno]'}
                        </p>
                      ) : (
                        <pre style={styles.gradeAnswerText}>{q.answerText || '(sin respuesta)'}</pre>
                      )}
                    </div>
                    <div style={styles.gradeInputRow}>
                      <div style={styles.gradeScoreBlock}>
                        <label style={styles.label}>Puntaje otorgado</label>
                        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                          <input
                            type="number"
                            min="0"
                            max={maxPts}
                            step="0.25"
                            value={gd.score}
                            onChange={e => setGradeData(prev => ({ ...prev, [q.questionId]: { ...gd, score: e.target.value } }))}
                            style={{ ...styles.smallInput, width: 90 }}
                          />
                          <span style={styles.muted}>/ {maxPts}</span>
                        </div>
                      </div>
                      <div style={{ flex: 1 }}>
                        <label style={styles.label}>Comentario de devolución</label>
                        <textarea
                          value={gd.comment}
                          onChange={e => setGradeData(prev => ({ ...prev, [q.questionId]: { ...gd, comment: e.target.value } }))}
                          style={{ ...styles.textarea, minHeight: 60, marginTop: 4 }}
                          placeholder="Feedback para el alumno..."
                          rows={2}
                        />
                      </div>
                    </div>
                  </div>
                )
              })}
              <div style={styles.gradeTotalRow}>
                <strong>Total calificado: </strong>
                <span style={{ color: '#1956D8', fontWeight: 800, fontSize: 16 }}>
                  {gradingSubmission.questions.reduce((sum, q) => {
                    const s = Number(gradeData[q.questionId]?.score || 0)
                    return sum + (isNaN(s) ? 0 : s)
                  }, 0).toFixed(2)} / {gradingSubmission.questions.reduce((sum, q) => sum + Number(q.points), 0).toFixed(2)}
                </span>
              </div>
            </div>
          </div>
        </div>
      )
      })()}

      {modal && (
        <div style={styles.modalOverlay} onClick={() => setModal(null)}>
          <div style={styles.modalBox} onClick={(e) => e.stopPropagation()}>
            {modal.type === 'pastDate' ? (
              <>
                <h3 style={styles.modalTitle}>⚠️ Fecha y hora ya pasaron</h3>
                <p style={styles.modalText}>
                  La fecha programada ({new Date(modal.isoDate).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' })}) ya pasó.
                  No podés publicar con una hora anterior a la actual.
                </p>
                <p style={styles.modalText}>
                  Actualizá la fecha y hora en <strong>Datos del borrador</strong> antes de publicar.
                </p>
                <div style={styles.modalActions}>
                  <button onClick={() => setModal(null)} style={styles.primaryBtn}>Entendido</button>
                </div>
              </>
            ) : modal.type === 'confirmPublish' ? (
              <>
                <h3 style={styles.modalTitle}>Publicar examen</h3>
                <p style={styles.modalText}>
                  Al publicar, el examen estará disponible para los alumnos y <strong>no podrás volver a borrador</strong>.
                </p>
                <div style={styles.modalActions}>
                  <button onClick={() => setModal(null)} style={styles.secondaryBtn}>Cancelar</button>
                  <button onClick={() => { setModal(null); publishExam() }} style={styles.primaryBtn}>Publicar</button>
                </div>
              </>
            ) : modal.type === 'confirmClose' ? (
              <>
                <h3 style={styles.modalTitle}>Cerrar examen</h3>
                <p style={styles.modalText}>
                  Al cerrar, <strong>no se aceptarán nuevas entregas</strong>. Los alumnos que ya iniciaron podrán seguir respondiendo.
                </p>
                <div style={styles.modalActions}>
                  <button onClick={() => setModal(null)} style={styles.secondaryBtn}>Cancelar</button>
                  <button onClick={() => { setModal(null); closeExam() }} style={styles.closeBtn}>Cerrar examen</button>
                </div>
              </>
            ) : modal.type === 'badPoints' ? (
              <>
                <h3 style={styles.modalTitle}>No se puede publicar todavía</h3>
                <p style={styles.modalText}>Los siguientes temas no suman exactamente 10 puntos:</p>
                <ul style={styles.modalList}>
                  {modal.topics.map((t) => (
                    <li key={t.id}><strong>{t.name}</strong>: {t.totalPoints} / 10 pts</li>
                  ))}
                </ul>
                <div style={styles.modalActions}>
                  <button onClick={() => setModal(null)} style={styles.secondaryBtn}>Revisar manualmente</button>
                  <button onClick={redistributeAndPublish} style={styles.primaryBtn}>Redistribuir y publicar</button>
                </div>
              </>
            ) : modal.type === 'confirmDelete' ? (
              <>
                <h3 style={styles.modalTitle}>Eliminar examen</h3>
                <p style={styles.modalText}>
                  ¿Confirmas que querés eliminar <strong>{modal.examTitle}</strong>? Esta acción no se puede deshacer.
                </p>
                <div style={styles.modalActions}>
                  <button onClick={() => setModal(null)} style={styles.secondaryBtn}>Cancelar</button>
                  <button onClick={() => deleteExam(modal.examId)} style={styles.closeBtn}>Eliminar</button>
                </div>
              </>
            ) : (
              <>
                <h3 style={styles.modalTitle}>Faltan respuestas modelo</h3>
                <p style={styles.modalText}>Las siguientes preguntas no tienen respuesta modelo completa:</p>
                <ul style={styles.modalList}>
                  {modal.items.map((item, i) => (
                    <li key={i}>
                      <strong>{item.topicName} · Pregunta {item.order}</strong>
                      {item.prompt ? `: ${item.prompt.slice(0, 80)}${item.prompt.length > 80 ? '…' : ''}` : ' (sin enunciado)'}
                    </li>
                  ))}
                </ul>
                <div style={styles.modalActions}>
                  <button onClick={() => setModal(null)} style={styles.primaryBtn}>Entendido, voy a completarlas</button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  )
}

const fStyles = {
  landingCards: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(260px, 1fr))', gap: 20, padding: '40px', maxWidth: 1200, margin: '0 auto' },
  navCard: { background: '#fff', borderRadius: 12, padding: '28px 24px', boxShadow: '0 2px 8px rgba(0,0,0,0.07)', display: 'flex', flexDirection: 'column', gap: 8, transition: 'box-shadow .15s' },
  navCardTitle: { fontSize: 20, fontWeight: 800, margin: 0 },
  navCardSub: { fontSize: 13, color: '#6B7280', margin: 0 },
  navCardDivider: { borderTop: '1px solid #E5E7EB', margin: '12px 0' },
  navCardStats: { display: 'flex', gap: 24 },
  navStat: { display: 'flex', flexDirection: 'column', gap: 2 },
  navCardArrow: { fontSize: 20, color: '#9CA3AF', textAlign: 'right', marginTop: 'auto', fontWeight: 700 },
  backLink: { background: 'none', border: 'none', color: '#6B7280', fontSize: 14, fontWeight: 700, cursor: 'pointer', padding: '0 0 16px', display: 'flex', alignItems: 'center', gap: 6 },
  pageHeaderRow: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 24 },
  pageTitle: { fontSize: 28, fontWeight: 800, margin: '0 0 4px', color: '#09222A' },
  pageSubtitle: { fontSize: 14, color: '#6B7280', margin: 0 },
  createBtn: { display: 'flex', alignItems: 'center', gap: 8, padding: '12px 20px', background: '#09222A', color: '#fff', border: 'none', borderRadius: 10, fontSize: 15, fontWeight: 700, cursor: 'pointer' },
  tableCard: { background: '#fff', borderRadius: 12, boxShadow: '0 2px 8px rgba(0,0,0,0.07)', overflow: 'hidden' },
  table: { width: '100%', borderCollapse: 'collapse' },
  th: { padding: '14px 20px', textAlign: 'left', fontSize: 13, fontWeight: 700, color: '#374151', borderBottom: '2px solid #E5E7EB' },
  tr: { borderBottom: '1px solid #F3F4F6' },
  td: { padding: '14px 20px', fontSize: 14, color: '#09222A' },
  editBtn: { background: 'none', border: 'none', color: '#1956D8', fontWeight: 700, cursor: 'pointer', fontSize: 14 },
  deleteBtn: { background: 'none', border: 'none', color: '#9CA3AF', cursor: 'pointer', fontSize: 16 },
  emptyBox: { padding: '48px', textAlign: 'center' },
  formCard: { background: '#fff', borderRadius: 12, padding: '28px 24px', boxShadow: '0 2px 8px rgba(0,0,0,0.07)' },
  // Running exam
  runningMain: { display: 'grid', gridTemplateColumns: '1fr 340px', gap: 28, padding: '32px 40px', maxWidth: 1200, margin: '0 auto' },
  runningLeft: {},
  runningRight: { display: 'flex', flexDirection: 'column', gap: 16, alignSelf: 'start', position: 'sticky', top: 24 },
  finalizeBtn: { padding: '16px', background: '#DC2626', color: '#fff', border: 'none', borderRadius: 12, fontSize: 18, fontWeight: 800, cursor: 'pointer', textAlign: 'center', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10 },
  iniciarBtn: { padding: '16px', background: '#09222A', color: '#fff', border: 'none', borderRadius: 12, fontSize: 18, fontWeight: 800, cursor: 'pointer', textAlign: 'center' },
  examFinishedBtn: { padding: '16px', background: '#6B7280', color: '#fff', border: 'none', borderRadius: 12, fontSize: 18, fontWeight: 700, textAlign: 'center' },
  examScheduledBtn: { padding: '16px', background: '#FEF3C7', color: '#92400E', border: '1px solid #F0D36A', borderRadius: 12, fontSize: 18, fontWeight: 800, textAlign: 'center' },
  timerWidget: { background: '#fff', borderRadius: 12, padding: '20px 24px', boxShadow: '0 2px 8px rgba(0,0,0,0.07)', textAlign: 'center' },
  timerLabel: { fontSize: 12, fontWeight: 800, color: '#374151', textTransform: 'uppercase', letterSpacing: '0.05em', margin: '0 0 8px' },
  timerValue: { fontSize: 44, fontWeight: 800, margin: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10 },
  countersWidget: { background: '#fff', borderRadius: 12, padding: '16px 24px', boxShadow: '0 2px 8px rgba(0,0,0,0.07)', display: 'flex', justifyContent: 'space-around' },
  counterItem: { display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 },
}

const lStyles = {
  landingMain: { display: 'grid', gridTemplateColumns: '360px 1fr', gap: 32, padding: '32px 40px', maxWidth: 1400, margin: '0 auto' },
  detailMain: { padding: '24px 40px', maxWidth: 1200, margin: '0 auto', width: '100%', boxSizing: 'border-box' },
  createCard: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 12, padding: '24px 22px', alignSelf: 'start', position: 'sticky', top: 24 },
  createTitle: { fontSize: 18, fontWeight: 800, color: '#1956D8', margin: '0 0 18px' },
  createForm: { display: 'flex', flexDirection: 'column', gap: 8 },
  examPanel: { minWidth: 0 },
  examPanelTitle: { fontSize: 22, fontWeight: 800, color: '#09222A', margin: '0 0 20px' },
  groupsContainer: { display: 'flex', flexDirection: 'column', gap: 24 },
  group: {},
  groupHeader: { display: 'flex', alignItems: 'center', gap: 10, borderLeft: '3px solid #ccc', paddingLeft: 10, marginBottom: 10 },
  groupLabel: { fontSize: 13, fontWeight: 800, textTransform: 'uppercase', letterSpacing: '0.05em' },
  groupCount: { fontSize: 12, fontWeight: 700, color: '#7B919B', background: '#F0F4F6', borderRadius: 999, padding: '1px 8px' },
  examCards: { display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(220px, 1fr))', gap: 12 },
  examCard: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 10, padding: '14px 16px', textAlign: 'left', cursor: 'pointer', display: 'flex', flexDirection: 'column', gap: 6, transition: 'box-shadow .15s, border-color .15s', boxShadow: '0 1px 4px rgba(9,34,42,0.06)' },
  examCardTitle: { fontSize: 14, fontWeight: 700, color: '#09222A', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' },
  examCardMeta: { fontSize: 12, color: '#536B76' },
  backBtn: { background: 'none', border: 'none', color: 'rgba(203,238,243,0.85)', fontSize: 14, fontWeight: 700, cursor: 'pointer', padding: '0 12px 0 0', display: 'flex', alignItems: 'center', gap: 4, flexShrink: 0 },
  backBtnSide: { background: 'none', border: 'none', color: '#1956D8', fontSize: 13, fontWeight: 700, cursor: 'pointer', padding: '4px 0', display: 'block' },
}

function isGraded(submission) {
  return submission.questions?.some(q => q.score != null)
}

function nextTopicLetter(topics) {
  return String.fromCharCode(65 + Math.min((topics || []).length, 25))
}

function isoToDate(isoString) {
  if (!isoString) return ''
  const d = new Date(isoString)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function isoToTime(isoString) {
  if (!isoString) return ''
  const d = new Date(isoString)
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function isValidTime(t) {
  return /^([01]\d|2[0-3]):[0-5]\d$/.test(t)
}

function todayStr() {
  const d = new Date()
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function combineDateTime(date, time) {
  if (!date) return null
  if (!time) return null
  if (!isValidTime(time)) return null
  return new Date(`${date}T${time}:00`).toISOString()
}

function scheduleIsoFromForm(form) {
  const hasDate = Boolean(form.availableDate)
  const hasTime = Boolean(form.availableTime)
  if (!hasDate && !hasTime) return null
  if (!hasDate || !hasTime) {
    throw new Error('Completa fecha y hora de inicio, o deja ambos campos vacios.')
  }
  if (!isValidTime(form.availableTime)) {
    throw new Error('La hora de inicio es invalida. Usa formato HH:MM.')
  }
  return combineDateTime(form.availableDate, form.availableTime)
}

function formatScheduleDateTime(isoString) {
  if (!isoString) return 'fecha a definir'
  return new Date(isoString).toLocaleString('es-AR', { dateStyle: 'short', timeStyle: 'short' })
}

function publishSuccessMessage(exam) {
  if (derivedStatus(exam) === 'PROGRAMADO') {
    return `Examen programado. Estara disponible el ${formatScheduleDateTime(exam.availableFrom)}.`
  }
  return 'Examen publicado. Los alumnos ya pueden iniciarlo.'
}

function formatProfTime(seconds) {
  const s = Math.max(0, seconds)
  const h = Math.floor(s / 3600)
  const m = Math.floor((s % 3600) / 60)
  const sec = s % 60
  const pad = (n) => String(n).padStart(2, '0')
  if (h > 0) return `${h}:${pad(m)}:${pad(sec)}`
  return `${pad(m)}:${pad(sec)}`
}

function clearNumberInputValidity(event) {
  event.currentTarget.setCustomValidity('')
}

function setNumberInputValidity(event) {
  const input = event.currentTarget
  if (input.validity.rangeOverflow) {
    input.setCustomValidity(`El valor debe ser menor o igual a ${input.max}.`)
  } else if (input.validity.rangeUnderflow) {
    input.setCustomValidity(`El valor debe ser mayor o igual a ${input.min}.`)
  } else if (input.validity.stepMismatch) {
    input.setCustomValidity(`Usá incrementos de ${input.step}.`)
  } else if (input.validity.valueMissing) {
    input.setCustomValidity('Completá este campo.')
  } else {
    input.setCustomValidity('')
  }
}

function derivedStatus(exam) {
  if (!exam) return 'BORRADOR'
  if (exam.status === 'BORRADOR') return 'BORRADOR'
  if (exam.status === 'CERRADO') return 'CERRADO'
  if (exam.status === 'PUBLICADO') {
    if (exam.availableFrom && new Date(exam.availableFrom).getTime() > Date.now()) return 'PROGRAMADO'
    return 'EN_CURSO'
  }
  return exam.status
}

function labelStatus(examOrStatus) {
  const s = typeof examOrStatus === 'string' ? examOrStatus : derivedStatus(examOrStatus)
  return { BORRADOR: 'Borrador', PUBLICADO: 'Publicado', EN_CURSO: 'En curso', PROGRAMADO: 'Programado', CERRADO: 'Cerrado' }[s] || s
}

function statusStyle(examOrStatus) {
  const s = typeof examOrStatus === 'string' ? examOrStatus : derivedStatus(examOrStatus)
  const base = { display: 'inline-flex', alignItems: 'center', height: 24, padding: '0 10px', borderRadius: 999, fontSize: 12, fontWeight: 700 }
  if (s === 'EN_CURSO')   return { ...base, background: '#DDF6EC', color: '#087A55' }
  if (s === 'PUBLICADO')  return { ...base, background: '#DDF6EC', color: '#087A55' }
  if (s === 'PROGRAMADO') return { ...base, background: '#FEF3C7', color: '#92400E' }
  if (s === 'CERRADO')    return { ...base, background: '#ECEFF3', color: '#4A5565' }
  return { ...base, background: '#E6EEFF', color: '#1956D8' }
}


function isBlankModelAnswer(answer) {
  if (!answer || answer.trim() === '') return true
  const trimmed = answer.trim()
  if (isDecisionTreeValue(trimmed)) {
    try {
      const data = JSON.parse(trimmed.slice(DECISION_TREE_PREFIX.length))
      const hasNodeText = (data.nodes || []).some((n) => n.text && n.text.trim())
      const hasEdgeLabel = (data.edges || []).some((e) => e.label && e.label.trim())
      return !hasNodeText && !hasEdgeLabel
    } catch { return true }
  }
  if (isDecisionTableValue(trimmed)) {
    try {
      const data = JSON.parse(trimmed.slice(DECISION_TABLE_PREFIX.length))
      return (data.cells || []).every((row) => row.every((cell) => !String(cell).trim()))
    } catch { return true }
  }
  return false
}

function findMissingAnswers(exam) {
  const missing = []
  for (const topic of exam.topics || []) {
    for (const question of topic.questions || []) {
      if (isBlankModelAnswer(question.modelAnswer)) {
        missing.push({ topicName: topic.name, order: question.displayOrder, prompt: question.prompt })
      }
    }
  }
  return missing
}

function topicExceedsLimit(topic, nextPoints, editingQuestionId = null) {
  if (!topic || Number.isNaN(nextPoints)) return false
  const currentTotal = topic.questions.reduce((sum, question) => {
    if (question.id === editingQuestionId) return sum
    return sum + Number(question.points || 0)
  }, 0)
  return currentTotal + nextPoints > 10
}

function normalizedModelAnswer(form) {
  if (isDecisionTableForm(form) && !isDecisionTableValue(form.modelAnswer)) {
    return emptyDecisionTableValue()
  }
  if (isDecisionTreeForm(form) && !isDecisionTreeValue(form.modelAnswer)) {
    return emptyDecisionTreeValue()
  }
  return form.modelAnswer || ''
}

function initialEditorValue(question) {
  if (isDecisionTableQuestion(question) && !isDecisionTableValue(question.modelAnswer)) {
    return emptyDecisionTableValue()
  }
  if (isDecisionTreeQuestion(question) && !isDecisionTreeValue(question.modelAnswer)) {
    return emptyDecisionTreeValue()
  }
  return question.modelAnswer
}

function isDecisionTableForm(form = {}) {
  return isDecisionTableValue(form.modelAnswer) || isDecisionTablePrompt(form.prompt)
}

function isDecisionTreeForm(form = {}) {
  return isDecisionTreeValue(form.modelAnswer) || isDecisionTreePrompt(form.prompt)
}

function isDecisionTableQuestion(question = {}) {
  return isDecisionTableValue(question.modelAnswer)
    || isDecisionTablePrompt(question.prompt)
    || (Number(question.points) === 2 && question.displayOrder === 7 && !isDecisionTreeQuestion(question))
}

function isDecisionTreeQuestion(question = {}) {
  return isDecisionTreeValue(question.modelAnswer) || isDecisionTreePrompt(question.prompt)
}

function questionDisplayTitle(question) {
  const prompt = question.prompt?.trim()
  if (prompt) return prompt
  if (isDecisionTableQuestion(question)) return 'Practico - Tabla de decision'
  if (isDecisionTreeQuestion(question)) return 'Practico - Arbol de decision'
  if (Number(question.points) === 2 && question.displayOrder === 7) return 'Practico - Tabla de decision'
  if (Number(question.points) === 1 && question.displayOrder <= 6) return `Teorica ${question.displayOrder}`
  return 'Enunciado sin completar'
}

const styles = {
  page: { minHeight: '100vh', background: '#F4F8FA', color: '#09222A' },
  header: { background: 'linear-gradient(120deg, #09222A 0%, #1956D8 100%)', color: '#fff', padding: '16px 32px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' },
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
  promptTextarea: { minHeight: 118, padding: '10px 12px', border: '1px solid #C9DDE3', borderRadius: 6, fontSize: 14, color: '#09222A', resize: 'vertical', boxSizing: 'border-box', width: '100%', fontFamily: 'inherit', lineHeight: 1.5 },
  modelTextarea: { minHeight: 220, padding: '10px 12px', border: '1px solid #C9DDE3', borderRadius: 6, fontSize: 14, color: '#09222A', resize: 'vertical', boxSizing: 'border-box', width: '100%', fontFamily: 'inherit', lineHeight: 1.5 },
  primaryBtn: { minHeight: 38, padding: '8px 16px', background: '#1956D8', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  secondaryBtn: { minHeight: 38, padding: '8px 14px', background: '#fff', color: '#1956D8', border: '1px solid #1956D8', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  disabledBtn: { minHeight: 38, padding: '8px 16px', background: '#C9DDE3', color: '#536B76', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 700 },
  closeBtn: { minHeight: 38, padding: '8px 16px', background: '#fff', color: '#9B2C2C', border: '1px solid #9B2C2C', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  examGroup: { marginBottom: 10 },
  examGroupLabel: { fontSize: 11, fontWeight: 800, color: '#7B919B', textTransform: 'uppercase', letterSpacing: '0.06em', margin: '10px 0 4px', paddingLeft: 4 },
  examItem: { width: '100%', borderBottom: '1px solid #E7F0F3', background: '#fff', padding: '8px 4px', display: 'flex', alignItems: 'center', gap: 4 },
  examItemActive: { width: '100%', borderBottom: '1px solid #E7F0F3', background: '#F0F5FF', padding: '8px 8px', display: 'flex', alignItems: 'center', gap: 4, borderRadius: 6 },
  examItemSelect: { flex: 1, border: 'none', background: 'none', padding: '4px 0', display: 'flex', justifyContent: 'space-between', alignItems: 'center', cursor: 'pointer', textAlign: 'left', minWidth: 0 },
  examItemTitle: { fontSize: 14, fontWeight: 700, color: '#09222A', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: 155 },
  deleteExamBtn: { flexShrink: 0, width: 24, height: 24, borderRadius: 6, border: '1px solid #9B2C2C', background: '#fff', color: '#9B2C2C', fontWeight: 800, fontSize: 11, cursor: 'pointer' },
  workspace: { minWidth: 0 },
  message: { background: '#FFF8DF', border: '1px solid #E7CE74', color: '#5D4700', padding: '10px 12px', borderRadius: 8, marginBottom: 14, fontSize: 14 },
  emptyState: { background: '#fff', border: '1px dashed #B9CDD3', borderRadius: 8, padding: 24, color: '#536B76', textAlign: 'center' },
  examHeader: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 20, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 16, marginBottom: 16 },
  examTitle: { fontSize: 24, fontWeight: 800, margin: '0 0 6px' },
  examMeta: { fontSize: 14, color: '#536B76', margin: 0 },
  headerActions: { display: 'flex', alignItems: 'center', gap: 10 },
  editBox: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 16, marginBottom: 16 },
  editTitle: { fontSize: 16, fontWeight: 800, color: '#1956D8', margin: '0 0 12px' },
  editGrid: { display: 'grid', gridTemplateColumns: 'minmax(220px, 1fr) minmax(260px, 2fr)', gap: 12 },
  fieldBlock: { display: 'flex', flexDirection: 'column', gap: 6 },
  editActions: { display: 'flex', justifyContent: 'flex-end', marginTop: 12 },
  topicForm: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 14, display: 'grid', gridTemplateColumns: '1fr auto', gap: 10, marginBottom: 16 },
  topicGrid: { display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))', gap: 16 },
  topicTabs: { display: 'flex', flexWrap: 'wrap', gap: 0, borderBottom: '1px solid #D8E8EC', marginBottom: 0 },
  topicTab: { display: 'flex', alignItems: 'center', padding: '9px 18px', cursor: 'pointer', border: 'none', borderRadius: '8px 8px 0 0', fontSize: 14, transition: 'all .15s', whiteSpace: 'nowrap' },
  topicCard: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 16 },
  topicHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 },
  topicTitleRow: { display: 'flex', alignItems: 'center', gap: 8, marginBottom: 6 },
  topicSwatch: { width: 12, height: 12, borderRadius: 999, flex: '0 0 auto' },
  topicTitle: { fontSize: 18, margin: 0 },
  topicRenameBtn: { background: 'none', border: 'none', color: '#536B76', fontSize: 14, cursor: 'pointer', padding: '0 2px', lineHeight: 1 },
  topicRenameForm: { display: 'flex', alignItems: 'center', gap: 4 },
  topicRenameInput: { fontSize: 16, fontWeight: 700, border: '1px solid #1956D8', borderRadius: 4, padding: '2px 6px', width: 140 },
  topicRenameConfirm: { background: '#1956D8', color: '#fff', border: 'none', borderRadius: 4, padding: '2px 8px', cursor: 'pointer', fontWeight: 700 },
  topicRenameCancel: { background: '#fff', color: '#9B2C2C', border: '1px solid #9B2C2C', borderRadius: 4, padding: '2px 8px', cursor: 'pointer', fontWeight: 700 },
  totalOk: { color: '#087A55', fontSize: 13, fontWeight: 800 },
  totalPending: { color: '#9B6A00', fontSize: 13, fontWeight: 800 },
  questions: { display: 'flex', flexDirection: 'column', gap: 10 },
  questionRow: {
    border: '1px solid #E7F0F3',
    borderRadius: 6,
    padding: 10,
    display: 'grid',
    gridTemplateColumns: 'minmax(0, 1fr) auto',
    gap: 12,
    overflow: 'hidden',
  },
  answer: { margin: '6px 0 0', color: '#536B76', fontSize: 13, lineHeight: 1.4, whiteSpace: 'pre-wrap' },
  questionActions: { display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 6 },
  points: { color: '#1956D8', fontWeight: 800, fontSize: 13 },
  editLinkBtn: { background: 'none', border: 'none', color: '#1956D8', fontWeight: 700, cursor: 'pointer', fontSize: 13 },
  linkBtn: { background: 'none', border: 'none', color: '#9B2C2C', fontWeight: 700, cursor: 'pointer', fontSize: 13 },
  questionForm: { marginTop: 14, borderTop: '1px solid #E7F0F3', paddingTop: 14, display: 'flex', flexDirection: 'column', gap: 8 },
  editQuestionForm: { gridColumn: '1 / -1', display: 'flex', flexDirection: 'column', gap: 8 },
  editQuestionActions: { display: 'flex', justifyContent: 'flex-end', gap: 10 },
  templateActions: { display: 'flex', flexWrap: 'wrap', gap: 8, marginBottom: 4 },
  inlineFields: { display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', gap: 12 },
  muted: { color: '#536B76', fontSize: 14, margin: 0 },
  fieldError: { color: '#9B2C2C', fontSize: 12, fontWeight: 600 },
  submissionPanel: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 16, marginBottom: 16 },
  submissionHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, marginBottom: 12 },
  submissionTitle: { fontSize: 17, margin: 0 },
  submissionList: { display: 'flex', flexDirection: 'column', gap: 8 },
  submissionRow: { border: '1px solid #E7F0F3', borderRadius: 6, padding: 10, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 },
  gradeBtn: { minHeight: 28, padding: '4px 10px', background: '#fff', color: '#7C3AED', border: '1px solid #7C3AED', borderRadius: 6, fontSize: 12, fontWeight: 700, cursor: 'pointer' },
  publishFeedbackBtn: { minHeight: 30, padding: '5px 14px', background: '#087A55', color: '#fff', border: 'none', borderRadius: 6, fontSize: 12, fontWeight: 800, cursor: 'pointer' },
  feedbackPublishedBadge: { background: '#087A55', color: '#fff', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
  closeXBtn: { width: 36, height: 36, borderRadius: '50%', border: '1px solid rgba(203,238,243,0.4)', background: 'rgba(203,238,243,0.1)', color: '#CBEEF3', fontSize: 16, fontWeight: 800, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 },
  gradingOverlay: { position: 'fixed', inset: 0, zIndex: 2000, display: 'flex', alignItems: 'stretch' },
  gradingPanel: { width: '100%', background: '#F4F8FA', display: 'flex', flexDirection: 'column' },
  gradingHeader: { background: '#09222A', color: '#fff', padding: '16px 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' },
  gradingTitle: { fontSize: 18, fontWeight: 800, margin: '0 0 4px', color: '#fff' },
  gradingBody: { flex: 1, overflowY: 'auto', padding: '20px 24px', display: 'flex', flexDirection: 'column', gap: 16 },
  gradeCard: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 16 },
  gradeCardHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 },
  gradeQuestionNum: { fontSize: 13, fontWeight: 800, color: '#304653' },
  gradePrompt: { fontSize: 16, fontWeight: 700, color: '#09222A', margin: '0 0 10px', lineHeight: 1.4 },
  gradeAnswerBox: { background: '#F4F8FA', borderRadius: 6, padding: '10px 12px', marginBottom: 12 },
  gradeAnswerLabel: { fontSize: 11, fontWeight: 700, color: '#536B76', margin: '0 0 6px', textTransform: 'uppercase', letterSpacing: '0.04em' },
  gradeAnswerText: { margin: 0, fontSize: 13, color: '#09222A', whiteSpace: 'pre-wrap', fontFamily: 'inherit', lineHeight: 1.5 },
  gradeInputRow: { display: 'flex', gap: 16, alignItems: 'flex-start' },
  gradeScoreBlock: { display: 'flex', flexDirection: 'column', gap: 4, minWidth: 130 },
  gradeTotalRow: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: '12px 16px', textAlign: 'right', fontSize: 15 },
  gradedBadge: { background: '#087A55', color: '#fff', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
  submittedBadge: { background: '#DDF6EC', color: '#087A55', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
  progressBadge: { background: '#E6EEFF', color: '#1956D8', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
  exceededBadge: { background: '#FDECEA', color: '#9B2C2C', padding: '3px 8px', borderRadius: 999, fontSize: 11, fontWeight: 800 },
  timerProf: { background: '#E6EEFF', color: '#1956D8', padding: '5px 12px', borderRadius: 8, fontSize: 13, fontWeight: 800 },
  timerExpiredProf: { background: '#FDECEA', color: '#9B2C2C', padding: '5px 12px', borderRadius: 8, fontSize: 13, fontWeight: 800 },
  submissionCounters: { display: 'flex', gap: 8, flexWrap: 'wrap', marginBottom: 12 },
  counterBadgeProgress: { background: '#E6EEFF', color: '#1956D8', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 700 },
  counterBadgeDone: { background: '#DDF6EC', color: '#087A55', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 700 },
  counterBadgeTotal: { background: '#F0F0F0', color: '#536B76', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 700 },
  modalOverlay: { position: 'fixed', inset: 0, background: 'rgba(9,34,42,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 },
  modalBox: { background: '#fff', borderRadius: 12, padding: '28px 32px', maxWidth: 520, width: '90%', boxShadow: '0 8px 40px rgba(9,34,42,0.22)' },
  modalTitle: { fontSize: 18, fontWeight: 800, margin: '0 0 10px', color: '#09222A' },
  modalText: { fontSize: 14, color: '#304653', margin: '0 0 8px' },
  modalList: { margin: '0 0 18px 20px', padding: 0, fontSize: 14, color: '#09222A', lineHeight: 2 },
  modalActions: { display: 'flex', gap: 10, justifyContent: 'flex-end', flexWrap: 'wrap' },
practicalAnswerContainer: {
  width: '100%',
  maxWidth: '100%',
  height: 220,
  overflow: 'auto',
  boxSizing: 'border-box',
  border: '1px solid #D8E8EC',
  borderRadius: 8,
  background: '#EEF5F7',
  marginTop: 8,
},

practicalAnswerContainerLarge: {
  width: '100%',
  maxWidth: '100%',
  height: 680,
  overflow: 'auto',
  boxSizing: 'border-box',
  border: '1px solid #D8E8EC',
  borderRadius: 8,
  background: '#EEF5F7',
  marginTop: 8,
},
deleteTopicBtn: {
  minHeight: 30,
  padding: '5px 10px',
  background: '#fff',
  color: '#9B2C2C',
  border: '1px solid #9B2C2C',
  borderRadius: 6,
  fontSize: 12,
  fontWeight: 700,
  cursor: 'pointer',
},
}
