# 7test — Release v1.0 (Milestone 1)

## Fecha de entrega: 19/05/2026

## Equipo de desarrollo
- **Mario Besednjak** — Backend Lead (HU-01 a HU-09, arquitectura, seguridad)
- **Carla Gorgal** — Tech Lead (Frontend, DevOps, pruebas unitarias, PostgreSQL)
- **Martín Gueler** — Backend Support

---

## Funcionalidades incluidas (HU-01 a HU-09)

| HU | Descripción | Estado |
|----|-------------|--------|
| HU-01 | Alta de usuario por administrador | ✅ |
| HU-02 | Listado y búsqueda de usuarios | ✅ |
| HU-03 | Edición de datos de usuario | ✅ |
| HU-04 | Desactivación y reactivación de usuario | ✅ |
| HU-05 | Login unificado con email y contraseña | ✅ |
| HU-06 | Cierre de sesión (logout) | ✅ |
| HU-07 | Landing personalizada por rol | ✅ |
| HU-08 | Solicitud de recuperación de contraseña | ✅ |
| HU-09 | Configuración de política de contraseñas | ✅ |

---

## Credenciales de prueba

### Usuario administrador por defecto
- **Email:** `admin@seventest.local`
- **Contraseña:** `admin1234`

### Roles disponibles
- `ALUMNO`
- `PROFESOR`
- `DIRECTOR_DE_CATEDRA`
- `ADMINISTRADOR`

---

## Acceso a la aplicación

- **URL del backend (Swagger UI):** `https://<URL-RAILWAY>/swagger-ui/index.html`
- **URL del frontend:** `https://<URL-RAILWAY-FRONTEND>`

*(Las URLs de Railway serán comunicadas al equipo de QA una vez completado el deploy.)*

---

## Instrucciones para QA — levantar el ambiente localmente con Docker

Requisitos previos: tener **Docker Desktop** instalado y corriendo.

```bash
# 1. Clonar el repositorio
git clone https://github.com/besednjak/7test.git
cd 7test

# 2. Compilar el backend
./mvnw package -DskipTests

# 3. Levantar todo con Docker Compose
docker-compose up --build
```

- Backend disponible en: `http://localhost:8080/swagger-ui/index.html`
- PostgreSQL disponible en: `localhost:5432` (usuario: `seventest_user`, contraseña: `seventest_pass`, base: `seventest`)

---

## Limitaciones conocidas de esta versión

1. **Envío de emails simulado:** el endpoint de recuperación de contraseña (HU-08) funciona correctamente, pero el email no se envía realmente. La notificación se loguea en la consola del servidor. Esta limitación será resuelta en el Sprint 2.

2. **Tokens JWT no renovables:** los tokens expiran a la hora. No hay refresh automático. Al expirar, el usuario debe volver a loguearse.

3. **Frontend básico:** las landings de Profesor, Alumno y Director de Cátedra muestran un mensaje de bienvenida. Las funcionalidades de exámenes se implementarán en sprints futuros.

---

## Bug intencional (para el equipo de QA)

Existe un bug de seguridad deliberadamente introducido en la lógica de autenticación. El equipo de QA deberá encontrarlo y documentarlo como parte del trabajo práctico.

**Pista:** está relacionado con la validación de credenciales durante el login.

---

## Notas técnicas

- **Base de datos:** PostgreSQL 16 (producción/QA) / H2 in-memory (desarrollo local sin Docker)
- **Autenticación:** JWT propio con BCrypt. El SSO con Microsoft queda para sprints futuros.
- **Documentación de la API:** Swagger UI disponible en `/swagger-ui/index.html`
- **Pruebas unitarias:** JUnit 5 + Mockito para `AuthService`, `UserService` y `PasswordPolicyService`
