# 7test
## Sistema de Gestión de Evaluaciones Universitarias
### Versión 3.0 · Release Notes
**Fecha de entrega:** 03/06/2026

---

**EQUIPO DE DESARROLLO**
- **Carla Gorgal** · Tech Lead / Frontend / DevOps
- **Mario Besednjak** · Backend Lead
- **Martin Gueler** · Backend Support

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

7test es una plataforma web orientada a la digitalización del proceso de evaluación universitaria. El caso de uso piloto es el primer parcial de la materia Testing de Aplicaciones de la UADE. El objetivo central es reemplazar el flujo manual de impresión, distribución y corrección en papel por un sistema accesible desde el navegador, sin instalación de software por parte del usuario.

Esta versión incorpora el módulo completo de rendición del examen de punta a punta, correspondiente al Milestone 3 del proyecto. Permite al alumno ver los exámenes disponibles, seleccionar su tema, responder y entregar. El profesor puede publicar el examen, monitorear el progreso de los alumnos en tiempo real, agregar tiempo extra cuando el temporizador vence, y corregir y devolver las entregas. Los datos ahora persisten entre reinicios gracias a la migración a PostgreSQL.

---

## 2. Funcionalidades incluidas en esta versión

Además de las funcionalidades de los Milestones 1 y 2 (HU-01 a HU-15), esta versión incorpora:

### HU-28: Dashboard del alumno
El alumno accede a un dashboard con todos sus exámenes disponibles y sus entregas. Los exámenes en progreso o disponibles aparecen primero; los entregados y cerrados quedan como histórico. Cada examen muestra título, curso, duración y estado. Si tiene fecha configurada, también se muestra.

### HU-29: Selección manual de tema
Al acceder a un examen disponible, el alumno ve una pantalla con botones grandes para cada tema. Debe seleccionar uno antes de poder comenzar. El tema queda bloqueado una vez confirmado el inicio. Reemplaza la asignación automática de HU-24.

### HU-30: Cambio de tema antes del inicio
El alumno puede cambiar el tema seleccionado mientras no haya confirmado el inicio de su entrega. Una vez iniciada la entrega, el tema queda bloqueado.

### HU-31: Indicador de disponibilidad del examen
La pantalla de selección de tema funciona también como sala de espera. El estado del examen se muestra de forma clara. El botón "Comenzar examen" se habilita solo si el examen está publicado y el alumno seleccionó un tema.

### HU-32: Configuración de fecha y duración del examen
El profesor puede configurar la duración en minutos (obligatorio) y opcionalmente fecha y hora de inicio del examen. La fecha utiliza formato de texto en 24 horas con validación. Una vez publicado, la configuración queda bloqueada.

### HU-33: Publicar/Iniciar y cerrar examen
El profesor dispone de un botón "Publicar" sobre exámenes en borrador válidos, con confirmación previa. Al publicar se registra la fecha y hora de inicio. El profesor puede cerrar el examen para impedir nuevas entregas, también con confirmación.

### HU-34: Panel de entregas del examen (vista profesor)
El panel muestra los alumnos con entrega creada, su tema y estado. Incluye contadores por estado (En progreso / Entregado / Total), tiempo restante del examen y etiqueta derivada "Excedido de tiempo" para entregas en progreso fuera del plazo.

### HU-35: Timer informativo para el alumno
El alumno ve un temporizador visible durante toda la resolución, calculado a partir de la fecha/hora de publicación más la duración configurada. Cuando llega a cero se muestra un aviso visual. El vencimiento no bloquea la entrega.

### HU-36: Responder preguntas de texto libre
El alumno responde preguntas en campos de texto con guardado automático y manual. Las respuestas quedan bloqueadas una vez entregado el examen.

### HU-37: Responder con árbol y tabla de decisión
Para preguntas configuradas como árbol o tabla de decisión, el alumno ve el editor correspondiente. Las respuestas se guardan automáticamente. Los editores quedan en modo solo lectura tras la entrega.

### HU-38: Envío del examen
El alumno dispone del botón "Entregar examen" con confirmación previa. Al confirmar, las respuestas pendientes se guardan antes de entregar. La entrega pasa a estado ENTREGADO y el alumno accede a una pantalla de confirmación con opciones de ir al inicio o ver sus respuestas.

---

## 3. Novedades respecto de la versión anterior

### Selección de tema manual (cambio arquitectónico)
La asignación automática de temas por hash de usuario fue reemplazada por selección manual. El endpoint `POST /api/submissions/exams/{id}/start` ahora requiere `topicId` en el body. La card HU-24 de Trello fue actualizada.

