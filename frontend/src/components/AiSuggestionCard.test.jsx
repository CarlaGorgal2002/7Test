import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import AiSuggestionCard from './AiSuggestionCard.jsx'

const ready = {
  id: 'new',
  attemptNumber: 2,
  status: 'READY',
  suggestedFraction: 0.75,
  suggestedScore: 0.3125,
  suggestedComment: 'Comprension mayormente correcta.',
  strengths: ['Distingue niveles'],
  issues: ['Falta precision'],
  sourcePages: [55, 81],
  confidence: 'MEDIUM',
  requiresHumanReview: true,
  reviewReason: 'Verificar ambiguedad',
}

describe('AiSuggestionCard', () => {
  it('muestra advertencia, puntaje exacto e historial', () => {
    render(<AiSuggestionCard suggestion={ready} history={[ready, { id: 'old', attemptNumber: 1, status: 'REJECTED', suggestedScore: 0 }]} maxPoints={0.5} onReview={() => {}} />)
    expect(screen.getByText(/0.3125/)).toBeInTheDocument()
    expect(screen.getByRole('alert')).toHaveTextContent('Verificar ambiguedad')
    expect(screen.getByText(/Ver historial \(2 intentos\)/)).toBeInTheDocument()
  })

  it('envia acciones independientes de aceptar y rechazar', () => {
    const onReview = vi.fn()
    render(<AiSuggestionCard suggestion={ready} maxPoints={1} onReview={onReview} />)
    fireEvent.click(screen.getByRole('button', { name: 'Aceptar sugerencia' }))
    fireEvent.click(screen.getByRole('button', { name: 'Rechazar' }))
    expect(onReview).toHaveBeenNthCalledWith(1, ready, 'accept')
    expect(onReview).toHaveBeenNthCalledWith(2, ready, 'reject')
  })

  it('muestra un fallo individual sin acciones de revision', () => {
    render(<AiSuggestionCard suggestion={{ id: 'failed', attemptNumber: 1, status: 'FAILED', errorSummary: 'Cuota agotada' }} maxPoints={1} onReview={() => {}} />)
    expect(screen.getByText('Cuota agotada')).toBeInTheDocument()
    expect(screen.queryByRole('button')).not.toBeInTheDocument()
  })
})
