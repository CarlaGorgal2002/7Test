# CONTEXTO COMPLETO DEL PROYECTO 7test
### Documento de referencia para sesiones de IA — última actualización: 26/05/2026

Este documento fue generado para que una sesión futura de Claude Code pueda retomar el trabajo sin depender del historial del chat anterior. Contiene TODO lo relevante: hosting, credenciales, arquitectura, bugs intencionales, sprints, incidentes operativos, y pendientes.

---

## 1. Qué es 7test

7test es una plataforma web de digitalización de evaluaciones universitarias. El caso de uso piloto es el primer parcial de la materia **Testing de Aplicaciones** de la **UADE** (Universidad Argentina de la Empresa).

El sistema permite:
- Al administrador: gestionar usuarios y política de contraseñas.
- Al profesor: crear exámenes con temas y preguntas, publicarlos, cerrarlos, y ver el estado de entregas.
- Al alumno: rendir exámenes publicados, responder preguntas de texto libre, y entregar.
- Al director de cátedra: supervisar todos los exámenes del sistema.

---

## 2. Equipo de desarrollo

| Nombre | Rol |
|--------|-----|
| **Carla Gorgal** | Tech Lead / Frontend / DevOps |
| **Mario Besednjak** | Backend Lead |
| **Martín Gueler** | Backend Support |

Repositorio de control de versiones: https://github.com/CarlaGorgal2002/7Test  
Branch principal: `main`  
Git user para commits desde Claude: CarlaGorgal2002

**Nota:** El repositorio original en el Sprint 1 estaba bajo la cuenta `besednjak/7test`. Fue migrado/traspasado a la cuenta de Carla durante el proceso de desarrollo.

---

## 3. Stack tecnológico

### Frontend
- React 18 + Vite 5
- React Router v6
- Axios (con instancia en `frontend/src/api/client.js`)
- Sin librería de componentes externa (todo CSS-in-JS con `style={{}}`)
- Sin TypeScript (JavaScript puro)
- Sin tests de frontend

### Backend
- Spring Boot 3 / Java 21
- Arquitectura hexagonal (domain → application → infrastructure)
- Spring Security + JWT + BCrypt
- JPA / Hibernate
- Base de datos: H2 in-memory (se reinicia con cada restart del servidor)
- Swagger/OpenAPI 3: disponible en `/swagger-ui/index.html`
- Tests: JUnit 5 + Mockito

### Herramientas de build
- Frontend: `npm` + `vite`
- Backend: Maven (`./mvnw`)

---

## 4. Hosting y URLs actuales (v1.2 en adelante)

| Servicio | Plataforma | URL |
|----------|-----------|-----|
| Frontend | Vercel | https://7test-frontend.vercel.app |
| Backend | Render.com | https://seventest-backend.onrender.com |
| Swagger UI | Render.com | https://seventest-backend.onrender.com/swagger-ui/index.html |
| Repositorio | GitHub | https://github.com/CarlaGorgal2002/7Test |

### Comportamiento de Render.com (plan gratuito)
- El servidor **duerme** tras ~15 minutos sin tráfico.
- El **primer request** luego de inactividad puede tardar hasta **50 segundos**.
- Informar a QA de este comportamiento para que no lo reporten como bug.
- El deploy se actualiza automáticamente al hacer `git push` a `main` con cambios en el backend (Render sí está conectado a GitHub).

---

## 5. Historia del hosting — incidentes críticos

### 5.1. Abandono de Railway.app (Sprint 1 → Sprint 2)

En el **Sprint 1 / Milestone 1**, tanto el frontend como el backend estaban hosteados en **Railway.app**. Las release notes v1.0 muestran placeholders `<URL-RAILWAY>` porque las URLs exactas se comunicaban manualmente al equipo de QA por WhatsApp.

**Problema:** Railway en el plan gratuito resultó ser **inestable**: el servicio se caía con frecuencia, generando downtime impredecible durante las sesiones de QA. Esto impedía que el equipo de QA pudiera hacer pruebas consistentes.