### Corrección manual de exámenes
El profesor puede calificar cada entrega asignando un puntaje (en incrementos de 0,25, de 0 hasta el máximo de la pregunta) y un comentario de devolución por pregunta. El panel de calificación ocupa toda la pantalla. Al cerrar el panel las calificaciones se guardan automáticamente.

### Publicación de devoluciones
Cuando todas las entregas de un examen cerrado están calificadas, aparece el botón "Entregar devoluciones". Al confirmarlo, los alumnos acceden a sus puntajes y comentarios desde la vista de su entrega.

### Tiempo extra
Cuando el temporizador del examen vence, el profesor recibe un popup con la opción de agregar tiempo extra (entre 1 y 60 minutos, máximo una vez). Si no responde en 10 minutos, el examen se cierra automáticamente.

### Dashboard del profesor rediseñado
El panel del profesor ahora muestra una landing page con 4 tarjetas de navegación agrupando los exámenes por estado. Al crear o abrir un examen se navega a una vista de detalle sin distracciones. La flechita de atrás del browser vuelve al home.

### Creación y gestión de temas mejorada
Los temas se nombran automáticamente (A, B, C...) sin necesidad de ingresarlos manualmente. Al crear un examen, el Tema A se genera automáticamente con la plantilla base. Al eliminar un tema, los restantes se renombran ordenadamente. Se expandió la paleta a 10 colores distintos sin repetición.

### Fondo pastel por tema del alumno
Durante la resolución, el fondo de la pantalla del alumno adopta el color pastel correspondiente a su tema, permitiendo al profesor identificar visualmente qué tema está rindiendo cada alumno con solo pasar por el aula.

### Editores visuales mejorados
- **Tabla de decisión:** soporte para combinar y separar celdas arrastrando la selección.
- **Árbol de decisión:** canvas ampliado (860px), nodos más grandes, flechas más gruesas, posibilidad de reubicar extremos de flechas arrastrando, pan del canvas haciendo click en el fondo, zoom por defecto al 85%.

### Login rediseñado
El login ahora usa un layout dividido: panel izquierdo con gradiente azul, logo y tagline; panel derecho blanco con el formulario.

### Persistencia de datos (PostgreSQL)
La base de datos fue migrada de H2 in-memory a PostgreSQL hosteado en Render. Los datos persisten entre reinicios y redeployments.

### Usuarios pre-cargados
Se incorporaron 47 usuarios con emails `@uade.edu.ar` al sistema, generados automáticamente al iniciar el backend. Incluye alumnos, profesores y el administrador.

### Validación de fecha de publicación
El sistema impide publicar un examen con una fecha de inicio anterior al momento actual, tanto en el frontend (con mensaje específico) como en el backend.

### Estados derivados del examen
Los estados del panel del profesor ahora distinguen: **Borrador**, **Programado** (fecha futura), **En curso** (timer activo), **Cerrado (por devolver)** y **Cerrado (ya devueltos)**.

### Pruebas unitarias · Sprint 3
Se agregaron 4 nuevos casos de prueba para `ExamSubmissionService`:
- Iniciar examen con tema válido (crea entrega EN_PROGRESO)
- Rechazar inicio sin topicId
- Rechazar inicio con topicId inexistente
- Devolver entrega existente sin cambiar tema

---

## 4. Limitaciones conocidas

### Actualización en tiempo real no implementada
El panel del profesor no se actualiza automáticamente. El profesor debe recargar la página para ver nuevas entregas. WebSockets y SSE quedan fuera del scope de esta versión.

### Corrección con IA no implementada
Las entregas se corrigen manualmente. La corrección asistida por IA se implementará en una versión posterior.

### Publicación de notas no implementada
El sistema permite devolver comentarios y puntajes por pregunta, pero no calcula ni publica una nota final. Esta funcionalidad se incorporará en el siguiente sprint.

### Envío de emails simulado
El endpoint de recuperación de contraseña (HU-08) funciona a nivel de API pero el correo no se envía realmente. La notificación se registra en el log del servidor.

### Tokens JWT no renovables
Los tokens de sesión tienen validez de una hora y no se renuevan. Al vencer, el usuario debe volver a iniciar sesión.

### Demora en el primer request tras inactividad
El servidor backend está en un plan gratuito de Render. Si no recibe tráfico durante 15 minutos, entra en modo de suspensión. El primer request posterior puede tardar hasta 50 segundos.

