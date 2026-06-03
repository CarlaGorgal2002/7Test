import { useEffect, useRef, useState } from 'react'

export const DECISION_TREE_PREFIX = '7TEST_DECISION_TREE:'

const emptyTree = { nodes: [], edges: [] }
const boardSize = { width: 1800, height: 1200 }
const nodeSize = { oval: { width: 158, height: 88 }, rect: { width: 156, height: 80 } }
const sides = ['top', 'right', 'bottom', 'left']

export function emptyDecisionTreeValue() {
  return serializeTree(emptyTree)
}

export function isDecisionTreeValue(value = '') {
  return typeof value === 'string' && value.startsWith(DECISION_TREE_PREFIX)
}

export function isDecisionTreePrompt(prompt = '') {
  const normalized = prompt.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase()
  return normalized.includes('arbol de decision')
}

export function isEmptyDecisionTreeValue(value = '') {
  const tree = parseTree(value)
  return tree.nodes.length === 0 && tree.edges.length === 0
}

export function serializeTree(tree) {
  return `${DECISION_TREE_PREFIX}${JSON.stringify({
    nodes: safeNodes(tree.nodes),
    edges: safeEdges(tree.edges),
  })}`
}

function parseTree(value) {
  if (!isDecisionTreeValue(value)) return emptyTree
  try {
    const parsed = JSON.parse(value.slice(DECISION_TREE_PREFIX.length))
    return {
      nodes: safeNodes(parsed.nodes),
      edges: safeEdges(parsed.edges),
    }
  } catch {
    return emptyTree
  }
}

