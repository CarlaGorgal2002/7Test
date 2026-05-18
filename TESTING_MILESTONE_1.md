# Guía de pruebas manuales — Milestone 1

Pruebas funcionales paso a paso para todas las historias de usuario del Milestone 1.

## Prerequisito

La aplicación debe estar corriendo:

```bash
./mvnw spring-boot:run
```

Todas las pruebas usan `curl`. Se puede usar también Postman, Insomnia o cualquier cliente HTTP.

---

## Variables de entorno para la sesión

Conviene exportar el token en una variable para no repetirlo en cada comando:

```bash
export TOKEN=""  # se completa en HU-05
```

---

## HU-01 — Alta de usuario

### Obtener token de admin primero

```bash
export TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@seventest.local","password":"admin1234"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")
```

### ✅ Alta exitosa

```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fullName": "María García",
    "email": "maria@uade.edu.ar",
    "role": "ALUMNO",
    "initialPassword": "1234"
  }'
```

**Resultado esperado:** `201 Created` con el usuario creado, `status: ACTIVO`.

---

### ❌ Email duplicado

```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fullName": "Otro Usuario",
    "email": "maria@uade.edu.ar",
    "role": "ALUMNO",
    "initialPassword": "1234"
  }'
```

**Resultado esperado:** `409 Conflict` — `"El email ya está registrado en la plataforma: maria@uade.edu.ar"`

---

### ❌ Email con formato inválido

```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fullName": "Carlos",
    "email": "no-es-un-email",
    "role": "ALUMNO",
    "initialPassword": "1234"
  }'
```

**Resultado esperado:** `400 Bad Request` — mensaje de validación de formato.

---

### ❌ Contraseña que viola la política (muy corta)

```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fullName": "Test",
    "email": "test@uade.edu.ar",
    "role": "ALUMNO",
    "initialPassword": "ab"
  }'
```

**Resultado esperado:** `400 Bad Request` — `"La contraseña debe tener al menos 4 caracteres"`

---

### ❌ Sin token de autorización

```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "fullName": "Test",
    "email": "test2@uade.edu.ar",
    "role": "ALUMNO",
    "initialPassword": "1234"
  }'
```

**Resultado esperado:** `403 Forbidden`

---

## HU-02 — Listado y búsqueda de usuarios

Primero crear algunos usuarios de prueba:

```bash
for data in \
  '{"fullName":"Bruno Martínez","email":"bruno@uade.edu.ar","role":"PROFESOR","initialPassword":"1234"}' \
  '{"fullName":"Carla Gómez","email":"carla@uade.edu.ar","role":"ALUMNO","initialPassword":"1234"}' \
  '{"fullName":"Diego Fernández","email":"diego@uade.edu.ar","role":"PROFESOR","initialPassword":"1234"}'; do
  curl -s -o /dev/null -X POST http://localhost:8080/api/users \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "$data"
done
```

### ✅ Listado completo paginado

```bash
curl -s "http://localhost:8080/api/users" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** `200 OK` con `content` (lista ordenada por nombre), `totalElements`, `totalPages`, `page`.

---

### ✅ Búsqueda por nombre

```bash
curl -s "http://localhost:8080/api/users?search=bruno" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** solo usuarios cuyo nombre o email contenga "bruno" (case-insensitive).

---

### ✅ Búsqueda por email parcial

```bash
curl -s "http://localhost:8080/api/users?search=uade" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** todos los usuarios con dominio `@uade.edu.ar`.

---

### ✅ Filtro por rol

```bash
curl -s "http://localhost:8080/api/users?role=PROFESOR" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** solo usuarios con `role: PROFESOR`.

---

### ✅ Filtro por estado

```bash
curl -s "http://localhost:8080/api/users?status=ACTIVO" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** solo usuarios con `status: ACTIVO`.

---

### ✅ Paginado con tamaño reducido

```bash
curl -s "http://localhost:8080/api/users?page=0&size=2" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** máximo 2 usuarios en `content`, `totalPages` mayor a 1 si hay más de 2 usuarios.

---

### ✅ Combinación de filtros

```bash
curl -s "http://localhost:8080/api/users?role=ALUMNO&status=ACTIVO&search=garcia" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** alumnos activos cuyo nombre o email contenga "garcia".

---

## HU-03 — Edición de usuario

Guardar el ID de un usuario creado en HU-01:

```bash
export USER_ID=$(curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"fullName":"Ana López","email":"ana@uade.edu.ar","role":"ALUMNO","initialPassword":"1234"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['id'])")
```

### ✅ Editar nombre y rol

```bash
curl -s -X PUT "http://localhost:8080/api/users/$USER_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fullName": "Ana López Modificada",
    "email": "ana@uade.edu.ar",
    "role": "PROFESOR",
    "newPassword": null
  }'
