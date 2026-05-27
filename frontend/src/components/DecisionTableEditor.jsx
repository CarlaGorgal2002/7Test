import { useEffect, useRef, useState } from 'react'

export const DECISION_TABLE_PREFIX = '7TEST_DECISION_TABLE:'

const initialTable = {
  rows: 2,
  cols: 2,
  cells: [
    ['', ''],
    ['', ''],
  ],
}

export function emptyDecisionTableValue() {
  return serializeTable(initialTable)
}

export function isDecisionTableValue(value = '') {
  return typeof value === 'string' && value.startsWith(DECISION_TABLE_PREFIX)
}

export function isDecisionTablePrompt(prompt = '') {
  const normalized = prompt.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
  return normalized.includes('tabla de decision')
}

export function isEmptyDecisionTableValue(value = '') {
  const table = parseTable(value)
  return table.cells.every((row) => row.every((cell) => !cell.trim()))
}

export function serializeTable(table) {
  const safe = safeTable(table)
  return `${DECISION_TABLE_PREFIX}${JSON.stringify(safe)}`
}

function parseTable(value) {
  if (!isDecisionTableValue(value)) return initialTable
  try {
    return safeTable(JSON.parse(value.slice(DECISION_TABLE_PREFIX.length)))
  } catch {
    return initialTable
  }
}