**Decisión:** A partir del Sprint 2, se migró:
- **Backend → Render.com** (plan gratuito, Docker): más estable que Railway, aunque introduce el problema del cold start de ~50s.
- **Frontend → Vercel**: servicio especializado en frontends estáticos, prácticamente sin downtime.

**Vestigio técnico:** En `SecurityConfig.java`, los orígenes CORS permitidos todavía incluyen `*.railway.app` aunque ya no se usa. No remover por las dudas (si algún dev testea localmente con túnel Railway).

### 5.2. Vercel no auto-deployaba desde GitHub (Sprint 3)

Al deployar el Sprint 3, se descubrió que el frontend en Vercel estaba mostrando **código del Sprint 1** (`"Panel de creación de exámenes — próximamente."`) a pesar de que los commits de Sprint 2 y 3 estaban en GitHub.

**Causa raíz:** El proyecto de Vercel **no estaba conectado al repositorio de GitHub** para auto-deploy. Vercel había sido configurado manualmente en algún momento anterior sin linkear el repo.

**Solución aplicada:**
```bash
cd frontend
npx vercel link --yes --project 7test-frontend
npx vercel --prod --yes
```

**Consecuencia permanente:** Vercel **NO hace auto-deploy al hacer `git push`**. Cada vez que se quiera deployar el frontend, hay que correr manualmente desde la carpeta `frontend/`:
```bash
npx vercel --prod --yes
```

El directorio `frontend/.vercel/` contiene la configuración de link. No borrar.

### 5.3. Base de datos: de PostgreSQL a H2 in-memory

En el **Sprint 1**, el backend usaba **PostgreSQL 16** vía Docker Compose en local, y estaba pensado para usar PostgreSQL en producción también (en Railway).

Al migrar a Render.com (plan gratuito), se decidió simplificar y usar **H2 in-memory** en producción para evitar pagar por una base de datos gestionada. Esto tiene la consecuencia de que **todos los datos se pierden en cada restart del servidor**.

La configuración de Docker Compose (con PostgreSQL) sigue existiendo en el repo pero no es usada en producción actualmente.

**Pendiente futuro:** migrar a PostgreSQL en producción (probablemente en Supabase o Railway cuando se suba de plan).

### 5.4. Contraseña del admin cambió entre versiones

- **v1.0:** La contraseña del admin por defecto era `admin1234`.
- **v1.2 (actual):** La contraseña es `Admin#7T$2026` (cumple con la política de contraseñas fuerte que el propio sistema administra).

Si se usa un entorno con datos de v1.0, la contraseña vieja puede que ya no funcione si la política fue actualizada.

---

## 6. Proceso de deploy — paso a paso

### Deploy del frontend a Vercel

**Requisito:** tener Node.js y npm instalados. El package `vercel` se instala on-demand con npx.

```bash
# 1. Desde la raíz del proyecto, entrar a la carpeta del frontend
cd frontend

# 2. Deployar a producción
npx vercel --prod --yes
```

Si es la primera vez en la máquina (o si se borró `frontend/.vercel/`):
```bash
npx vercel link --yes --project 7test-frontend
npx vercel --prod --yes
```

**No usar** `git push` para deployar el frontend — no tiene efecto.

### Deploy del backend a Render.com

El backend **sí está conectado a GitHub**. Basta con:
```bash
git add .
git commit -m "descripción del cambio"
git push
```

Render detecta el push y hace rebuild automático del Docker container.

---

## 7. Desarrollo local

### Levantar el frontend
```bash
cd frontend
npm install
npm run dev
# → http://localhost:5173
```

El proxy de Vite redirige `/api` a `http://localhost:8080`.

### Levantar el backend
```bash
# En la raíz del proyecto
./mvnw spring-boot:run
# → http://localhost:8080
```

- Swagger local: http://localhost:8080/swagger-ui/index.html
- H2 Console: http://localhost:8080/h2-console  
  JDBC URL: `jdbc:h2:mem:testdb`, usuario: `sa`, contraseña: (vacía)