export default function DecisionTreeEditor({ value, onChange, readOnly = false, compact = false }) {
  const lastEmitted = useRef(value || '')
  const boardRef = useRef(null)
  const [tree, setTree] = useState(() => parseTree(value))
  const [selected, setSelected] = useState(null)
  const [drag, setDrag] = useState(null)
  const [draftEdge, setDraftEdge] = useState(null)
  const [movingEdge, setMovingEdge] = useState(null) // { edgeId, endpoint:'from'|'to', point }
  const [panning, setPanning] = useState(null) // { startX, startY, scrollLeft, scrollTop }
  const [zoom, setZoom] = useState(0.85)

  useEffect(() => {
    if ((value || '') !== lastEmitted.current) {
      setTree(parseTree(value))
    }
  }, [value])

  function emit(nextTree) {
    setTree(nextTree)
    const serialized = serializeTree(nextTree)
    lastEmitted.current = serialized
    onChange?.(serialized)
  }

  function addNode(type, point = null) {
    if (readOnly) return
    const size = nodeSize[type]
    const index = tree.nodes.length
    const x = point ? point.x - size.width / 2 : 120 + (index % 4) * 180
    const y = point ? point.y - size.height / 2 : 90 + Math.floor(index / 4) * 120
    const node = {
      id: makeId('node'),
      type,
      x: clamp(snap(x), 12, boardSize.width - size.width - 12),
      y: clamp(snap(y), 12, boardSize.height - size.height - 12),
      width: size.width,
      height: size.height,
      text: '',
    }
    emit({ ...tree, nodes: [...tree.nodes, node] })
    setSelected({ type: 'node', id: node.id })
  }

  function updateNode(id, changes) {
    emit({ ...tree, nodes: tree.nodes.map((node) => node.id === id ? { ...node, ...changes } : node) })
  }

  function updateEdge(id, changes) {
    emit({ ...tree, edges: tree.edges.map((edge) => edge.id === id ? { ...edge, ...changes } : edge) })
  }

  function removeSelected() {
    if (readOnly || !selected) return
    if (selected.type === 'node') {
      emit({
        nodes: tree.nodes.filter((node) => node.id !== selected.id),
        edges: tree.edges.filter((edge) => edge.from.nodeId !== selected.id && edge.to.nodeId !== selected.id),
      })
    }
    if (selected.type === 'edge') {
      emit({ ...tree, edges: tree.edges.filter((edge) => edge.id !== selected.id) })
    }
    setSelected(null)
  }

  function startNodeDrag(event, node) {
    if (readOnly) return
    event.preventDefault()
    event.stopPropagation()
    const point = boardPoint(event)
    setDrag({ nodeId: node.id, offsetX: point.x - node.x, offsetY: point.y - node.y })
    setSelected({ type: 'node', id: node.id })
  }

  function startPan(event) {
    if (drag || draftEdge || movingEdge) return
    setPanning({
      startX: event.clientX,
      startY: event.clientY,
      scrollLeft: boardRef.current.scrollLeft,
      scrollTop: boardRef.current.scrollTop,
    })
  }

  function movePointer(event) {
    if (panning) {
      boardRef.current.scrollLeft = panning.scrollLeft - (event.clientX - panning.startX)
      boardRef.current.scrollTop = panning.scrollTop - (event.clientY - panning.startY)
    }
    if (drag) {
      const node = tree.nodes.find((item) => item.id === drag.nodeId)
      if (!node) return
      const point = boardPoint(event)
      updateNode(node.id, {
        x: clamp(snap(point.x - drag.offsetX), 8, boardSize.width - node.width - 8),
        y: clamp(snap(point.y - drag.offsetY), 8, boardSize.height - node.height - 8),
      })
    }
    if (draftEdge) setDraftEdge({ ...draftEdge, point: boardPoint(event) })
    if (movingEdge) setMovingEdge({ ...movingEdge, point: boardPoint(event) })
  }

  function stopPointer() {
    setDrag(null)
    setDraftEdge(null)
    setMovingEdge(null)
    setPanning(null)
  }

  function startMoveEdgeEndpoint(event, edge, endpoint) {
    if (readOnly) return
    event.preventDefault()
    event.stopPropagation()
    const anchor = edgeAnchor(tree.nodes, endpoint === 'from' ? edge.from : edge.to)
    setMovingEdge({ edgeId: edge.id, endpoint, point: anchor || { x: 0, y: 0 } })
    setSelected({ type: 'edge', id: edge.id })
  }

  function startEdge(event, node, side) {
    if (readOnly) return
    event.preventDefault()
    event.stopPropagation()
    setSelected({ type: 'node', id: node.id })
    setDraftEdge({
      from: { nodeId: node.id, side },
      point: anchorFor(node, side),
    })
  }

  function finishEdge(event, node, side) {
    if (readOnly) return
    event.preventDefault()
    event.stopPropagation()

    if (movingEdge) {
      const edge = tree.edges.find(e => e.id === movingEdge.edgeId)
      if (edge) {
        const other = movingEdge.endpoint === 'from' ? edge.to : edge.from
        if (node.id !== other.nodeId || side !== other.side) {
          const updated = movingEdge.endpoint === 'from'
            ? { ...edge, from: { nodeId: node.id, side } }
            : { ...edge, to: { nodeId: node.id, side } }
          emit({ ...tree, edges: tree.edges.map(e => e.id === movingEdge.edgeId ? updated : e) })
          setSelected({ type: 'edge', id: movingEdge.edgeId })
        }
      }
      setMovingEdge(null)
      return
    }

    if (!draftEdge) return
    if (draftEdge.from.nodeId === node.id && draftEdge.from.side === side) {
      setDraftEdge(null)
      return
    }
    const edge = {
      id: makeId('edge'),
      from: draftEdge.from,
      to: { nodeId: node.id, side },
      label: '',
    }
    emit({ ...tree, edges: [...tree.edges, edge] })
    setSelected({ type: 'edge', id: edge.id })
    setDraftEdge(null)
  }

  function boardPoint(event) {
    const rect = boardRef.current.getBoundingClientRect()
    return {
      x: clamp((event.clientX - rect.left + boardRef.current.scrollLeft) / zoom, 0, boardSize.width),
      y: clamp((event.clientY - rect.top + boardRef.current.scrollTop) / zoom, 0, boardSize.height),
    }
  }

  function changeZoom(delta) {
    setZoom((current) => clamp(Number((current + delta).toFixed(2)), 0.35, 1.25))
  }

  function dropShape(event) {
    if (readOnly) return
    event.preventDefault()
    const type = event.dataTransfer.getData('shape')
    if (type !== 'oval' && type !== 'rect') return
    addNode(type, boardPoint(event))
  }

  const edgeModels = tree.edges
    .map((edge) => ({ edge, from: edgeAnchor(tree.nodes, edge.from), to: edgeAnchor(tree.nodes, edge.to) }))
    .filter((item) => item.from && item.to)

  return (
    <div style={compact ? styles.editorCompact : styles.editor}>
      <div style={styles.toolbar}>
        {!readOnly && (
          <>
            <button type="button" draggable onDragStart={(event) => event.dataTransfer.setData('shape', 'oval')} onClick={() => addNode('oval')} style={styles.toolButton} title="Ovalo">
              <span style={styles.ovalTool} />
            </button>
            <button type="button" draggable onDragStart={(event) => event.dataTransfer.setData('shape', 'rect')} onClick={() => addNode('rect')} style={styles.toolButton} title="Rectangulo">
              <span style={styles.rectTool} />
            </button>
            <button type="button" onClick={removeSelected} disabled={!selected} style={selected ? styles.deleteButton : styles.disabledButton}>Eliminar</button>
          </>
        )}
        <div style={styles.zoomControls}>
          <button type="button" onClick={() => changeZoom(-0.1)} style={styles.zoomButton}>−</button>
          <span style={styles.zoomLabel}>{Math.round(zoom * 100)}%</span>
          <button type="button" onClick={() => changeZoom(0.1)} style={styles.zoomButton}>+</button>
        </div>
      </div>

      <div
        ref={boardRef}
        style={compact ? styles.boardFrameCompact : styles.boardFrame}
        onPointerMove={movePointer}
        onPointerUp={stopPointer}
        onDragOver={(event) => event.preventDefault()}
        onDrop={dropShape}
      >
        <div style={{ width: boardSize.width * zoom, height: boardSize.height * zoom, position: 'relative' }}>
        <div
          style={{ ...styles.board, transform: `scale(${zoom})`, cursor: panning ? 'grabbing' : (drag || draftEdge || movingEdge ? 'default' : 'grab') }}
          onPointerDown={(e) => { setSelected(null); startPan(e) }}
        >
          <svg width={boardSize.width} height={boardSize.height} style={styles.svgLayer}>
            <defs>
              <marker id="arrow-head" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="11" markerHeight="11" orient="auto-start-reverse">
                <path d="M 0 0 L 10 5 L 0 10 z" fill="#304653" />
              </marker>
            </defs>
            {edgeModels.map(({ edge, from, to }) => (
              <line
                key={edge.id}
                x1={from.x}
                y1={from.y}
                x2={to.x}
                y2={to.y}
                stroke={selected?.type === 'edge' && selected.id === edge.id ? '#1956D8' : '#304653'}
                strokeWidth={selected?.type === 'edge' && selected.id === edge.id ? 4 : 3}
                markerEnd="url(#arrow-head)"
                onPointerDown={(event) => {
                  event.stopPropagation()
                  setSelected({ type: 'edge', id: edge.id })
                }}
              />
            ))}
            {draftEdge && (
              <line
                x1={edgeAnchor(tree.nodes, draftEdge.from)?.x || draftEdge.point.x}
                y1={edgeAnchor(tree.nodes, draftEdge.from)?.y || draftEdge.point.y}
                x2={draftEdge.point.x}
                y2={draftEdge.point.y}
                stroke="#1956D8" strokeWidth="3" strokeDasharray="6 4"
                markerEnd="url(#arrow-head)"
              />
            )}

            {movingEdge && (() => {
              const edge = tree.edges.find(e => e.id === movingEdge.edgeId)
              if (!edge) return null
              const fixedEnd = movingEdge.endpoint === 'from' ? edge.to : edge.from
              const fixedAnchor = edgeAnchor(tree.nodes, fixedEnd)
              if (!fixedAnchor) return null
              const x1 = movingEdge.endpoint === 'to' ? fixedAnchor.x : movingEdge.point.x
              const y1 = movingEdge.endpoint === 'to' ? fixedAnchor.y : movingEdge.point.y
              const x2 = movingEdge.endpoint === 'from' ? fixedAnchor.x : movingEdge.point.x
              const y2 = movingEdge.endpoint === 'from' ? fixedAnchor.y : movingEdge.point.y
              return (
                <line
                  x1={x1} y1={y1} x2={x2} y2={y2}
                  stroke="#1956D8" strokeWidth="3" strokeDasharray="6 4"
                  markerEnd="url(#arrow-head)"
                />
              )
            })()}

            {selected?.type === 'edge' && !readOnly && !movingEdge && edgeModels
              .filter(({ edge }) => edge.id === selected.id)
              .map(({ edge, from, to }) => (
                <g key={`handles-${edge.id}`}>
                  <circle cx={from.x} cy={from.y} r={8} fill="#fff" stroke="#1956D8" strokeWidth={2}
                    style={{ cursor: 'crosshair', pointerEvents: 'all' }}
                    onPointerDown={(e) => startMoveEdgeEndpoint(e, edge, 'from')} />
                  <circle cx={to.x} cy={to.y} r={8} fill="#1956D8" stroke="#1956D8" strokeWidth={2}
                    style={{ cursor: 'crosshair', pointerEvents: 'all' }}
                    onPointerDown={(e) => startMoveEdgeEndpoint(e, edge, 'to')} />
                </g>
              ))
            }
          </svg>

          {edgeModels.map(({ edge, from, to }) => {
            const midpoint = { x: (from.x + to.x) / 2, y: (from.y + to.y) / 2 }
            return readOnly ? (
              edge.label && <span key={edge.id} style={{ ...styles.edgeLabelReadOnly, left: midpoint.x - 32, top: midpoint.y - 15 }}>{edge.label}</span>
            ) : (
              <input
                key={edge.id}
                value={edge.label}
                onChange={(event) => updateEdge(edge.id, { label: event.target.value })}
                onPointerDown={(event) => {
                  event.stopPropagation()
                  setSelected({ type: 'edge', id: edge.id })
                }}
                style={{
                  ...styles.edgeLabel,
                  ...(selected?.type === 'edge' && selected.id === edge.id ? styles.edgeLabelActive : {}),
                  left: midpoint.x - 32,
                  top: midpoint.y - 15,
                }}
                placeholder=""
              />
            )
          })}

          {tree.nodes.map((node) => (
            <div
              key={node.id}
              style={{
                ...styles.node,
                ...(node.type === 'oval' ? styles.ovalNode : styles.rectNode),
                ...(selected?.type === 'node' && selected.id === node.id ? styles.selectedNode : {}),
                left: node.x,
                top: node.y,
                width: node.width,
                height: node.height,
              }}
              onPointerDown={(event) => {
                event.stopPropagation()
                setSelected({ type: 'node', id: node.id })
              }}
            >
              {!readOnly && <div style={styles.dragHandle} onPointerDown={(event) => startNodeDrag(event, node)} />}
              <textarea
                value={node.text}
                onChange={(event) => updateNode(node.id, { text: event.target.value })}
                readOnly={readOnly}
                style={readOnly ? { ...styles.nodeText, height: '100%', paddingTop: 12 } : styles.nodeText}
                spellCheck="false"
              />
              {!readOnly && sides.map((side) => (
                <button
                  key={side}
                  type="button"
                  onPointerDown={(event) => startEdge(event, node, side)}
                  onPointerUp={(event) => finishEdge(event, node, side)}
                  style={{ ...styles.magnet, ...magnetStyle(side), ...((draftEdge || movingEdge) ? styles.magnetActive : {}) }}
                  title="Flecha"
                />
              ))}
            </div>
          ))}
        </div>
        </div>
      </div>
    </div>
  )
}

