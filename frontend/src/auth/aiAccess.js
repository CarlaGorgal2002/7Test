export const AI_VIP_TEACHER_EMAIL = 'pfarias@uade.edu.ar'

export function canTeacherUseAi(user) {
  return user?.role === 'PROFESOR'
    && user?.email?.trim().toLowerCase() === AI_VIP_TEACHER_EMAIL
}