```

**Resultado esperado:** `200 OK` con los datos actualizados.

---

### ✅ Reset de contraseña

```bash
curl -s -X PUT "http://localhost:8080/api/users/$USER_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fullName": "Ana López Modificada",
    "email": "ana@uade.edu.ar",
    "role": "PROFESOR",
    "newPassword": "nuevapass99"
  }'
```

Verificar que puede loguearse con la nueva contraseña:

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@uade.edu.ar","password":"nuevapass99"}'
```

**Resultado esperado:** `200 OK` con token JWT.

---

### ❌ Cambiar email a uno ya usado por otro usuario

```bash
curl -s -X PUT "http://localhost:8080/api/users/$USER_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fullName": "Ana",
    "email": "bruno@uade.edu.ar",
    "role": "ALUMNO",
    "newPassword": null
  }'
```

**Resultado esperado:** `409 Conflict`

---

### ❌ ID inexistente

```bash
curl -s -X PUT "http://localhost:8080/api/users/00000000-0000-0000-0000-000000000000" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fullName": "Nadie",
    "email": "nadie@uade.edu.ar",
    "role": "ALUMNO",
    "newPassword": null
  }'
```

**Resultado esperado:** `404 Not Found`

---

## HU-04 — Desactivación y reactivación de usuario

### ✅ Desactivar usuario

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X PATCH "http://localhost:8080/api/users/$USER_ID/deactivate" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** `204 No Content`

---

### ✅ Verificar que no puede iniciar sesión

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@uade.edu.ar","password":"nuevapass99"}'
```

**Resultado esperado:** `403 Forbidden` — `"La cuenta se encuentra inactiva"`

---

### ✅ Verificar que el usuario persiste (no se elimina)

```bash
curl -s "http://localhost:8080/api/users/$USER_ID" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** `200 OK` con `status: INACTIVO` — el usuario sigue existiendo.

---

### ✅ Reactivar usuario

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X PATCH "http://localhost:8080/api/users/$USER_ID/reactivate" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** `204 No Content`

---

### ✅ Verificar que puede volver a iniciar sesión

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@uade.edu.ar","password":"nuevapass99"}'
```

**Resultado esperado:** `200 OK` con token JWT.

---

## HU-05 — Inicio de sesión unificado

### ✅ Login exitoso — respuesta contiene token, rol y nombre

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@seventest.local","password":"admin1234"}'
```

**Resultado esperado:** `200 OK` con `token`, `role: ADMINISTRADOR`, `fullName: Administrador`.

---

### ❌ Contraseña incorrecta (mensaje genérico)

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@seventest.local","password":"incorrecta"}'
```

**Resultado esperado:** `401 Unauthorized` — `"Credenciales incorrectas"` (no revela qué campo falló).

---

### ❌ Email inexistente (mismo mensaje genérico)

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"fantasma@uade.edu.ar","password":"algo"}'
```

**Resultado esperado:** `401 Unauthorized` — `"Credenciales incorrectas"` (idéntico al caso anterior, no revela si el email existe).

---

### ❌ Bloqueo por intentos fallidos

Ejecutar 5 veces seguidas con contraseña incorrecta:

```bash
for i in 1 2 3 4 5; do
  echo "Intento $i:"
  curl -s -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"ana@uade.edu.ar","password":"mal"}'
  echo ""
done
```

**Resultado esperado:** a partir del intento 5 (configurable, default 5), el siguiente intento devuelve `423 Locked` — `"La cuenta está bloqueada temporalmente por exceso de intentos fallidos"`.

Verificar que la contraseña correcta también es bloqueada mientras dure el lock:

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"ana@uade.edu.ar","password":"nuevapass99"}'
```

**Resultado esperado:** `423 Locked`

---

## HU-06 — Cierre de sesión

### ✅ Logout exitoso

```bash
# Obtener un token fresco
export TOKEN_TEMP=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@seventest.local","password":"admin1234"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['token'])")

# Logout
curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8080/api/auth/logout \
  -H "Authorization: Bearer $TOKEN_TEMP"
```

**Resultado esperado:** `204 No Content`

---

### ✅ Token invalidado tras logout

```bash
curl -s -o /dev/null -w "%{http_code}" \
  "http://localhost:8080/api/users" \
  -H "Authorization: Bearer $TOKEN_TEMP"