### Variables de entorno

El frontend usa `VITE_API_URL` para apuntar al backend en producción. Si no está definida, usa `/api` (que el proxy de Vite redirige localmente). En Vercel, esta variable debería estar configurada en el dashboard del proyecto apuntando a `https://seventest-backend.onrender.com`.

---

## 8. Credenciales

### Administrador por defecto (se crea al iniciar el servidor)
- **Email:** `admin@seventest.local`
- **Contraseña:** `Admin#7T$2026`
- **Rol:** `ADMINISTRADOR`

### Token de desarrollo
- **Valor:** `4989`
- **Usos:**
  - Desactivar/reactivar usuarios desde el panel del Admin
  - Crear más de 60 usuarios en bulk

### Roles disponibles en el sistema
| Rol | Acceso |
|-----|--------|
| `ADMINISTRADOR` | Panel de usuarios, política de contraseñas, supervisión de exámenes |
| `PROFESOR` | Crear/editar/publicar/cerrar exámenes, ver entregas |
| `ALUMNO` | Rendir exámenes publicados |
| `DIRECTOR_DE_CATEDRA` | Supervisión de todos los exámenes con filtro por estado |

---

## 9. Bugs intencionales (NO CORREGIR — son para QA)

Estos defectos están deliberadamente introducidos. El equipo de QA debe encontrarlos y reportarlos como parte de la cursada.

| Sprint | Categoría | Descripción |
|--------|-----------|-------------|
| 1 | Autenticación | Bug de seguridad en la lógica de login: bajo cierta condición, un usuario puede autenticarse con credenciales de otro usuario (cross-user auth). Está relacionado con la validación de contraseñas durante el login. |
| 2/3 | Visual | Problema de contraste en **modo oscuro** que afecta la legibilidad de al menos un campo de la interfaz. |
| 2/3 | Texto/UX | Errores ortográficos y mezcla de idiomas en etiquetas. Por ejemplo, el botón "Search" en inglés en el panel del Administrador. |

**Regla:** Si en algún sprint se agrega funcionalidad nueva, se mantienen los bugs viejos Y se agregan nuevos bugs para QA.

---

## 10. Paleta de colores y estilos del frontend

El frontend usa estilos inline (`style={{}}`). No hay Tailwind ni CSS externo. La paleta principal:

| Uso | Color |
|-----|-------|
| Header / fondo oscuro | `#09222A` |
| Fondo de página | `#F4F8FA` |
| Texto principal | `#09222A` |
| Azul primario (botones, badges) | `#1956D8` |
| Verde éxito / publicado | `#087A55` / `#DDF6EC` |
| Rojo peligro / cerrado | `#9B2C2C` |
| Gris neutro / borrador | `#4A5565` / `#ECEFF3` |
| Borde inputs | `#C9DDE3` |
| Bordes de cards | `#D8E8EC` |
| Texto secundario / muted | `#536B76` |
| Mensaje de feedback | fondo `#FFF8DF`, borde `#E7CE74`, texto `#5D4700` |

Modo oscuro: existe en algunas pantallas pero tiene el bug intencional de contraste.

---

## 11. Arquitectura del backend

El backend usa arquitectura hexagonal:

```
src/main/java/com/seventest/
├── domain/
│   ├── model/           # Entidades: User, Exam, ExamTopic, ExamQuestion,
│   │                    #   ExamSubmission, AnswerEntry, PasswordPolicy
│   └── port/            # Interfaces de repositorio (UserRepository, ExamRepository, etc.)
├── application/
│   └── service/         # Lógica de negocio:
│                        #   AuthService, UserService, PasswordPolicyService,
│                        #   ExamService, ExamSubmissionService
└── infrastructure/
    ├── web/
    │   ├── controller/  # REST controllers (ver sección 13)
    │   └── dto/         # Request/Response DTOs
    ├── security/        # JwtFilter, SecurityConfig, JwtUtil
    └── persistence/     # Repositorios JPA (implementan los ports del domain)
```

