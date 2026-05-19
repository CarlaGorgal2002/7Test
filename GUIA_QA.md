# Guía de Testing — 7test v1.0

**Equipo Dev:** Carla Gorgal (Tech Lead), Mario Besednjak (Backend), Martín Gueler (Backend)  
**Sprint:** 1 · **Fecha de release:** 20/05/2026  
**Milestone:** 1 — Login y gestión de usuarios (HU-01 a HU-09)

---

## ¿Qué es 7test?

Una plataforma para gestionar evaluaciones universitarias. En este primer milestone el foco es el sistema de usuarios y autenticación. El alumno ingresa, ve su landing. El profesor, la suya. El administrador gestiona a todos.

---

## Acceso rápido

| Recurso | URL |
|---|---|
| Aplicación (frontend) | https://7test-frontend.vercel.app |
| API documentada (Swagger) | https://7test-hosteado-production.up.railway.app/swagger-ui/index.html |
| Repositorio | https://github.com/CarlaGorgal2002/7test-hosteado |

---

## Usuarios de prueba

> Estos usuarios fueron creados por el equipo Dev para que puedan testear sin necesidad de una cuenta admin.

| Nombre | Email | Contraseña | Rol |
|---|---|---|---|
| *(completar antes de entregar)* | alumno1@uade.edu.ar | Alumno1234 | Alumno |
| *(completar antes de entregar)* | profesor1@uade.edu.ar | Profe1234 | Profesor |
| *(completar antes de entregar)* | director1@uade.edu.ar | Director1234 | Director de Cátedra |

> **Nota para los QA:** si necesitan crear más usuarios para sus pruebas, pídanselo al equipo Dev.

---

## FORMA 1 — Testing desde la UI (modo QA/PM)

*Esta es la forma que los PMs planificaron. Se testea la aplicación como la usaría un usuario real.*

### Paso 1: Abrir la aplicación

Entrá a **https://7test-frontend.vercel.app** desde cualquier navegador.

### Paso 2: Iniciar sesión (HU-05)

1. Ingresá el email y la contraseña de uno de los usuarios de prueba de la tabla de arriba.
2. Hacé click en el botón de login.
3. Si las credenciales son correctas, el sistema te redirige automáticamente a la pantalla que corresponde a tu rol.

**Qué verificar:**
- ¿El login funciona con credenciales válidas? → debería redirigirte
- ¿El login falla con credenciales inválidas? → debería mostrar un mensaje de error
- ¿El mensaje de error revela si el email existe o no? (según HU-05, NO debería revelar eso)
- ¿Podés entrar con el email de un usuario y la contraseña de OTRO usuario? (esto es algo para investigar)

### Paso 3: Verificar la landing por rol (HU-07)

Según el rol con el que iniciaste sesión, deberías ver una pantalla distinta:
- **Alumno** → pantalla con mensaje de bienvenida y rol "Alumno"
- **Profesor** → pantalla con mensaje de bienvenida y rol "Profesor"
- **Director de Cátedra** → pantalla con mensaje de bienvenida y rol "Director de Cátedra"
- **Administrador** → panel de gestión de usuarios

**Qué verificar:**
- ¿El nombre del usuario aparece en la pantalla?
- ¿El mensaje de bienvenida es correcto y está bien escrito?
- ¿Si escribís en la barra de direcciones la URL de otro rol (ej: `/admin` siendo alumno), podés entrar?

### Paso 4: Cerrar sesión (HU-06)

1. Buscá el botón "Cerrar sesión" en la pantalla.
2. Hacé click en él.
3. El sistema debería redirigirte al login.

**Qué verificar:**
- ¿El botón está visible?
- ¿Después de cerrar sesión, si intentás volver a la pantalla anterior (botón atrás del navegador), podés acceder?

### Paso 5: Recuperación de contraseña (HU-08)

1. En la pantalla de login, hacé click en **"¿Olvidaste tu contraseña?"**.
2. El sistema te pide un dato para identificarte.
3. Completá el formulario y enviá.

**Qué verificar:**
- ¿El mensaje de respuesta revela información sobre el usuario? (según HU-08, NO debería)
- ¿El formulario acepta cualquier valor o valida el formato?

### Paso 6: Panel de administración — solo con rol Administrador (HU-01 a HU-04 y HU-09)

Iniciá sesión como administrador para acceder al panel completo. **Las credenciales de admin las provee el equipo Dev por mensaje privado — no están en este documento.**

Desde el panel podés:

**Crear usuario (HU-01):**
1. Completá el formulario: nombre completo, email, rol, contraseña.
2. Enviá el formulario.
3. El usuario nuevo debería aparecer en el listado.

**Qué verificar:**
- ¿Podés crear un usuario con email duplicado?
- ¿La contraseña valida las reglas de seguridad configuradas?
- ¿El campo "rol" muestra todas las opciones: Alumno, Profesor, Director de Cátedra, Administrador?

**Buscar y listar usuarios (HU-02):**
1. Entrá a la sección de gestión de usuarios.
2. Usá el buscador para filtrar por nombre o email.
3. Probá los filtros de rol y estado.

**Editar usuario (HU-03):**
1. Desde el listado, hacé click en editar sobre un usuario.
2. Modificá algún dato (nombre, email, rol).
3. Guardá los cambios.

**Desactivar / reactivar usuario (HU-04):**
1. Desde el listado, desactivá un usuario.
2. Intentá iniciar sesión con ese usuario.
3. Verificá que el sistema no le permite entrar y muestra un mensaje claro.
4. Reactivá el usuario desde el panel y volvé a intentar el login.