### Defectos conocidos (intencionales, para el equipo de QA)
Esta versión mantiene los defectos intencionales de versiones anteriores:
- Defecto de autenticación en el flujo de login para usuarios no administradores (heredado de v1.1).
- Problema de contraste visual en la interfaz de modo oscuro (heredado de v1.1).
- Errores ortográficos y mezcla de idiomas en algunos textos y etiquetas (heredado de v1.1).
- Defecto en el módulo de recuperación de contraseña que revela información de usuarios registrados (heredado de v2.0).

---

## 5. Acciones requeridas para el equipo de QA

El equipo de QA no necesita instalar ningún software. El acceso se realiza desde el navegador.

Para comenzar las pruebas se recomienda:

- Acceder a la URL del frontend indicada en la sección de credenciales.
- Los usuarios ya están precargados en el sistema; no es necesario crearlos.
- Iniciar sesión como **PROFESOR** con las credenciales indicadas, crear un examen con al menos dos temas y publicarlo.
- Iniciar sesión como **ALUMNO**, seleccionar un tema manualmente, responder las preguntas y entregar.
- Verificar el panel de entregas desde el **PROFESOR**, calificar cada entrega y publicar las devoluciones.
- Verificar que el alumno puede ver sus puntajes y comentarios.
- Si al intentar iniciar sesión el sistema tarda sin responder, aguardar hasta un minuto (comportamiento normal del primer request tras inactividad).

Para el reporte de defectos, seguir el formato indicado por el equipo de PMs:

`BUG-ID · Version/Run · TC-ID · Ambiente · Descripción · Resultado esperado · Resultado obtenido · STR · Evidencia · Etapa · Prioridad · Criticidad`

---

## 6. Credenciales de acceso

**URL del frontend:** https://7test-frontend.vercel.app  
**URL del backend (Swagger UI):** https://seventest-backend.onrender.com/swagger-ui/index.html

### Usuario administrador por defecto
| Campo | Valor |
|-------|-------|
| Email | `admin@seventest.local` |
| Contraseña | `Admin#7T$2026` |
| Rol | ADMINISTRADOR |

### Usuarios precargados (selección)

| Nombre | Email | Contraseña | Rol |
|--------|-------|------------|-----|
| Pablo Farias | `pfarias@uade.edu.ar` | `PabloFarias123` | PROFESOR |
| Claudio Godio | `cgodio@uade.edu.ar` | `ClaudioGodio123` | DIRECTOR_DE_CATEDRA |
| Carla Gorgal | `cgorgal@uade.edu.ar` | `CarlaGorgal123` | ALUMNO |
| Mario Besednjak | `mbesednjak@uade.edu.ar` | `MarioBesednjak123` | ALUMNO |
| Martin Gueler | `mgueler@uade.edu.ar` | `MartinGueler123` | ALUMNO |
| Nelson Carreño | `NelsonCarreno@uade.edu.ar` | `NelsonCarreno123` | ALUMNO |
| Brian Durán Vargas | `BrianDuran@uade.edu.ar` | `BrianDuran123` | ALUMNO |

> El sistema incluye 47 alumnos y profesores precargados con el patrón `NombreApellido@uade.edu.ar` / `NombreApellido123`. Ver documentación completa de usuarios para la lista completa.

---

## 7. Notas técnicas

| Componente | Detalle |
|------------|---------|
| **Backend** | Spring Boot 3 (Java 21), arquitectura hexagonal |
| **Base de datos** | PostgreSQL hosteado en Render (datos persistentes) |
| **Autenticación** | JWT con BCrypt, expiración de 1 hora |
| **Frontend** | React 18 + Vite, hosteado en Vercel |
| **Backend hosting** | Render.com (plan gratuito, Docker) |
| **API docs** | Swagger UI en `/swagger-ui/index.html` |
| **Control de versiones** | GitHub — github.com/CarlaGorgal2002/7Test |
| **Pruebas unitarias** | JUnit 5 + Mockito (AuthService, UserService, PasswordPolicyService, ExamService, ExamSubmissionService) |

### Nuevos endpoints (v3.0)

- `POST /api/submissions/exams/{id}/start` — ahora requiere `{ topicId }` en el body (ALUMNO)
- `PUT /api/submissions/{id}/grade` — calificar entrega por pregunta (PROFESOR)
- `GET /api/submissions/{id}` — ver entrega individual (PROFESOR)
- `PATCH /api/exams/{id}/publish-feedback` — publicar devoluciones a alumnos (PROFESOR)
- `PATCH /api/exams/{id}/add-extra-time` — agregar tiempo extra al examen (PROFESOR)

---

*7test · Testing de Aplicaciones, UADE · Sprint 3 · 03/06/2026*