### Consideraciones de arquitectura
- Los controllers no llaman directamente a repositorios; van por los services.
- Los services reciben ports (interfaces) por constructor injection.
- `SecurityConfig` define qué endpoints son públicos y qué roles tienen acceso a cada uno.
- `@PreAuthorize` se usa en los controllers para control de acceso por rol.

---

## 12. Seguridad y CORS

### Endpoints públicos (sin token)
- `POST /api/auth/login`
- `POST /api/auth/password-recovery`
- `GET /h2-console/**`
- `GET /swagger-ui/**`
- `GET /v3/api-docs/**`

### CORS — orígenes permitidos
```
http://localhost:3000
http://localhost:5173
http://127.0.0.1:3000
http://127.0.0.1:5173
*.railway.app          ← vestigio del Sprint 1, no eliminar
*.vercel.app           ← frontend productivo
```

### JWT
- Generado en login, expiración: **1 hora**
- Sin refresh token (el usuario debe re-loguear al vencer)
- Enviado en header `Authorization: Bearer <token>`
- El cliente (Axios) lo agrega automáticamente vía interceptor en `frontend/src/api/client.js`
- Al recibir 401, el interceptor limpia `localStorage` y redirige a `/login`

---

## 13. Endpoints REST completos

### Auth
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | /api/auth/login | Público | Login. Devuelve JWT + objeto user. |
| POST | /api/auth/password-recovery | Público | Recuperación de contraseña (solo loguea en consola, no envía email). |

### Usuarios
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| GET | /api/users | ADMINISTRADOR | Listar usuarios |
| POST | /api/users | ADMINISTRADOR | Crear usuario |
| PUT | /api/users/{id} | ADMINISTRADOR | Editar usuario |
| PATCH | /api/users/{id}/deactivate | ADMINISTRADOR | Desactivar (requiere token 4989) |
| POST | /api/users/bulk | ADMINISTRADOR | Crear usuarios en masa (requiere token 4989 si >60) |

### Política de contraseñas
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| GET | /api/password-policy | ADMINISTRADOR | Ver política actual |
| PUT | /api/password-policy | ADMINISTRADOR | Actualizar política |

### Exámenes
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | /api/exams | PROFESOR | Crear examen en borrador |
| GET | /api/exams/mine | PROFESOR | Listar exámenes propios |
| PUT | /api/exams/{id} | PROFESOR | Editar examen (solo BORRADOR) |
| POST | /api/exams/{id}/topics | PROFESOR | Agregar tema |
| POST | /api/exams/{id}/topics/{topicId}/questions | PROFESOR | Agregar pregunta |
| DELETE | /api/exams/{id}/topics/{topicId}/questions/{questionId} | PROFESOR | Eliminar pregunta |
| PATCH | /api/exams/{id}/publish | PROFESOR | Publicar examen (valida que cada tema sume 10 pts) |
| PATCH | /api/exams/{id}/close | PROFESOR | Cerrar examen |
| GET | /api/exams/published | ALUMNO | Ver exámenes publicados disponibles |
| GET | /api/exams/supervision | ADMINISTRADOR, DIRECTOR_DE_CATEDRA | Todos los exámenes con filtro ?status= |

### Entregas
| Método | Ruta | Rol | Descripción |
|--------|------|-----|-------------|
| POST | /api/submissions/exams/{examId}/start | ALUMNO | Iniciar examen (crea entrega, asigna tema) |
| GET | /api/submissions/mine | ALUMNO | Ver mis entregas |
| PUT | /api/submissions/{id}/answers | ALUMNO | Guardar respuestas (debounce 900ms en frontend) |
| PATCH | /api/submissions/{id}/submit | ALUMNO | Entregar examen definitivamente |
| GET | /api/submissions/exams/{examId} | PROFESOR | Ver entregas de un examen |

---

## 14. Reglas de negocio clave

