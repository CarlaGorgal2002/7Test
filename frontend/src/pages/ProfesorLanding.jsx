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

const emptyExam = { title: '', description: '', courseName: 'Testing de Aplicaciones', durationMinutes: 120 }
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
  const [topicName, setTopicName] = useState('Tema A')
  const [questionForms, setQuestionForms] = useState({})
  const [editingQuestionForms, setEditingQuestionForms] = useState({})
  const [editingTopicId, setEditingTopicId] = useState(null)
  const [editingTopicName, setEditingTopicName] = useState('')
  const [templateLoading, setTemplateLoading] = useState('')
  const [modal, setModal] = useState(null)
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

  useEffect(() => {
    if (!selectedExam) return
    setEditExamForm({
      title: selectedExam.title || '',
      description: selectedExam.description || '',
      courseName: selectedExam.courseName || 'Testing de Aplicaciones',
      durationMinutes: selectedExam.durationMinutes || 120,
    })
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
      const previousTopicIds = new Set((selectedExam.topics || []).map((topic) => topic.id))
      const res = await api.post(`/exams/${selectedExam.id}/topics`, { name: topicName })
      let updated = res.data
      const createdTopic = updated.topics?.find((topic) => !previousTopicIds.has(topic.id))
      replaceExam(updated)
      if (createdTopic) {
        setTemplateLoading(createdTopic.id)
        updated = await appendDefaultTemplate(updated, createdTopic.id)
        replaceExam(updated)
      }
      setTopicName(nextTopicName(res.data.topics.length + 1))
      setMessage('Tema agregado con 6 teoricas vacias, tabla vacia y arbol vacio.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo agregar el tema o cargar la plantilla.')
    } finally {
      setTemplateLoading('')
    }
  }

  async function renameTopic(topicId) {
    if (!selectedExam || !editingTopicName.trim()) return
    setMessage('')
    try {
      const res = await api.put(`/exams/${selectedExam.id}/topics/${topicId}`, { name: editingTopicName.trim() })
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
    try {
      const res = await api.put(`/exams/${selectedExam.id}`, {
        title: editExamForm.title,
        description: editExamForm.description,
        courseName: editExamForm.courseName || selectedExam.courseName || 'Testing de Aplicaciones',
        durationMinutes: Number(editExamForm.durationMinutes || selectedExam.durationMinutes) || null,
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
      const res = await api.patch(`/exams/${selectedExam.id}/publish`)
      replaceExam(res.data)
      setMessage('Examen publicado. Los alumnos ya pueden iniciarlo.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo publicar el examen.')
    }
  }

  function handlePublishClick() {
    if (!selectedExam) return
    const missingAnswers = findMissingAnswers(selectedExam)
    if (missingAnswers.length > 0) {
      setModal({ type: 'missingAnswers', items: missingAnswers })
      return
    }
    const badTopics = (selectedExam.topics || []).filter((t) => Number(t.totalPoints) !== 10)
    if (badTopics.length > 0) {
      setModal({ type: 'badPoints', topics: badTopics })
    } else {
      publishExam()
    }
  }

  async function redistributeAndPublish() {
    const topicsToFix = modal?.topics || []
    setModal(null)
    setMessage('')
    try {
      let exam = selectedExam
      for (const topic of exam.topics.filter((t) => topicsToFix.some((bt) => bt.id === t.id))) {
        exam = await redistributeTopicPoints(exam, topic)
        replaceExam(exam)
      }
      const res = await api.patch(`/exams/${exam.id}/publish`)
      replaceExam(res.data)
      setMessage('Examen publicado. Los alumnos ya pueden iniciarlo.')
    } catch (err) {
      setMessage(err.response?.data?.message || 'No se pudo publicar el examen.')
    }
  }

  async function redistributeTopicPoints(exam, topic) {
    const n = topic.questions.length
    if (n === 0) return exam
    const base = Math.floor((10 / n) / 0.25) * 0.25
    const remainder = parseFloat((10 - base * n).toFixed(2))
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
              <div key={exam.id} style={exam.id === selectedExam?.id ? styles.examItemActive : styles.examItem}>
                <button
                  onClick={() => setSelectedId(exam.id)}
                  style={styles.examItemSelect}
                >
                  <span style={styles.examItemTitle}>{exam.title}</span>
                  <span style={statusStyle(exam.status)}>{labelStatus(exam.status)}</span>
                </button>
                {(exam.status === 'BORRADOR' || exam.status === 'CERRADO') && (
                  <button
                    onClick={() => setModal({ type: 'confirmDelete', examId: exam.id, examTitle: exam.title })}
                    style={styles.deleteExamBtn}
                    title="Eliminar examen"
                  >✕</button>
                )}
              </div>
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
                    <button onClick={handlePublishClick} style={styles.primaryBtn}>Publicar</button>
                  )}
                  {selectedExam.status === 'PUBLICADO' && (
                    <button onClick={closeExam} style={styles.closeBtn}>Cerrar examen</button>
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
                    </div>
                    <div style={styles.editActions}>
                      <button type="submit" style={styles.secondaryBtn}>Guardar cambios</button>
                    </div>
                  </form>

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
                </>
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
                  const treeForm = isDecisionTreeForm(form)
                  const tableForm = isDecisionTableForm(form)
                  return (
                    <article key={topic.id} style={{ ...styles.topicCard, borderTop: `4px solid ${topic.colorHex || '#1956D8'}` }}>
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
                                  <button
                                    type="button"
                                    onClick={() => { setEditingTopicId(topic.id); setEditingTopicName(topic.name) }}
                                    style={styles.topicRenameBtn}
                                    title="Renombrar tema"
                                  >✎</button>
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
                                    <DecisionTableEditor
                                      value={editForm.modelAnswer || emptyDecisionTableValue()}
                                      onChange={(value) => updateEditingQuestionForm(question.id, 'modelAnswer', value)}
                                    />
                                  ) : isDecisionTreeForm(editForm) ? (
                                    <DecisionTreeEditor
                                      value={editForm.modelAnswer || emptyDecisionTreeValue()}
                                      onChange={(value) => updateEditingQuestionForm(question.id, 'modelAnswer', value)}
                                    />
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
                                      <DecisionTableEditor value={question.modelAnswer} readOnly compact />
                                    ) : treeQuestion ? (
                                      <DecisionTreeEditor value={question.modelAnswer} readOnly compact />
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
                            <DecisionTableEditor
                              value={form.modelAnswer || emptyDecisionTableValue()}
                              onChange={(value) => updateQuestionForm(topic.id, 'modelAnswer', value)}
                            />
                          ) : treeForm ? (
                            <DecisionTreeEditor
                              value={form.modelAnswer || emptyDecisionTreeValue()}
                              onChange={(value) => updateQuestionForm(topic.id, 'modelAnswer', value)}
                            />
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

      {modal && (
        <div style={styles.modalOverlay} onClick={() => setModal(null)}>
          <div style={styles.modalBox} onClick={(e) => e.stopPropagation()}>
            {modal.type === 'badPoints' ? (
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
  promptTextarea: { minHeight: 118, padding: '10px 12px', border: '1px solid #C9DDE3', borderRadius: 6, fontSize: 14, color: '#09222A', resize: 'vertical', boxSizing: 'border-box', width: '100%', fontFamily: 'inherit', lineHeight: 1.5 },
  modelTextarea: { minHeight: 220, padding: '10px 12px', border: '1px solid #C9DDE3', borderRadius: 6, fontSize: 14, color: '#09222A', resize: 'vertical', boxSizing: 'border-box', width: '100%', fontFamily: 'inherit', lineHeight: 1.5 },
  primaryBtn: { minHeight: 38, padding: '8px 16px', background: '#1956D8', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  secondaryBtn: { minHeight: 38, padding: '8px 14px', background: '#fff', color: '#1956D8', border: '1px solid #1956D8', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  disabledBtn: { minHeight: 38, padding: '8px 16px', background: '#C9DDE3', color: '#536B76', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 700 },
  closeBtn: { minHeight: 38, padding: '8px 16px', background: '#fff', color: '#9B2C2C', border: '1px solid #9B2C2C', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
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
  questionRow: { border: '1px solid #E7F0F3', borderRadius: 6, padding: 10, display: 'grid', gridTemplateColumns: '1fr auto', gap: 12 },
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
  submissionPanel: { background: '#fff', border: '1px solid #D8E8EC', borderRadius: 8, padding: 16, marginBottom: 16 },
  submissionHeader: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12, marginBottom: 12 },
  submissionTitle: { fontSize: 17, margin: 0 },
  submissionList: { display: 'flex', flexDirection: 'column', gap: 8 },
  submissionRow: { border: '1px solid #E7F0F3', borderRadius: 6, padding: 10, display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 12 },
  submittedBadge: { background: '#DDF6EC', color: '#087A55', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
  progressBadge: { background: '#E6EEFF', color: '#1956D8', padding: '4px 10px', borderRadius: 999, fontSize: 12, fontWeight: 800 },
  modalOverlay: { position: 'fixed', inset: 0, background: 'rgba(9,34,42,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000 },
  modalBox: { background: '#fff', borderRadius: 12, padding: '28px 32px', maxWidth: 520, width: '90%', boxShadow: '0 8px 40px rgba(9,34,42,0.22)' },
  modalTitle: { fontSize: 18, fontWeight: 800, margin: '0 0 10px', color: '#09222A' },
  modalText: { fontSize: 14, color: '#304653', margin: '0 0 8px' },
  modalList: { margin: '0 0 18px 20px', padding: 0, fontSize: 14, color: '#09222A', lineHeight: 2 },
  modalActions: { display: 'flex', gap: 10, justifyContent: 'flex-end', flexWrap: 'wrap' },
}
