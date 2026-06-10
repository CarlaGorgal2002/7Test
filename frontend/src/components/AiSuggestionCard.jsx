export default function AiSuggestionCard({ suggestion, history = [], maxPoints, onReview }) {
  if (!suggestion) return null
  const failed = suggestion.status === 'FAILED'
  return (
    <section aria-label="Sugerencia de IA" style={failed ? styles.failed : styles.card}>
      <div style={styles.header}>
        <strong>Sugerencia IA, intento {suggestion.attemptNumber}</strong>
        <span>{suggestion.status}</span>
      </div>
      {failed ? (
        <p style={styles.text}>{suggestion.errorSummary || 'No se pudo generar esta sugerencia.'}</p>
      ) : (
        <>
          <p style={styles.score}>
            Fraccion {suggestion.suggestedFraction} · Puntaje sugerido {suggestion.suggestedScore} / {maxPoints}
          </p>
          <p style={styles.text}>{suggestion.suggestedComment}</p>
          {suggestion.strengths?.length > 0 && <p style={styles.text}><strong>Fortalezas:</strong> {suggestion.strengths.join(' · ')}</p>}
          {suggestion.issues?.length > 0 && <p style={styles.text}><strong>Problemas:</strong> {suggestion.issues.join(' · ')}</p>}
          <p style={styles.meta}>
            Confianza: {suggestion.confidence || 'SIN DATO'} · Paginas: {suggestion.sourcePages?.join(', ') || 'sin respaldo'}
          </p>
          {suggestion.requiresHumanReview && (
            <p role="alert" style={styles.warning}>Requiere revision humana: {suggestion.reviewReason || 'verificar manualmente'}</p>
          )}
          {suggestion.status === 'READY' && (
            <div style={styles.actions}>
              <button onClick={() => onReview(suggestion, 'accept')} style={styles.accept}>Aceptar sugerencia</button>
              <button onClick={() => onReview(suggestion, 'reject')} style={styles.reject}>Rechazar</button>
            </div>
          )}
        </>
      )}
      {history.length > 1 && (
        <details style={styles.history}>
          <summary>Ver historial ({history.length} intentos)</summary>
          {history.slice(1).map(previous => (
            <p key={previous.id} style={styles.meta}>
              Intento {previous.attemptNumber}: {previous.status}
              {previous.suggestedScore != null ? ` · ${previous.suggestedScore} pts` : ''}
            </p>
          ))}
        </details>
      )}
    </section>
  )
}

const styles = {
  card: { background: '#F7F4FF', border: '1px solid #C4B5FD', borderRadius: 8, padding: 12, marginBottom: 14 },
  failed: { background: '#FFF5F5', border: '1px solid #FCA5A5', borderRadius: 8, padding: 12, marginBottom: 14 },
  header: { display: 'flex', justifyContent: 'space-between', alignItems: 'center', color: '#5B21B6', fontSize: 12, marginBottom: 6 },
  score: { margin: '4px 0', color: '#5B21B6', fontWeight: 800, fontSize: 13 },
  text: { margin: '5px 0', color: '#304653', fontSize: 13, lineHeight: 1.45 },
  meta: { margin: '5px 0', color: '#536B76', fontSize: 12 },
  warning: { margin: '8px 0', background: '#FFF8DF', border: '1px solid #E7CE74', borderRadius: 6, padding: 8, color: '#7A5600', fontSize: 12, fontWeight: 700 },
  actions: { display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 10 },
  accept: { minHeight: 38, padding: '8px 16px', background: '#1956D8', color: '#fff', border: 'none', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  reject: { minHeight: 38, padding: '8px 16px', background: '#fff', color: '#9B2C2C', border: '1px solid #9B2C2C', borderRadius: 6, fontSize: 14, fontWeight: 700, cursor: 'pointer' },
  history: { marginTop: 10, color: '#536B76', fontSize: 12 },
}