function safeNodes(nodes) {
  if (!Array.isArray(nodes)) return []
  return nodes
    .filter((node) => node && (node.type === 'oval' || node.type === 'rect'))
    .map((node) => {
      const size = nodeSize[node.type]
      return {
        id: String(node.id || makeId('node')),
        type: node.type,
        x: Number.isFinite(Number(node.x)) ? Number(node.x) : 40,
        y: Number.isFinite(Number(node.y)) ? Number(node.y) : 40,
        width: size.width,
        height: size.height,
        text: String(node.text || '').slice(0, 200),
      }
    })
}

function safeEdges(edges) {
  if (!Array.isArray(edges)) return []
  return edges
    .filter((edge) => edge?.from?.nodeId && edge?.to?.nodeId)
    .map((edge) => ({
      id: String(edge.id || makeId('edge')),
      from: { nodeId: String(edge.from.nodeId), side: sides.includes(edge.from.side) ? edge.from.side : 'bottom' },
      to: { nodeId: String(edge.to.nodeId), side: sides.includes(edge.to.side) ? edge.to.side : 'top' },
      label: String(edge.label || '').slice(0, 80),
    }))
}

function edgeAnchor(nodes, endpoint) {
  const node = nodes.find((item) => item.id === endpoint.nodeId)
  return node ? anchorFor(node, endpoint.side) : null
}

