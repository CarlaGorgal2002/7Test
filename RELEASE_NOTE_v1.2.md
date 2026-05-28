# 7test
## Sistema de Gestión de Evaluaciones Universitarias
### Versión 1.2 · Release Notes
**Fecha de entrega: 28/05/2026**

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

7test es una plataforma web orientada a la digitalización del proceso de evaluación universitaria. El caso de uso piloto es el primer parcial de la materia Testing de Aplicaciones de la UADE. El objetivo central es reemplazar el flujo manual de impresión, distribución y corrección en papel por un sistema accesible desde el navegador, sin instalación de software por parte del usuario.

Esta versión incorpora el módulo completo de gestión y resolución de exámenes, correspondiente al Milestone 2 del proyecto. Permite a los profesores crear, configurar, publicar y cerrar exámenes; a los alumnos visualizarlos, iniciar su resolución y entregar sus respuestas; y a los supervisores monitorear el estado de todos los exámenes del sistema.

---

## 2. Funcionalidades incluidas en esta versión

Además de las funcionalidades del Milestone 1 (HU-01 a HU-09, incluidas en la versión 1.1), esta versión incorpora:

### HU-10: Creación y gestión de exámenes

El profesor puede crear un examen en estado borrador indicando título, descripción, nombre del curso, fecha de disponibilidad y duración en minutos. El examen puede editarse libremente mientras se encuentre en borrador. Un examen publicado no puede ser modificado. Un borrador puede eliminarse en cualquier momento; un examen cerrado puede eliminarse únicamente si no tiene entregas registradas.

### HU-11: Gestión de temas y preguntas

Dentro de cada examen en borrador, el profesor puede agregar, editar y eliminar temas. Cada tema recibe automáticamente un color identificador de la paleta del sistema. Dentro de cada tema, el profesor puede agregar, editar y eliminar preguntas. Cada pregunta cuenta con un enunciado, una respuesta modelo y un puntaje asignado. El puntaje puede distribuirse libremente entre las preguntas de un tema mientras el examen esté en borrador. Las preguntas soportan dos tipos de contenido especial además de texto libre: árbol de decisión y tabla de decisión, accesibles mediante un editor visual integrado.

### HU-12: Publicación y cierre de examen

El profesor puede publicar un examen que cumpla con las siguientes condiciones: tener al menos un tema, que cada tema tenga al menos una pregunta, que cada tema sume exactamente diez puntos, y que todas las preguntas tengan enunciado y respuesta modelo completos. Al publicarse, el examen queda visible para los alumnos. El profesor puede cerrar el examen en cualquier momento posterior a la publicación para impedir nuevas entregas.

### HU-13: Vista de exámenes publicados para alumnos

El alumno accede al listado de exámenes publicados y puede ver el detalle de cada uno, incluyendo sus temas y preguntas. Desde esta pantalla puede iniciar la resolución de un examen disponible. Al iniciarlo, el sistema le asigna un tema de forma determinista según su identificador de usuario.

### HU-14: Supervisión de exámenes

Los usuarios con rol ADMINISTRADOR o DIRECTOR_DE_CATEDRA pueden acceder a la vista de supervisión, que muestra todos los exámenes del sistema con filtrado opcional por estado (borrador, publicado o cerrado).

### HU-15: Inicio y entrega de examen

El alumno puede iniciar un examen publicado, lo que genera un intento en estado EN_PROGRESO. Durante la sesión puede guardar sus respuestas parcialmente; el sistema las guarda automáticamente tras cada pausa en la escritura. Al entregar, el intento pasa al estado ENTREGADO y las respuestas quedan registradas de forma definitiva. Una vez entregado, el intento no puede ser modificado.

---

## 3. Novedades respecto de la versión anterior

### Módulo de exámenes

Se incorpora el flujo completo de gestión de exámenes: creación por el profesor, configuración de temas y preguntas con puntaje, publicación con validaciones de integridad, cierre, y visualización diferenciada por rol.