- Un examen solo puede publicarse si **cada tema suma exactamente 10 puntos**.
- Un examen publicado **no puede editarse** (temas, preguntas, título, etc.).
- La asignación de tema al alumno es **determinista**: `hash(alumnoId) % cantidadTemas`. Siempre el mismo alumno recibe el mismo tema en el mismo examen.
- Una entrega en estado `ENTREGADO` **no puede modificarse** (ni guardar respuestas ni volver a entregar).
- El estado del examen sigue el flujo: `BORRADOR → PUBLICADO → CERRADO`.
- Estados de entrega: `EN_PROGRESO → ENTREGADO`.

---

## 15. Archivos del frontend

### Estructura
```
frontend/
├── src/
│   ├── api/
│   │   └── client.js         # Axios instance + JWT interceptor + 401 handler
│   ├── components/
│   │   └── Logo.jsx           # Logo del sistema (svg inline)
│   └── pages/
│       ├── Login.jsx          # Login con redirección por rol
│       ├── AdminLanding.jsx   # Panel admin: tabs Users / Política / Examenes
│       ├── ProfesorLanding.jsx # Panel profesor: exámenes completo
│       ├── AlumnoLanding.jsx  # Panel alumno: rendir exámenes
│       └── DirectorLanding.jsx # Supervisión de exámenes
├── public/
├── vite.config.js             # Dev proxy /api → localhost:8080
├── vercel.json                # SPA rewrites + cache headers
├── .vercel/                   # Config de link con el proyecto de Vercel (no borrar)
└── package.json
```

### client.js — lógica clave
- `baseURL`: si existe `VITE_API_URL`, usa `${VITE_API_URL}/api`. Si no, usa `/api` (el proxy de Vite lo resuelve en dev).
- Interceptor de request: agrega `Authorization: Bearer <token>` para todos los endpoints excepto los de auth.
- Interceptor de response: en 401 (y no siendo un endpoint de auth), limpia localStorage y redirige a `/login`.

### AlumnoLanding.jsx — flujo completo
1. Al montar: fetch de `/exams/published` y `/submissions/mine`.
2. Cruza ambas listas para saber si el alumno ya inició cada examen.
3. "Iniciar examen" → POST `/submissions/exams/{id}/start`.
4. En el modo de resolución: textarea por pregunta, auto-save con debounce de 900ms → PUT `/submissions/{id}/answers`.
5. "Guardar ahora" → mismo PUT, manual.
6. "Entregar examen" → PATCH `/submissions/{id}/submit`.
7. Post-entrega: modo lectura, botón "Ver entrega".

### ProfesorLanding.jsx — flujo completo
- Sidebar: formulario "Nuevo examen" + lista de exámenes propios.
- Workspace: según el examen seleccionado, muestra temas y preguntas (si BORRADOR, formularios de edición; si PUBLICADO/CERRADO, lista de entregas de alumnos).
- Botón "Publicar" (solo en BORRADOR).
- Botón "Cerrar examen" (solo en PUBLICADO, estilo rojo outline `#9B2C2C`).
- Botón "Actualizar" en el panel de entregas para refrescar manualmente.

---

## 16. Historial de sprints

### Milestone 1 / Sprint 1 — v1.0 (19/05/2026)
HU-01 a HU-09:
- Alta, listado, edición, desactivación/reactivación de usuarios
- Login con redirección por rol
- Logout
- Recuperación de contraseña (simulada)
- Configuración de política de contraseñas
- Landings de Profesor, Alumno y Director mostraban mensaje placeholder "próximamente"

### Sprint 2 — v1.1
- Profesor crea exámenes en borrador
- Agrega temas (Tema A, Tema B, Tema C...) con sugerencia automática del siguiente nombre
- Carga preguntas: enunciado, respuesta modelo, puntaje (mínimo 0.25, máximo 10, paso 0.25)
- Validación: cada tema debe sumar exactamente 10 puntos para poder publicar
- Publicación y la pantalla del alumno ya mostraba el examen (pero sin poder rendirlo)

