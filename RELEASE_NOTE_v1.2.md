# 7test
## Sistema de Gestión de Evaluaciones Universitarias
### Versión 1.2 · Release Notes
**Fecha de entrega: 21/05/2026**

**EQUIPO DE DESARROLLO**
Carla Gorgal · Tech Lead / Frontend / DevOps
Mario Besednjak · Backend Lead
Martin Gueler · Backend Support

---

## Contenido

1. Descripción del producto
2. Funcionalidades incluidas en esta versión
3. Novedades respecto de la versión anterior
4. Limitaciones conocidas
5. Acciones requeridas para el equipo de QA
6. Credenciales de acceso
7. Notas técnicas

---

## 1. Descripción del producto

7test es una plataforma web orientada a la digitalización del proceso de evaluación universitaria. El caso de uso piloto es el primer parcial de la materia Testing de Aplicaciones de la UADE.

Esta versión cubre el módulo de resolución de exámenes por parte del alumno (Sprint 3), junto con las funcionalidades de creación y publicación de exámenes del Sprint 2, y el módulo de administración de usuarios del Milestone 1.

---

## 2. Funcionalidades incluidas en esta versión

### Sprint 3 — Resolución del examen por alumno

**Vista del alumno:**
- El alumno ve la lista de exámenes publicados disponibles para rendir.
- Al iniciar un examen, el sistema le asigna un tema automáticamente (asignación determinista basada en el ID del alumno).
- Responde preguntas de texto libre.
- Las respuestas se guardan automáticamente cada vez que el alumno deja de escribir (debounce de 900ms).
- El alumno puede guardar manualmente en cualquier momento.
- El alumno puede entregar el examen de forma definitiva.
- Una vez entregado, el examen pasa a modo lectura y no puede modificarse.
- El alumno puede volver a ver su entrega en cualquier momento.

**Vista del profesor:**
- Ve en tiempo real qué alumnos iniciaron el examen y su estado (En progreso / Entregado).
- Puede cerrar el examen para que no se acepten nuevas entregas.

**Administración y supervisión:**
- El Director de Cátedra y el Administrador pueden ver el estado de todos los exámenes con filtro por estado.

### Sprint 2 — Creación de exámenes (ya incluido desde v1.1)

- Profesor crea exámenes en estado Borrador.
- Agrega temas (Tema A, Tema B, Tema C, etc.).
- Carga preguntas con enunciado, respuesta modelo y puntaje.
- Validación: cada tema debe sumar exactamente 10 puntos antes de poder publicar.
- Publicación del examen para que quede disponible a los alumnos.
- Cierre del examen para cortar nuevas entregas.

### Milestone 1 — Gestión de usuarios y autenticación (ya incluido desde v1.0)

HU-01 a HU-09: alta, edición, desactivación de usuarios; login con redirección por rol; logout; recuperación de contraseña; configuración de política de contraseñas.

---

## 3. Novedades respecto de la versión anterior (v1.1)

### Resolución de exámenes por alumno
El alumno puede ahora rendir exámenes reales cargados por el profesor. El sistema asigna un tema, presenta las preguntas, guarda las respuestas automáticamente y registra la entrega final.

### Cierre de examen por el profesor
El profesor puede cerrar un examen publicado desde su panel para que deje de aceptar nuevas entregas.

### Supervisión para Director y Administrador
El Director de Cátedra tiene una vista de supervisión de todos los exámenes con filtro por estado. El Administrador cuenta con la misma vista en su pestaña "Examenes".

### Pruebas unitarias — Sprint 3
Se agregaron pruebas unitarias de caja blanca para `ExamService` y `ExamSubmissionService` con JUnit 5 + Mockito:
- Crear examen en borrador como profesor válido
- Publicar examen con cada tema en 10 puntos
- Rechazar publicación si algún tema no suma 10
- Rechazar edición de examen ya publicado
- Iniciar examen publicado (crea entrega con respuestas vacías)
- Guardar respuestas en entrega En Progreso
- Rechazar guardado en entrega ya Entregada
- Entregar examen (cambia estado a Entregado con timestamp)

---

## 4. Limitaciones conocidas

### Envío de emails simulado
El endpoint de recuperación de contraseña (HU-08) registra la solicitud únicamente en el log del servidor. El correo no se envía realmente.

### Tokens JWT no renovables
Los tokens expiran después de una hora. El usuario debe volver a iniciar sesión al vencer.

