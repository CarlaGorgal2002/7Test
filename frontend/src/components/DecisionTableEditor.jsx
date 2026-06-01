import { useEffect, useRef, useState } from 'react'

export const DECISION_TABLE_PREFIX = '7TEST_DECISION_TABLE:'

const initialTable = { rows: 2, cols: 2, cells: [['', ''], ['', '']], spans: {} }

export function emptyDecisionTableValue() { return serializeTable(initialTable) }
export function isDecisionTableValue(v = '') { return typeof v === 'string' && v.startsWith(DECISION_TABLE_PREFIX) }
export function isDecisionTablePrompt(p = '') {
  return p.normalize('NFD').replace(/[̀-ͯ]/g, '').toLowerCase().includes('tabla de decision')
}
export function isEmptyDecisionTableValue(v = '') {
  return parseTable(v).cells.every(row => row.every(c => !c.trim()))
}
export function serializeTable(table) {
  return `${DECISION_TABLE_PREFIX}${JSON.stringify(safeTable(table))}`
}

function parseTable(value) {
  if (!isDecisionTableValue(value)) return initialTable
  try { return safeTable(JSON.parse(value.slice(DECISION_TABLE_PREFIX.length))) }
  catch { return initialTable }
}

function safeTable(table) {
  const rows = clamp(Number(table?.rows) || 2, 1, 40)
  const cols = clamp(Number(table?.cols) || 2, 1, 40)
  const sourceCells = Array.isArray(table?.cells) ? table.cells : []
  const cells = Array.from({ length: rows }, (_, r) => {
    const sr = Array.isArray(sourceCells[r]) ? sourceCells[r] : []
    return Array.from({ length: cols }, (_, c) => String(sr[c] ?? '').slice(0, 1000))
  })
  const spans = {}
  for (const [key, span] of Object.entries(table?.spans || {})) {
    const [r, c] = key.split('-').map(Number)
    if (r >= 0 && r < rows && c >= 0 && c < cols) {
      const sr = clamp(Number(span?.r) || 1, 1, rows - r)
      const sc = clamp(Number(span?.c) || 1, 1, cols - c)
      if (sr > 1 || sc > 1) spans[key] = { r: sr, c: sc }
    }
  }
  return { rows, cols, cells, spans }
}

function getCovered(spans) {
  const covered = new Set()
  for (const [key, span] of Object.entries(spans || {})) {
    const [r, c] = key.split('-').map(Number)
    for (let dr = 0; dr < span.r; dr++)
      for (let dc = 0; dc < span.c; dc++)
        if (dr || dc) covered.add(`${r + dr}-${c + dc}`)
  }
  return covered
}

function normSel(sel) {
  return {
    r1: Math.min(sel.startRow, sel.endRow),
    c1: Math.min(sel.startCol, sel.endCol),
    r2: Math.max(sel.startRow, sel.endRow),
    c2: Math.max(sel.startCol, sel.endCol),
  }
}

function clamp(v, min, max) { return Math.min(Math.max(v, min), max) }