```

**Resultado esperado:** `403 Forbidden` — el token ya no es válido.

---

## HU-07 — Landing personalizada por rol

El backend indica el rol en la respuesta del login para que el frontend realice la redirección.

### ✅ Login como ADMINISTRADOR

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@seventest.local","password":"admin1234"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('role:', d['role'])"
```

**Resultado esperado:** `role: ADMINISTRADOR`

---

### ✅ Login como ALUMNO

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@uade.edu.ar","password":"1234"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('role:', d['role'])"
```

**Resultado esperado:** `role: ALUMNO`

---

### ✅ Login como PROFESOR

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"bruno@uade.edu.ar","password":"1234"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print('role:', d['role'])"
```

**Resultado esperado:** `role: PROFESOR`

---

## HU-08 — Recuperación de contraseña

### ✅ Solicitud con email registrado

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8080/api/auth/password-recovery \
  -H "Content-Type: application/json" \
  -d '{"email":"maria@uade.edu.ar"}'
```

**Resultado esperado:** `202 Accepted`. Verificar en los logs de la aplicación que aparece la línea:

```
[EMAIL] Recuperación de contraseña solicitada para: María García <maria@uade.edu.ar>
```

---

### ✅ Solicitud con email NO registrado (misma respuesta)

```bash
curl -s -o /dev/null -w "%{http_code}" \
  -X POST http://localhost:8080/api/auth/password-recovery \
  -H "Content-Type: application/json" \
  -d '{"email":"noexiste@uade.edu.ar"}'
```

**Resultado esperado:** `202 Accepted` — igual que el caso anterior. El sistema no revela si el email existe.

---

## HU-09 — Configuración de política de contraseñas

### ✅ Obtener política actual (defaults)

```bash
curl -s "http://localhost:8080/api/config/password-policy" \
  -H "Authorization: Bearer $TOKEN"
```

**Resultado esperado:** `200 OK` con `minLength: 4`, `maxLength: 100`, todos los `require*: false`.

---

### ✅ Actualizar política

```bash
curl -s -X PUT "http://localhost:8080/api/config/password-policy" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "minLength": 8,
    "maxLength": 50,
    "requireUppercase": true,
    "requireLowercase": true,
    "requireNumbers": true,
    "requireSpecialChars": false
  }'
```

**Resultado esperado:** `200 OK` con la política actualizada.

---

### ✅ Política se aplica inmediatamente — contraseña válida

```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fullName": "Usuario Valido",
    "email": "valido@uade.edu.ar",
    "role": "ALUMNO",
    "initialPassword": "Valida123"
  }'
```

**Resultado esperado:** `201 Created`

---

### ❌ Política se aplica — contraseña sin mayúscula

```bash
curl -s -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "fullName": "Usuario Invalido",
    "email": "invalido@uade.edu.ar",
    "role": "ALUMNO",
    "initialPassword": "sinmayuscula1"
  }'
```

**Resultado esperado:** `400 Bad Request` — `"La contraseña debe contener al menos una mayúscula"`

---

### ❌ Política incoherente: mínimo mayor que máximo

```bash
curl -s -X PUT "http://localhost:8080/api/config/password-policy" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "minLength": 20,
    "maxLength": 5,
    "requireUppercase": false,
    "requireLowercase": false,
    "requireNumbers": false,
    "requireSpecialChars": false
  }'
```

**Resultado esperado:** `400 Bad Request` — `"La longitud mínima no puede ser mayor que la máxima"`

---

### ❌ Acceso sin token de administrador

```bash
curl -s -o /dev/null -w "%{http_code}" \
  "http://localhost:8080/api/config/password-policy"
```

**Resultado esperado:** `403 Forbidden`

---

## Resumen de códigos HTTP esperados

| Situación | Código |
|---|---|
| Operación exitosa (creación) | `201 Created` |
| Operación exitosa (lectura/actualización) | `200 OK` |
| Operación exitosa (sin contenido) | `204 No Content` |
| Solicitud aceptada (async) | `202 Accepted` |
| Datos inválidos o violación de política | `400 Bad Request` |
| Sin token o token inválido | `403 Forbidden` |
| Credenciales incorrectas | `401 Unauthorized` |
| Cuenta inactiva | `403 Forbidden` |
| Cuenta bloqueada | `423 Locked` |
| Recurso no encontrado | `404 Not Found` |
| Email ya en uso | `409 Conflict` |