### Datos en memoria (H2)
La base de datos es in-memory. Al reiniciarse el servidor todos los datos se pierden y el sistema vuelve al estado inicial con el administrador por defecto. Es necesario recrear usuarios y exámenes en cada sesión.

### Corrección de exámenes no disponible aún
El profesor puede ver qué alumnos entregaron y en qué estado están, pero no puede ver las respuestas ni asignar puntajes. Esta funcionalidad se implementará en el Sprint 4.

### Demora en el primer request tras inactividad
El backend está en Render.com (plan gratuito). Si no recibe tráfico durante ~15 minutos entra en modo suspensión. El primer request puede tardar hasta 50 segundos.

### Defectos conocidos (intencionales, para el equipo de QA)
Esta versión mantiene los defectos deliberados del Sprint 2. Se incorporan además los siguientes para el Sprint 3:

| Categoría | Descripción |
|-----------|-------------|
| Autenticación | Defecto de login para usuarios no administradores (heredado de Sprint 2) |
| Visual | Problema de contraste en modo oscuro que afecta la legibilidad de al menos un campo |
| Texto | Errores ortográficos y mezcla de idiomas en textos y etiquetas de la interfaz |

---

## 5. Acciones requeridas para el equipo de QA

El equipo de QA no necesita instalar ningún software. El acceso se realiza únicamente desde el navegador.

**Para comenzar las pruebas del Sprint 3:**

1. Acceder a la URL del frontend indicada en la sección de credenciales.
2. Iniciar sesión como administrador y crear al menos un usuario PROFESOR y varios usuarios ALUMNO.
3. Iniciar sesión como PROFESOR y crear un examen con al menos dos temas, cada uno con preguntas que sumen 10 puntos. Publicar el examen.
4. Iniciar sesión como ALUMNO y acceder al examen publicado. Responder las preguntas y entregar.
5. Volver al panel del PROFESOR para verificar el estado de las entregas.

**Nota:** Los datos se pierden ante cualquier reinicio del servidor. Se recomienda recrear el estado de prueba al comienzo de cada sesión.

Para el reporte de defectos, seguir el formato indicado por el equipo de PMs:

`BUG-ID · Version/Run · TC-ID · Ambiente · Descripción · Resultado esperado · Resultado obtenido · STR · Evidencia · Etapa · Prioridad · Criticidad`

---

## 6. Credenciales de acceso

**URL del frontend:** https://7test-frontend.vercel.app
**URL del backend (Swagger UI):** https://seventest-backend.onrender.com/swagger-ui/index.html

**Usuario administrador por defecto**
- Email: `admin@seventest.local`
- Contraseña: `Admin#7T$2026`
- Rol: ADMINISTRADOR

**Roles disponibles para usuarios de prueba**
- ALUMNO
- PROFESOR
- DIRECTOR_DE_CATEDRA
- ADMINISTRADOR

---

## 7. Notas técnicas

- **Backend:** Spring Boot 3 (Java 21), arquitectura hexagonal
- **Base de datos:** H2 in-memory (se reinicia con el servidor)
- **Autenticación:** JWT con BCrypt, expiración de 1 hora
- **Frontend:** React 18 + Vite, hosteado en Vercel
- **Backend hosting:** Render.com (plan gratuito, Docker)
- **API docs:** Swagger UI disponible en `/swagger-ui/index.html`
- **Control de versiones:** GitHub — github.com/CarlaGorgal2002/7Test
- **Pruebas unitarias:** JUnit 5 + Mockito (AuthService, UserService, PasswordPolicyService, ExamService, ExamSubmissionService)
- **Nuevos endpoints (Sprint 3):**
  - `GET /api/exams/published` — exámenes publicados (ALUMNO)
  - `POST /api/submissions/exams/{examId}/start` — iniciar examen (ALUMNO)
  - `GET /api/submissions/mine` — mis entregas (ALUMNO)
  - `PUT /api/submissions/{id}/answers` — guardar respuestas (ALUMNO)
  - `PATCH /api/submissions/{id}/submit` — entregar examen (ALUMNO)
  - `GET /api/submissions/exams/{examId}` — entregas de un examen (PROFESOR)
  - `PATCH /api/exams/{id}/close` — cerrar examen (PROFESOR)
  - `GET /api/exams/supervision` — supervisión de exámenes (ADMINISTRADOR, DIRECTOR_DE_CATEDRA)

---

*7test · Testing de Aplicaciones, UADE · Sprint 3 · 21/05/2026*