### Editor de árbol de decisión y tabla de decisión

Los profesores pueden redactar respuestas modelo y enunciados utilizando un editor visual de árbol de decisión o tabla de decisión, además de texto libre. El sistema valida al momento de publicar que estas estructuras contengan información.

### Módulo de entregas

Se implementa el ciclo de vida de la entrega del alumno: inicio de sesión de examen, guardado progresivo de respuestas y envío final. El sistema previene la modificación de respuestas tras el envío.

### Exámenes de muestra precargados

El sistema incluye tres exámenes de muestra correspondientes a los temas A, B y C del modelo de examen de la materia. Estos exámenes están disponibles en la vista del profesor sin necesidad de crearlos y son de solo lectura. Su eliminación está protegida por un token de desarrollo.

### Pruebas unitarias — Sprint 2

Se agregaron pruebas unitarias de caja blanca para `ExamService` y `ExamSubmissionService` con JUnit 5 + Mockito. Casos cubiertos:

- Crear examen en borrador como profesor válido
- Publicar examen con cada tema sumando exactamente 10 puntos
- Rechazar publicación si algún tema no suma 10 puntos
- Rechazar publicación si alguna pregunta tiene enunciado o respuesta modelo vacíos
- Rechazar publicación con árbol de decisión vacío como respuesta modelo
- Rechazar publicación con tabla de decisión vacía como respuesta modelo
- Rechazar edición de examen publicado
- Permitir agregar preguntas con contenido vacío en borrador (para redistribución de puntaje)
- Permitir que el total de puntos supere 10 durante la edición (redistribución libre en borrador)
- Eliminar examen borrador
- Rechazar eliminación de examen cerrado con entregas registradas
- Iniciar examen publicado (crea entrega EN_PROGRESO)
- Guardar respuestas en entrega en progreso
- Rechazar guardado en entrega ya entregada
- Entregar examen (cambia estado a ENTREGADO con timestamp)
- Rechazar inicio de examen en estado no publicado

Se añade además un quinto caso a la suite de caja blanca de bugs conocidos (`KnownBugWhiteBoxChecks`), que documenta el defecto BUG-02 incorporado en esta versión (ver sección de defectos conocidos).

### Defecto adicional incorporado en esta versión (intencional)

Se incorpora un nuevo defecto intencional en el módulo de recuperación de contraseña: el mecanismo de búsqueda de usuario por nombre completo devuelve información que no debería ser accesible desde un endpoint no autenticado, comprometiendo la privacidad del usuario registrado. Este defecto es intencional y forma parte del conjunto de bugs para que el equipo de QA identifique y reporte.

---

## 4. Limitaciones conocidas

### Corrección de exámenes no implementada

Las entregas de los alumnos quedan registradas en el sistema pero no se procesan automáticamente. La corrección y calificación deberán realizarse de forma manual o se implementarán en un sprint posterior.

### Notificaciones de entrega no implementadas

El sistema no envía notificaciones al profesor cuando un alumno entrega un examen. Esta funcionalidad se incorporará en un sprint posterior.

### Envío de emails simulado

El endpoint de recuperación de contraseña (HU-08) funciona correctamente a nivel de API, pero el correo electrónico no se envía realmente. La notificación se registra únicamente en el log del servidor.

### Tokens JWT no renovables

Los tokens de sesión tienen una validez de una hora y no se renuevan automáticamente. Al vencer, el usuario debe volver a iniciar sesión.

### Datos en memoria

La base de datos es in-memory (H2). Cada vez que el servidor se reinicia, todos los datos se pierden y el sistema vuelve al estado inicial con únicamente el usuario administrador por defecto. Es necesario recrear usuarios y exámenes al comienzo de cada sesión de prueba.

### Demora en el primer request tras inactividad

El servidor backend está alojado en un plan gratuito de Render.com. Si no recibe tráfico durante aproximadamente quince minutos, entra en modo de suspensión. El primer request posterior puede tardar hasta cincuenta segundos en recibir respuesta. A partir de ese punto el sistema responde con normalidad.