export default function DecisionTableEditor({ value, onChange, readOnly = false, compact = false }) {
  const lastEmitted = useRef(value || '')
  const [table, setTable] = useState(() => parseTable(value))
  const [selected, setSelected] = useState({ row: 0, col: 0 })

  useEffect(() => {
    if ((value || '') !== lastEmitted.current) {
      setTable(parseTable(value))
    }
  }, [value])

  function emit(nextTable) {
    const safe = safeTable(nextTable)
    setTable(safe)
    const serialized = serializeTable(safe)
    lastEmitted.current = serialized
    onChange?.(serialized)
  }

  function updateCell(rowIndex, colIndex, text) {
    const cells = table.cells.map((row) => [...row])
    cells[rowIndex][colIndex] = text
    emit({ ...table, cells })
  }

  function addRow(after = selected.row) {
    if (readOnly) return
    const index = clamp(after + 1, 0, table.rows)
    const cells = table.cells.map((row) => [...row])
    cells.splice(index, 0, Array.from({ length: table.cols }, () => ''))
    emit({ rows: table.rows + 1, cols: table.cols, cells })
    setSelected({ row: index, col: Math.min(selected.col, table.cols - 1) })
  }

  function addColumn(after = selected.col) {
    if (readOnly) return
    const index = clamp(after + 1, 0, table.cols)
    const cells = table.cells.map((row) => {
      const next = [...row]
      next.splice(index, 0, '')
      return next
    })
    emit({ rows: table.rows, cols: table.cols + 1, cells })
    setSelected({ row: Math.min(selected.row, table.rows - 1), col: index })
  }

  function removeRow() {
    if (readOnly || table.rows <= 1) return
    const index = clamp(selected.row, 0, table.rows - 1)
    const cells = table.cells.filter((_, rowIndex) => rowIndex !== index)
    emit({ rows: table.rows - 1, cols: table.cols, cells })
    setSelected({ row: Math.max(0, index - 1), col: selected.col })
  }

  function removeColumn() {
    if (readOnly || table.cols <= 1) return
    const index = clamp(selected.col, 0, table.cols - 1)
    const cells = table.cells.map((row) => row.filter((_, colIndex) => colIndex !== index))
    emit({ rows: table.rows, cols: table.cols - 1, cells })
    setSelected({ row: selected.row, col: Math.max(0, index - 1) })
  }

  return (
    <div style={compact ? styles.editorCompact : styles.editor}>
      {!readOnly && (
        <div style={styles.toolbar}>
          <button type="button" onClick={() => addRow()} style={styles.toolButton}>+ Fila</button>
          <button type="button" onClick={() => addColumn()} style={styles.toolButton}>+ Columna</button>
          <button type="button" onClick={removeRow} disabled={table.rows <= 1} style={table.rows > 1 ? styles.deleteButton : styles.disabledButton}>Eliminar fila</button>
          <button type="button" onClick={removeColumn} disabled={table.cols <= 1} style={table.cols > 1 ? styles.deleteButton : styles.disabledButton}>Eliminar columna</button>
        </div>
      )}

      <div style={compact ? styles.tableFrameCompact : styles.tableFrame}>
        <table style={styles.table}>
          <tbody>
            {table.cells.map((row, rowIndex) => (
              <tr key={rowIndex}>
                {row.map((cell, colIndex) => {
                  const active = selected.row === rowIndex && selected.col === colIndex
                  return (
                    <td key={`${rowIndex}-${colIndex}`} style={active && !readOnly ? styles.cellActive : styles.cell}>
                      <textarea
                        value={cell}
                        readOnly={readOnly}
                        onFocus={() => setSelected({ row: rowIndex, col: colIndex })}
                        onChange={(event) => updateCell(rowIndex, colIndex, event.target.value)}
                        style={readOnly ? styles.cellTextReadOnly : styles.cellText}
                        spellCheck="false"
                      />
                    </td>
                  )
                })}
                {!readOnly && (
                  <td style={styles.sideControlCell}>
                    <button type="button" onClick={() => addRow(rowIndex)} style={styles.plusButton}>+</button>
                  </td>
                )}
              </tr>
            ))}
            {!readOnly && (
              <tr>
                {Array.from({ length: table.cols }, (_, colIndex) => (
                  <td key={colIndex} style={styles.bottomControlCell}>
                    <button type="button" onClick={() => addColumn(colIndex)} style={styles.plusButton}>+</button>
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

function safeTable(table) {
  const rows = clamp(Number(table?.rows) || 2, 1, 40)
  const cols = clamp(Number(table?.cols) || 2, 1, 40)
  const sourceCells = Array.isArray(table?.cells) ? table.cells : []
  const cells = Array.from({ length: rows }, (_, rowIndex) => {
    const sourceRow = Array.isArray(sourceCells[rowIndex]) ? sourceCells[rowIndex] : []
    return Array.from({ length: cols }, (_, colIndex) => String(sourceRow[colIndex] || '').slice(0, 500))
  })
  return { rows, cols, cells }
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

const styles = {
  editor: { border: '1px solid #C9DDE3', borderRadius: 8, background: '#F8FBFC', overflow: 'hidden' },
  editorCompact: { border: '1px solid #E7F0F3', borderRadius: 6, background: '#F8FBFC', overflow: 'hidden', marginTop: 8 },
  toolbar: { minHeight: 46, display: 'flex', alignItems: 'center', gap: 8, padding: '8px 10px', borderBottom: '1px solid #D8E8EC', background: '#fff', flexWrap: 'wrap' },
  toolButton: { minHeight: 32, padding: '6px 12px', border: '1px solid #1956D8', borderRadius: 6, background: '#fff', color: '#1956D8', fontWeight: 700, cursor: 'pointer' },
  deleteButton: { minHeight: 32, padding: '6px 12px', border: '1px solid #9B2C2C', borderRadius: 6, background: '#fff', color: '#9B2C2C', fontWeight: 700, cursor: 'pointer' },
  disabledButton: { minHeight: 32, padding: '6px 12px', border: '1px solid #C9DDE3', borderRadius: 6, background: '#F4F8FA', color: '#7B919B', fontWeight: 700 },
  tableFrame: { width: '100%', maxHeight: 620, overflowY: 'auto', overflowX: 'hidden', background: '#fff' },
  tableFrameCompact: { width: '100%', maxHeight: 320, overflowY: 'auto', overflowX: 'hidden', background: '#fff' },
  table: { borderCollapse: 'collapse', width: '100%', tableLayout: 'fixed' },
  cell: { height: 34, border: '1px solid #93AEB8', padding: 0, background: '#fff', overflow: 'hidden' },
  cellActive: { height: 34, border: '2px solid #1956D8', padding: 0, background: '#F8FBFF', overflow: 'hidden' },
  cellText: { display: 'block', width: '100%', height: 34, boxSizing: 'border-box', border: 'none', outline: 'none', resize: 'none', padding: '7px 3px 0', color: '#09222A', fontFamily: 'inherit', fontSize: 12, lineHeight: 1.3, background: 'transparent', textAlign: 'center', overflow: 'hidden' },
  cellTextReadOnly: { display: 'block', width: '100%', height: 34, boxSizing: 'border-box', border: 'none', outline: 'none', resize: 'none', padding: '7px 3px 0', color: '#09222A', fontFamily: 'inherit', fontSize: 12, lineHeight: 1.3, background: 'transparent', textAlign: 'center', overflow: 'hidden' },
  sideControlCell: { width: 34, border: 'none', padding: '0 0 0 4px', verticalAlign: 'middle' },
  bottomControlCell: { height: 36, border: 'none', padding: '6px 0 0', textAlign: 'center' },
  cornerCell: { width: 34, border: 'none' },
  plusButton: { width: 26, height: 26, borderRadius: 6, border: '1px solid #1956D8', background: '#fff', color: '#1956D8', fontWeight: 800, cursor: 'pointer' },
}