function anchorFor(node, side) {
  if (side === 'top') return { x: node.x + node.width / 2, y: node.y }
  if (side === 'right') return { x: node.x + node.width, y: node.y + node.height / 2 }
  if (side === 'bottom') return { x: node.x + node.width / 2, y: node.y + node.height }
  return { x: node.x, y: node.y + node.height / 2 }
}

function magnetStyle(side) {
  if (side === 'top') return { left: '50%', top: -7, transform: 'translateX(-50%)' }
  if (side === 'right') return { right: -7, top: '50%', transform: 'translateY(-50%)' }
  if (side === 'bottom') return { left: '50%', bottom: -7, transform: 'translateX(-50%)' }
  return { left: -7, top: '50%', transform: 'translateY(-50%)' }
}

function makeId(prefix) {
  return `${prefix}-${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function snap(value) {
  return Math.round(value / 10) * 10
}

function clamp(value, min, max) {
  return Math.min(Math.max(value, min), max)
}

const styles = {
  editor: { border: '1px solid #C9DDE3', borderRadius: 8, background: '#F8FBFC', overflow: 'hidden' },
  editorCompact: {
    width: '100%',
    maxWidth: '100%',
    boxSizing: 'border-box',
    border: '1px solid #E7F0F3',
    borderRadius: 6,
    background: '#F8FBFC',
    overflow: 'hidden',
    marginTop: 8,
  },
  toolbar: { minHeight: 46, display: 'flex', alignItems: 'center', gap: 8, padding: '8px 10px', borderBottom: '1px solid #D8E8EC', background: '#fff', flexWrap: 'wrap' },
  toolButton: { width: 44, height: 32, border: '1px solid #1956D8', borderRadius: 6, background: '#fff', display: 'inline-flex', alignItems: 'center', justifyContent: 'center', cursor: 'grab' },
  deleteButton: { minHeight: 32, padding: '6px 12px', border: '1px solid #9B2C2C', borderRadius: 6, background: '#fff', color: '#9B2C2C', fontWeight: 700, cursor: 'pointer' },
  disabledButton: { minHeight: 32, padding: '6px 12px', border: '1px solid #C9DDE3', borderRadius: 6, background: '#F4F8FA', color: '#7B919B', fontWeight: 700 },
  ovalTool: { width: 28, height: 18, border: '2px solid #1956D8', borderRadius: '50%', display: 'block' },
  rectTool: { width: 26, height: 18, border: '2px solid #1956D8', borderRadius: 2, display: 'block' },
  zoomControls: { display: 'inline-flex', alignItems: 'center', gap: 4, marginLeft: 'auto' },
  zoomButton: { width: 32, height: 32, border: '1px solid #1956D8', borderRadius: 6, background: '#fff', color: '#1956D8', fontSize: 16, fontWeight: 800, cursor: 'pointer' },
  zoomLabel: { minWidth: 46, color: '#304653', fontSize: 13, fontWeight: 800, textAlign: 'center' },
  boardFrame: { width: '100%', height: 860, overflow: 'auto', background: '#EEF5F7' },
  boardFrameCompact: {
    width: '100%',
    maxWidth: '100%',
    height: 420,
    overflow: 'auto',
    background: '#EEF5F7',
    boxSizing: 'border-box',
  },
  board: {
    width: boardSize.width,
    height: boardSize.height,
    position: 'relative',
    transformOrigin: 'top left',
    backgroundColor: '#fff',
    backgroundImage: 'linear-gradient(#EDF4F6 1px, transparent 1px), linear-gradient(90deg, #EDF4F6 1px, transparent 1px)',
    backgroundSize: '20px 20px',
  },
  svgLayer: { position: 'absolute', inset: 0, pointerEvents: 'none', overflow: 'visible' },
  node: { position: 'absolute', boxSizing: 'border-box', background: '#FFFFFF', border: '2px solid #304653', boxShadow: '0 6px 16px rgba(9,34,42,0.08)', zIndex: 3 },
  ovalNode: { borderRadius: '50%' },
  rectNode: { borderRadius: 4 },
  selectedNode: { borderColor: '#1956D8', boxShadow: '0 0 0 3px rgba(25,86,216,0.14)' },
  dragHandle: { height: 18, cursor: 'move', borderBottom: '1px solid rgba(48,70,83,0.12)' },
  nodeText: { width: '100%', height: 'calc(100% - 18px)', boxSizing: 'border-box', border: 'none', outline: 'none', resize: 'none', background: 'transparent', color: '#09222A', fontFamily: 'inherit', fontSize: 15, fontWeight: 700, lineHeight: 1.3, textAlign: 'center', padding: '10px 16px' },
  magnet: { position: 'absolute', width: 14, height: 14, borderRadius: '50%', border: '2px solid #1956D8', background: '#fff', cursor: 'crosshair', zIndex: 4, padding: 0 },
  magnetActive: { background: '#DDE8FF' },
  edgeLabel: { position: 'absolute', zIndex: 5, width: 64, minHeight: 30, boxSizing: 'border-box', border: '1px solid #C9DDE3', borderRadius: 999, background: '#fff', color: '#09222A', fontSize: 15, fontWeight: 700, textAlign: 'center', outline: 'none' },
  edgeLabelActive: { borderColor: '#1956D8', boxShadow: '0 0 0 3px rgba(25,86,216,0.12)' },
  edgeLabelReadOnly: { position: 'absolute', zIndex: 5, minWidth: 40, minHeight: 30, boxSizing: 'border-box', border: '1px solid #C9DDE3', borderRadius: 999, background: '#fff', color: '#09222A', fontSize: 15, fontWeight: 700, textAlign: 'center', padding: '5px 10px' },
}