export default function DecisionTableEditor({ value, onChange, readOnly = false, compact = false }) {
  const lastEmitted = useRef(value || '')
  const [table, setTable] = useState(() => parseTable(value))
  const [sel, setSel] = useState({ startRow: 0, startCol: 0, endRow: 0, endCol: 0 })
  const [dragging, setDragging] = useState(false)

  useEffect(() => {
    if ((value || '') !== lastEmitted.current) setTable(parseTable(value))
  }, [value])

  function emit(next) {
    const safe = safeTable(next)
    setTable(safe)
    const s = serializeTable(safe)
    lastEmitted.current = s
    onChange?.(s)
  }

  function updateCell(r, c, text) {
    const cells = table.cells.map(row => [...row])
    cells[r][c] = text
    emit({ ...table, cells })
  }

  function addRow(after) {
    if (readOnly) return
    const index = clamp((after ?? sel.startRow) + 1, 0, table.rows)
    const cells = table.cells.map(row => [...row])
    cells.splice(index, 0, Array(table.cols).fill(''))
    // clear spans that overlap (simple strategy: remove affected spans)
    const spans = clearSpansNearRow(table.spans, index)
    emit({ rows: table.rows + 1, cols: table.cols, cells, spans })
    setSel({ startRow: index, startCol: 0, endRow: index, endCol: 0 })
  }

  function addColumn(after) {
    if (readOnly) return
    const index = clamp((after ?? sel.startCol) + 1, 0, table.cols)
    const cells = table.cells.map(row => {
      const next = [...row]
      next.splice(index, 0, '')
      return next
    })
    const spans = clearSpansNearCol(table.spans, index)
    emit({ rows: table.rows, cols: table.cols + 1, cells, spans })
    setSel({ startRow: 0, startCol: index, endRow: 0, endCol: index })
  }

  function removeRow() {
    if (readOnly || table.rows <= 1) return
    const index = clamp(sel.startRow, 0, table.rows - 1)
    const cells = table.cells.filter((_, i) => i !== index)
    const spans = clearSpansNearRow(table.spans, index)
    emit({ rows: table.rows - 1, cols: table.cols, cells, spans })
    setSel(s => ({ ...s, startRow: Math.max(0, index - 1), endRow: Math.max(0, index - 1) }))
  }

  function removeColumn() {
    if (readOnly || table.cols <= 1) return
    const index = clamp(sel.startCol, 0, table.cols - 1)
    const cells = table.cells.map(row => row.filter((_, i) => i !== index))
    const spans = clearSpansNearCol(table.spans, index)
    emit({ rows: table.rows, cols: table.cols - 1, cells, spans })
    setSel(s => ({ ...s, startCol: Math.max(0, index - 1), endCol: Math.max(0, index - 1) }))
  }

  function mergeSelection() {
    if (readOnly) return
    const { r1, c1, r2, c2 } = normSel(sel)
    if (r1 === r2 && c1 === c2) return
    const texts = []
    for (let r = r1; r <= r2; r++)
      for (let c = c1; c <= c2; c++)
        if (table.cells[r]?.[c]?.trim()) texts.push(table.cells[r][c].trim())
    const cells = table.cells.map(row => [...row])
    for (let r = r1; r <= r2; r++)
      for (let c = c1; c <= c2; c++)
        cells[r][c] = (r === r1 && c === c1) ? texts.join(' ') : ''
    const spans = { ...(table.spans || {}) }
    // remove spans inside selection
    for (let r = r1; r <= r2; r++)
      for (let c = c1; c <= c2; c++)
        delete spans[`${r}-${c}`]
    spans[`${r1}-${c1}`] = { r: r2 - r1 + 1, c: c2 - c1 + 1 }
    emit({ ...table, cells, spans })
    setSel({ startRow: r1, startCol: c1, endRow: r1, endCol: c1 })
  }

  function unmerge() {
    if (readOnly) return
    const { r1, c1 } = normSel(sel)
    const key = `${r1}-${c1}`
    if (!table.spans?.[key]) return
    const spans = { ...(table.spans || {}) }
    delete spans[key]
    emit({ ...table, spans })
  }

  const covered = getCovered(table.spans)
  const { r1: sr1, c1: sc1, r2: sr2, c2: sc2 } = normSel(sel)
  const multiSel = sr2 > sr1 || sc2 > sc1
  const canMerge = !readOnly && multiSel
  const canUnmerge = !readOnly && !!table.spans?.[`${sr1}-${sc1}`]

  function startDrag(r, c) {
    setSel({ startRow: r, startCol: c, endRow: r, endCol: c })
    setDragging(true)
  }
  function extendDrag(r, c) {
    if (!dragging) return
    setSel(s => ({ ...s, endRow: r, endCol: c }))
  }
  function stopDrag() { setDragging(false) }

  function isInSel(r, c) {
    return r >= sr1 && r <= sr2 && c >= sc1 && c <= sc2
  }

  return (
    <div
      style={compact ? styles.editorCompact : styles.editor}
      onMouseUp={stopDrag}
      onMouseLeave={stopDrag}
    >
      {!readOnly && (
        <div style={styles.toolbar}>
          <button type="button" onClick={() => addRow()} style={styles.toolButton}>+ Fila</button>
          <button type="button" onClick={() => addColumn()} style={styles.toolButton}>+ Columna</button>
          <button type="button" onClick={removeRow} disabled={table.rows <= 1} style={table.rows > 1 ? styles.deleteButton : styles.disabledButton}>− Fila</button>
          <button type="button" onClick={removeColumn} disabled={table.cols <= 1} style={table.cols > 1 ? styles.deleteButton : styles.disabledButton}>− Columna</button>
          <span style={styles.sep} />
          <button type="button" onClick={mergeSelection} disabled={!canMerge} style={canMerge ? styles.mergeButton : styles.disabledButton}>Combinar</button>
          <button type="button" onClick={unmerge} disabled={!canUnmerge} style={canUnmerge ? styles.mergeButton : styles.disabledButton}>Separar</button>
        </div>
      )}

      <div style={compact ? styles.tableFrameCompact : styles.tableFrame}>
        <table style={styles.table} onMouseLeave={stopDrag}>
          <tbody>
            {table.cells.map((row, r) => (
              <tr key={r}>
                {row.map((cell, c) => {
                  const key = `${r}-${c}`
                  if (covered.has(key)) return null
                  const span = table.spans?.[key]
                  const inSel = isInSel(r, c)
                  return (
                    <td
                      key={key}
                      rowSpan={span?.r || 1}
                      colSpan={span?.c || 1}
                      onMouseDown={() => startDrag(r, c)}
                      onMouseEnter={() => extendDrag(r, c)}
                      style={inSel && !readOnly ? styles.cellSelected : styles.cell}
                    >
                      <textarea
                        value={cell}
                        readOnly={readOnly}
                        onChange={e => updateCell(r, c, e.target.value)}
                        style={readOnly ? styles.cellTextReadOnly : styles.cellText}
                        spellCheck="false"
                      />
                    </td>
                  )
                })}
                {!readOnly && (
                  <td style={styles.sideControlCell}>
                    <button type="button" onClick={() => addRow(r)} style={styles.plusButton}>+</button>
                  </td>
                )}
              </tr>
            ))}
            {!readOnly && (
              <tr>
                {Array.from({ length: table.cols }, (_, c) => (
                  <td key={c} style={styles.bottomControlCell}>
                    <button type="button" onClick={() => addColumn(c)} style={styles.plusButton}>+</button>
                  </td>
                ))}
                <td style={styles.cornerCell} />
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function clearSpansNearRow(spans, index) {
  const result = {}
  for (const [key, span] of Object.entries(spans || {})) {
    const [r, c] = key.split('-').map(Number)
    if (r === index) continue // remove spans starting at deleted row
    if (r < index && r + span.r > index) continue // remove spans that cross the deleted row
    result[key] = span
  }
  return result
}

function clearSpansNearCol(spans, index) {
  const result = {}
  for (const [key, span] of Object.entries(spans || {})) {
    const [r, c] = key.split('-').map(Number)
    if (c === index) continue
    if (c < index && c + span.c > index) continue
    result[key] = span
  }
  return result
}

const styles = {
  editor: { border: '1px solid #C9DDE3', borderRadius: 8, background: '#F8FBFC', overflow: 'hidden', userSelect: 'none' },
  editorCompact: { border: '1px solid #E7F0F3', borderRadius: 6, background: '#F8FBFC', overflow: 'hidden', marginTop: 8, userSelect: 'none' },
  toolbar: { minHeight: 46, display: 'flex', alignItems: 'center', gap: 6, padding: '8px 10px', borderBottom: '1px solid #D8E8EC', background: '#fff', flexWrap: 'wrap' },
  sep: { display: 'inline-block', width: 1, height: 24, background: '#D8E8EC', margin: '0 4px' },
  toolButton: { minHeight: 32, padding: '6px 12px', border: '1px solid #1956D8', borderRadius: 6, background: '#fff', color: '#1956D8', fontWeight: 700, cursor: 'pointer' },
  deleteButton: { minHeight: 32, padding: '6px 12px', border: '1px solid #9B2C2C', borderRadius: 6, background: '#fff', color: '#9B2C2C', fontWeight: 700, cursor: 'pointer' },
  mergeButton: { minHeight: 32, padding: '6px 12px', border: '1px solid #7B3FAE', borderRadius: 6, background: '#fff', color: '#7B3FAE', fontWeight: 700, cursor: 'pointer' },
  disabledButton: { minHeight: 32, padding: '6px 12px', border: '1px solid #C9DDE3', borderRadius: 6, background: '#F4F8FA', color: '#7B919B', fontWeight: 700 },
  tableFrame: { width: '100%', maxHeight: 620, overflowY: 'auto', overflowX: 'hidden', background: '#fff' },
  tableFrameCompact: { width: '100%', maxHeight: 320, overflowY: 'auto', overflowX: 'hidden', background: '#fff' },
  table: { borderCollapse: 'collapse', width: '100%', tableLayout: 'fixed' },
  cell: { height: 34, border: '1px solid #93AEB8', padding: 0, background: '#fff', overflow: 'hidden', cursor: 'cell' },
  cellSelected: { height: 34, border: '2px solid #1956D8', padding: 0, background: '#EEF4FF', overflow: 'hidden', cursor: 'cell' },
  cellText: { display: 'block', width: '100%', height: '100%', minHeight: 34, boxSizing: 'border-box', border: 'none', outline: 'none', resize: 'none', padding: '7px 3px 0', color: '#09222A', fontFamily: 'inherit', fontSize: 12, lineHeight: 1.3, background: 'transparent', textAlign: 'center', overflow: 'hidden', cursor: 'text', userSelect: 'text' },
  cellTextReadOnly: { display: 'block', width: '100%', height: '100%', minHeight: 34, boxSizing: 'border-box', border: 'none', outline: 'none', resize: 'none', padding: '7px 3px 0', color: '#09222A', fontFamily: 'inherit', fontSize: 12, lineHeight: 1.3, background: 'transparent', textAlign: 'center', overflow: 'hidden' },
  sideControlCell: { width: 34, border: 'none', padding: '0 0 0 4px', verticalAlign: 'middle' },
  bottomControlCell: { height: 36, border: 'none', padding: '6px 0 0', textAlign: 'center' },
  cornerCell: { width: 34, border: 'none' },
  plusButton: { width: 26, height: 26, borderRadius: 6, border: '1px solid #1956D8', background: '#fff', color: '#1956D8', fontWeight: 800, cursor: 'pointer' },
}