### Sprint 3 — v1.2 (21/05/2026) — VERSIÓN ACTUAL
- Alumno puede rendir exámenes publicados (flujo completo descripto arriba)
- Profesor puede cerrar exámenes
- Director y Admin: vista de supervisión con filtro por estado
- Pruebas unitarias para `ExamService` y `ExamSubmissionService`

### Sprint 4 — PENDIENTE (NO implementado en ningún branch)
Corrección y notas:
- El profesor ve las respuestas de cada alumno entregado
- Asigna puntaje por pregunta (escala 0 / 0.25 / 0.5 / 0.75 / 1 × puntaje de la pregunta)
- Sugerencia de IA mock (simulada, no IA real)
- Publica la nota final del alumno
- El alumno puede ver su nota una vez publicada

---

## 17. Pruebas unitarias existentes

Todas están en el backend. No hay tests de frontend.

### ExamServiceTest
- Crear examen en borrador como profesor válido
- Publicar examen con cada tema sumando 10 puntos (debe pasar)
- Rechazar publicación si algún tema no suma 10 (debe lanzar excepción)
- Rechazar edición de examen ya publicado

### ExamSubmissionServiceTest
- Iniciar examen publicado (crea entrega con respuestas vacías)
- Guardar respuestas en entrega EN_PROGRESO
- Rechazar guardado en entrega ya ENTREGADA
- Entregar examen (cambia estado a ENTREGADO con timestamp)

### Tests previos (Milestone 1)
- AuthServiceTest
- UserServiceTest
- PasswordPolicyServiceTest

---

## 18. Archivos de documentación en el repo

| Archivo | Contenido |
|---------|-----------|
| `RELEASE_NOTE_v1.0.md` | Sprint 1 — credenciales, HUs, notas técnicas |
| `RELEASE_NOTE_v1.2.md` | Sprint 3 — funcionalidades, endpoints nuevos, instrucciones QA |
| `TESTING_MILESTONE_1.md` | Casos de prueba del Milestone 1 |
| `HELP.md` | Instrucciones generales de ayuda |
| `README.md` | README principal del repositorio |
| `CONTEXTO_PROYECTO.md` | **Este archivo** — referencia completa para Claude |

---

## 19. Sistema de memoria de Claude

Claude mantiene memoria persistente del proyecto en:
```
C:\Users\Napero\.claude\projects\c--Users-Napero-Desktop-7test-hosteado\memory\
```

- `MEMORY.md` — índice de memorias
- `project_7test.md` — contexto del proyecto guardado

Al iniciar una nueva sesión de Claude Code en este directorio, estas memorias se cargan automáticamente. Sin embargo, para contexto detallado (como este documento), es preferible pegar el contenido de `CONTEXTO_PROYECTO.md` directamente en el chat.

---

## 20. Notas operativas importantes

1. **La base de datos se resetea en cada restart de Render.** Hay que recrear usuarios y exámenes al comienzo de cada sesión de QA.

2. **El frontend debe deployarse manualmente** con `npx vercel --prod --yes` desde la carpeta `frontend/`. No basta con hacer `git push`.

3. **El backend se deploya solo** al hacer `git push` a `main` (Render está conectado a GitHub).

4. **Cold start en Render:** el primer request tras ~15 min de inactividad tarda hasta 50 segundos. Informar a QA.

5. **Los tokens JWT vencen en 1 hora** sin posibilidad de renovar. El usuario debe volver a loguearse.

6. **Recuperación de contraseña:** solo loguea en consola del servidor, no envía email real.

7. **Los bugs intencionales deben mantenerse** en cada sprint. Si se agrega una feature nueva, agregar también nuevos bugs para QA del sprint correspondiente.

8. **El token de desarrollo `4989`** es requerido para desactivar usuarios y para crear más de 60 usuarios en bulk. Es una medida de seguridad del entorno de desarrollo.

---

*7test · Testing de Aplicaciones, UADE · Generado 26/05/2026*