### Defectos conocidos (intencionales, para el equipo de QA)

Esta versión mantiene los defectos intencionales de la versión 1.1 e incorpora uno adicional. Se indica la categoría de cada defecto sin revelar su ubicación exacta, para no condicionar el trabajo de testing:

— Existe un defecto de autenticación en el flujo de login para usuarios no administradores (heredado de v1.1).

— Existe un problema de contraste visual en la interfaz de modo oscuro que afecta la legibilidad de al menos un campo de entrada (heredado de v1.1).

— La interfaz presenta errores ortográficos y mezcla de idiomas en algunos textos y etiquetas (heredado de v1.1).

— Se añade un defecto en el módulo de recuperación de contraseña: la respuesta del sistema ante una búsqueda por nombre revela información sobre los usuarios registrados que no debería ser accesible para usuarios no autenticados (nuevo en v1.2).

---

## 5. Acciones requeridas para el equipo de QA

El equipo de QA no necesita instalar ningún software. El acceso a la aplicación se realiza únicamente desde el navegador.

Para comenzar las pruebas se recomienda:

— Acceder a la URL del frontend indicada en la sección de credenciales.
— Iniciar sesión con las credenciales del administrador por defecto.
— Crear al menos un usuario con rol PROFESOR y varios usuarios con rol ALUMNO desde el panel de administración, antes de iniciar la sesión de prueba.
— Iniciar sesión como PROFESOR y crear un examen con al menos dos temas, cada uno con preguntas cuyo puntaje sume exactamente diez puntos. Publicar el examen.
— Iniciar sesión como ALUMNO, acceder al examen publicado, responder las preguntas y entregar.
— Verificar el estado de las entregas desde el panel del PROFESOR.
— Si al intentar iniciar sesión el sistema tarda varios segundos sin responder, aguardar hasta un minuto. Es el comportamiento normal del primer request tras un periodo de inactividad.

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
- **Nuevos endpoints (v1.2):**
  - `POST /api/exams` — crear examen en borrador (PROFESOR)
  - `PUT /api/exams/{id}` — editar examen en borrador (PROFESOR)
  - `DELETE /api/exams/{id}` — eliminar examen (PROFESOR)
  - `POST /api/exams/{id}/topics` — agregar tema (PROFESOR)
  - `PUT /api/exams/{id}/topics/{topicId}` — editar tema (PROFESOR)
  - `DELETE /api/exams/{id}/topics/{topicId}` — eliminar tema (PROFESOR)
  - `POST /api/exams/{id}/topics/{topicId}/questions` — agregar pregunta (PROFESOR)
  - `PUT /api/exams/{id}/topics/{topicId}/questions/{qId}` — editar pregunta (PROFESOR)
  - `DELETE /api/exams/{id}/topics/{topicId}/questions/{qId}` — eliminar pregunta (PROFESOR)
  - `PATCH /api/exams/{id}/publish` — publicar examen (PROFESOR)
  - `PATCH /api/exams/{id}/close` — cerrar examen (PROFESOR)
  - `GET /api/exams/mine` — mis exámenes (PROFESOR)
  - `GET /api/exams/published` — exámenes publicados (ALUMNO)
  - `GET /api/exams/supervision` — supervisión de exámenes (ADMINISTRADOR, DIRECTOR_DE_CATEDRA)
  - `POST /api/submissions/exams/{examId}/start` — iniciar examen (ALUMNO)
  - `PUT /api/submissions/{id}/answers` — guardar respuestas (ALUMNO)
  - `PATCH /api/submissions/{id}/submit` — entregar examen (ALUMNO)
  - `GET /api/submissions/mine` — mis entregas (ALUMNO)
  - `GET /api/submissions/exams/{examId}` — entregas de un examen (PROFESOR)

---

*7test · Testing de Aplicaciones, UADE · Sprint 2 · 28/05/2026*
