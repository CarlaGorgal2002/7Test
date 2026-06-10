import { describe, expect, it } from 'vitest'
import { canTeacherUseAi } from './aiAccess.js'

describe('canTeacherUseAi', () => {
  it('allows only Pablo Farias teacher account', () => {
    expect(canTeacherUseAi({ role: 'PROFESOR', email: 'pfarias@uade.edu.ar' })).toBe(true)
    expect(canTeacherUseAi({ role: 'PROFESOR', email: 'PFARIAS@UADE.EDU.AR ' })).toBe(true)
  })

  it('rejects regular teachers and non-teachers', () => {
    expect(canTeacherUseAi({ role: 'PROFESOR', email: 'prof.mgueler@uade.edu.ar' })).toBe(false)
    expect(canTeacherUseAi({ role: 'ALUMNO', email: 'pfarias@uade.edu.ar' })).toBe(false)
    expect(canTeacherUseAi(null)).toBe(false)
  })
})