**Política de contraseñas (HU-09):**
1. Buscá la sección de configuración de política de contraseñas en el panel admin.
2. Cambiá los requisitos (longitud mínima, mayúsculas, números, etc.).
3. Intentá crear un usuario cuya contraseña NO cumpla la nueva política.

---

## FORMA 2 — Testing desde la API (modo Dev/QA avanzado)

*Esta es la forma que el equipo Dev recomienda para testing más profundo, sin depender de la UI.*

### Paso 1: Abrir Swagger UI

Entrá a **https://7test-hosteado-production.up.railway.app/swagger-ui/index.html**

Vas a ver todos los endpoints documentados. Podés ejecutarlos directamente desde ahí.

### Paso 2: Autenticarte en Swagger

Para usar endpoints que requieren login (todos excepto `/auth/login`):

1. Ejecutá el endpoint `POST /api/auth/login` con las credenciales de cualquier usuario.
2. Copiá el `token` que devuelve la respuesta.
3. Arriba a la derecha en Swagger, hacé click en **"Authorize"** (el candado).
4. En el campo escribí: `Bearer ` seguido del token copiado (con espacio entre Bearer y el token).
5. Hacé click en "Authorize" → "Close".

A partir de ese momento, todos los requests van autenticados.

### Paso 3: Probar los endpoints directamente

Ejemplos de endpoints útiles para testing:

| Endpoint | Método | Qué hace |
|---|---|---|
| `/api/auth/login` | POST | Login — devuelve token JWT |
| `/api/auth/logout` | POST | Cierra la sesión (invalida el token) |
| `/api/auth/password-recovery` | POST | Solicita recuperación de contraseña |
| `/api/users` | GET | Lista todos los usuarios (requiere rol ADMIN) |
| `/api/users` | POST | Crea un usuario (requiere rol ADMIN) |
| `/api/users/{id}` | PUT | Edita un usuario (requiere rol ADMIN) |
| `/api/users/{id}/status` | PATCH | Activa/desactiva un usuario (requiere rol ADMIN) |
| `/api/policy` | GET | Obtiene la política de contraseñas vigente |
| `/api/policy` | PUT | Actualiza la política (requiere rol ADMIN) |

### Paso 4 (opcional): Levantar el ambiente local con Docker

Si querés testear localmente sin depender del servidor hosteado:

**Requisitos:** tener **Docker Desktop** instalado (descargá de https://www.docker.com/products/docker-desktop/).

1. Descargá el repositorio: https://github.com/CarlaGorgal2002/7test-hosteado (botón verde "Code" → "Download ZIP")
2. Descomprimí el ZIP en cualquier carpeta.
3. Abrí una terminal (PowerShell en Windows, Terminal en Mac) dentro de esa carpeta.
4. Ejecutá:
   ```
   docker-compose up --build
   ```
5. Esperá ~2-3 minutos hasta que veas el mensaje `Started Application`.
6. Accedé a:
   - API local: http://localhost:8080/swagger-ui/index.html
   - La app funciona igual que la hosteada, pero sobre tu máquina y con base de datos propia.

Para detenerlo: `Ctrl + C` en la terminal, luego `docker-compose down`.

---

## Cómo reportar bugs

Según el formato que definieron los PMs, cada bug va en la planilla de Google Sheets del equipo con estas columnas:

| Campo | Descripción |
|---|---|
| **BUG-ID** | Número correlativo del bug (BUG-001, BUG-002...) |
| **Versión / Run** | Versión de la app (v1.0) y número de run (R01, R02...) |
| **TC-ID** | ID del caso de prueba que detectó el bug |
| **Ambiente** | Dónde se encontró: UI hosteada / Swagger / Local Docker |
| **Descripción** | Qué está mal, en una oración clara |
| **Resultado esperado** | Qué debería pasar según las HU |
| **Resultado obtenido** | Qué pasó en realidad |
| **STR** | Steps to reproduce — pasos para reproducir el bug |
| **Evidencia** | Screenshot, video, o response de la API |
| **Etapa** | En qué etapa del flujo ocurre (login, alta de usuario, etc.) |
| **Prioridad** | Alta / Media / Baja |
| **Criticidad** | Bloqueante / Mayor / Menor / Cosmético |

**Ejemplo de un bug reportado:**

| Campo | Valor |
|---|---|
| BUG-ID | BUG-001 |
| Versión / Run | v1.0 / R01 |
| TC-ID | TC-002 |
| Ambiente | UI hosteada |
| Descripción | El label del campo contraseña tiene un error ortográfico |
| Resultado esperado | El label dice "Contraseña" |
| Resultado obtenido | El label dice "Contraceña" |
| STR | 1. Ir al login. 2. Observar el label del campo de contraseña. |
| Evidencia | [screenshot] |
| Etapa | Login |
| Prioridad | Baja |
| Criticidad | Cosmético |

---

## Limitaciones conocidas de esta versión

- El envío de email de recuperación de contraseña **no envía un email real** — la solicitud queda registrada en el sistema.
- Los tokens de sesión expiran a la hora. Si el login deja de funcionar de repente, volvé a iniciar sesión.
- Esta versión solo cubre login y gestión de usuarios (HU-01 a HU-09). Las funcionalidades de exámenes son del siguiente milestone.

---

## Contacto del equipo Dev

Para reportar bugs bloqueantes, pedir usuarios adicionales, o consultas técnicas:

- **Carla Gorgal** — Tech Lead
- **Mario Besednjak** — Backend
- **Martín Gueler** — Backend
