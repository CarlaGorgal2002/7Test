# 7test · Resultados de Ejecución de Tests · Run R01
**Fecha:** 20/05/2026  
**Versión:** 1.1  
**Ejecutado por:** Carla Gorgal

---

## Contexto y metodología de testing

### Estrategia declarada en Sprint 1

Según la presentación de stack tecnológico del equipo DEV (Sprint 1), las pruebas unitarias de caja blanca se realizan con las siguientes herramientas:

| Capa | Framework | Tipo de prueba |
|------|-----------|---------------|
| Backend (Java) | **JUnit 5 + Mockito** | Pruebas unitarias de caja blanca |
| Frontend (React) | **Jest + React Testing Library** | Pruebas de componentes |

La modalidad es **caja blanca**: el tester tiene acceso al código fuente y diseña los casos de prueba conociendo la lógica interna, los caminos posibles y las condiciones de borde de cada método.

---

### Dónde viven los tests

```
src/
└── test/
    └── java/
        └── com/seventest/
            ├── ApplicationTests.java                          ← context load (Spring)
            ├── application/
            │   └── service/
            │       ├── AuthServiceTest.java                   ← JUnit 5 + Mockito
            │       ├── UserServiceTest.java                   ← JUnit 5 + Mockito
            │       └── PasswordPolicyServiceTest.java         ← JUnit 5 + Mockito
            └── whitebox/
                └── KnownBugWhiteBoxChecks.java               ← JUnit 5 + Mockito + AssertJ
```

### Cómo se ejecutan

**Suite normal** (corre automáticamente en cada build):
```bash
mvnw.cmd test
```

**Suite de bugs conocidos** (se corre a propósito para evidenciar los defectos intencionales):
```bash
mvnw.cmd -Dtest=KnownBugWhiteBoxChecks test
```

> La clase `KnownBugWhiteBoxChecks` no termina en `Test` a propósito: Maven Surefire solo ejecuta automáticamente las clases que terminan en `Test`, `Tests` o `TestCase`. Esto garantiza que la suite normal siempre sea verde, mientras los bug checks quedan como documentación ejecutable separada.

### Cómo están escritos (técnica de caja blanca)

- **`@ExtendWith(MockitoExtension.class)`** — cada dependencia del servicio bajo test se reemplaza por un mock. Esto permite aislar completamente la unidad bajo prueba sin necesidad de base de datos, servidor ni red.
- **`@InjectMocks`** — el servicio real se instancia con sus mocks inyectados automáticamente.
- **`@Mock`** — se mockean `UserRepository`, `PasswordEncoder`, `JwtProvider`, `TokenBlacklist`, `AppProperties`, `EmailPort` y `PasswordPolicyRepository`.
- **AssertJ** — librería de aserciones fluidas (`assertThat`, `assertThatThrownBy`, `assertSoftly`).
- Para los tests de frontend dentro de `KnownBugWhiteBoxChecks`, se usa `Files.readString()` para leer el fuente de `LoginPage.jsx` directamente y buscar patrones textuales — esto equivale a un análisis estático de código fuente, válido como caja blanca dado que el tester conoce exactamente qué strings buscar.

---

## Suite 1 — Tests unitarios normales (`mvnw.cmd test`)

Estos tests **pasan** porque caracterizan el comportamiento real del sistema (incluyendo los bugs intencionales).

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.seventest.ApplicationTests
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Running com.seventest.application.service.AuthServiceTest
[INFO] Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Running com.seventest.application.service.PasswordPolicyServiceTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Running com.seventest.application.service.UserServiceTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 37, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## Suite 2 — Caja blanca de bugs conocidos (`mvnw.cmd -Dtest=KnownBugWhiteBoxChecks test`)

Estos tests **fallan intencionalmente** porque describen el comportamiento CORRECTO esperado, que contradice los bugs sembrados en el sistema.

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.seventest.whitebox.KnownBugWhiteBoxChecks

[ERROR] Tests run: 5, Failures: 5, Errors: 0, Skipped: 0

[ERROR] FAILURE: loginNoAdmin_conPasswordDeOtroUsuario_deberiaRechazarCredenciales
  AssertionError: HU-05: el email de A con la password de B no deberia autenticar a A
  Expected: com.seventest.domain.exception.InvalidCredentialsException to be thrown
  But was: LoginResult(token=token-a, role=ALUMNO, userFullName=Usuario A)

[ERROR] FAILURE: recuperacionPorNombre_noDeberiaRevelarEmailRegistrado
  AssertionError: HU-08: la recuperacion debe responder igual exista o no exista el usuario
  Expected: not equal to "a@test.com"
  But was: "a@test.com"

[ERROR] FAILURE: loginPage_noDeberiaContenerErrorOrtograficoEnPassword
  AssertionError: Bug cosmetico solicitado: aparece 'Contrace...' en el label
  Expected: false (no debe aparecer el typo)
  But was: true (la cadena 'Contrace' aparece en LoginPage.jsx)

[ERROR] FAILURE: loginPage_deberiaMantenerTextosVisiblesEnEspanol
  Multiple failures (3):
  - Boton principal: 'Login' encontrado en LoginPage.jsx (debería estar en español)
  - Estado de carga: 'Loading...' encontrado en LoginPage.jsx (debería estar en español)
  - Fallback de error: 'session' encontrado en LoginPage.jsx (debería estar en español)

[ERROR] FAILURE: loginPage_modoOscuro_deberiaUsarColorDeTextoContrastante
  Multiple failures (2):
  - El color del texto no debe ser igual al fondo oscuro:
    'color: #122430 !important' encontrado en LoginPage.jsx
  - El color de autofill tampoco debe mimetizarse con el fondo:
    '-webkit-text-fill-color: #122430 !important' encontrado en LoginPage.jsx

[INFO]
[INFO] BUILD FAILURE
```

---

## Tabla de Resultados — Formato RUN (según estándar PMs)

| RUN | TC-ID | Caso | RE (Resultado Esperado) | RO (Resultado Obtenido) | Pass/Fail |
|-----|-------|------|------------------------|------------------------|-----------|
| R01 | TC-001 | Login exitoso — Admin con su propia contraseña | Ingreso al sistema como ADMINISTRADOR | Ingreso al sistema como ADMINISTRADOR | **PASS** |
| R01 | TC-002 | Login exitoso — Alumno con su propia contraseña | Ingreso al sistema como ALUMNO | Ingreso al sistema como ALUMNO | **PASS** |
| R01 | TC-003 | Login exitoso — Profesor con su propia contraseña | Ingreso al sistema como PROFESOR | Ingreso al sistema como PROFESOR | **PASS** |
| R01 | TC-004 | Login fallido — Email inexistente | Mensaje: "Usuario o contraseña incorrectos" | Mensaje: "Usuario o contraseña incorrectos" | **PASS** |
| R01 | TC-005 | Login fallido — Usuario inactivo | Mensaje: cuenta inactiva | Mensaje: cuenta inactiva | **PASS** |
| R01 | TC-006 | Login fallido — Cuenta bloqueada por intentos | Excepción AccountLockedException | Excepción AccountLockedException | **PASS** |
| R01 | TC-007 | Login fallido — Contraseña incorrecta incrementa contador | failedLoginAttempts sube a 1 | failedLoginAttempts sube a 1 | **PASS** |
| R01 | TC-008 | Bloqueo de cuenta al 5to intento fallido | lockedUntil != null, intentos = 5 | lockedUntil != null, intentos = 5 | **PASS** |
| R01 | TC-009 | Login exitoso resetea contador e bloqueo expirado | failedAttempts = 0, lockedUntil = null | failedAttempts = 0, lockedUntil = null | **PASS** |
| R01 | TC-010 | Logout con token válido invalida el token | Token agregado al blacklist | Token agregado al blacklist | **PASS** |
| R01 | TC-011 | Logout con token inválido no afecta blacklist | Sin interacción con blacklist | Sin interacción con blacklist | **PASS** |
| R01 | TC-012 | Recuperación contraseña — email existente | Email port notificado | Email port notificado | **PASS** |
| R01 | TC-013 | Recuperación contraseña — email inexistente | Sin excepción, sin notificación | Sin excepción, sin notificación | **PASS** |
| R01 | TC-014 | Alta usuario con datos válidos | Usuario activo, password hasheada, failedAttempts=0 | Usuario activo, password hasheada, failedAttempts=0 | **PASS** |
| R01 | TC-015 | Alta usuario — email duplicado | EmailAlreadyExistsException | EmailAlreadyExistsException | **PASS** |
| R01 | TC-016 | Alta usuario — password demasiado corta | PasswordPolicyViolationException con longitud mínima | PasswordPolicyViolationException con longitud mínima | **PASS** |
| R01 | TC-017 | Alta usuario — password demasiado larga | PasswordPolicyViolationException con longitud máxima | PasswordPolicyViolationException con longitud máxima | **PASS** |
| R01 | TC-018 | Alta usuario — sin mayúscula cuando es requerida | PasswordPolicyViolationException | PasswordPolicyViolationException | **PASS** |
| R01 | TC-019 | Alta usuario — sin número cuando es requerido | PasswordPolicyViolationException | PasswordPolicyViolationException | **PASS** |
| R01 | TC-020 | Edición usuario — sin cambio de contraseña | Hash anterior preservado | Hash anterior preservado | **PASS** |
| R01 | TC-021 | Edición usuario — email duplicado | EmailAlreadyExistsException | EmailAlreadyExistsException | **PASS** |
| R01 | TC-022 | Edición usuario — nueva contraseña la hashea | Nuevo hash generado | Nuevo hash generado | **PASS** |
| R01 | TC-023 | Edición usuario — contraseña en blanco no cambia hash | Hash anterior preservado | Hash anterior preservado | **PASS** |
| R01 | TC-024 | Desactivar usuario existente | Estado = INACTIVO | Estado = INACTIVO | **PASS** |
| R01 | TC-025 | Reactivar usuario existente | Estado = ACTIVO | Estado = ACTIVO | **PASS** |
| R01 | TC-026 | Política de contraseñas — retorna la almacenada | Política con minLength=8 | Política con minLength=8 | **PASS** |
| R01 | TC-027 | Política de contraseñas — sin política usa default | minLength = DEFAULT | minLength = DEFAULT | **PASS** |
| R01 | TC-028 | Actualizar política válida (min < max) | Política guardada | Política guardada | **PASS** |
| R01 | TC-029 | Actualizar política inválida (min > max) | IllegalArgumentException con "mínima" | IllegalArgumentException con "mínima" | **PASS** |
| R01 | TC-030 | Actualizar política con min == max | Política guardada (caso borde válido) | Política guardada (caso borde válido) | **PASS** |

---

## Tabla de Bugs — Detectados por Caja Blanca

| BUG-ID | Versión / Run | TC-ID | Ambiente | Descripción | RE | RO | STR | Evidencia | Etapa | Prioridad | Criticidad |
|--------|--------------|-------|----------|-------------|----|----|-----|-----------|-------|-----------|------------|
| BUG-01 | 1.1 / R01 | TC-031 | Backend (AuthService) | Usuario no-admin puede autenticarse con la contraseña de cualquier otro usuario activo, sin que sea su propia contraseña | Login debe fallar con `InvalidCredentialsException` | Login exitoso con email de Usuario A y contraseña de Usuario B | 1. Crear usuario A y usuario B activos. 2. Intentar login con email de A y contraseña de B. 3. El sistema devuelve token válido. | `KnownBugWhiteBoxChecks.loginNoAdmin_conPasswordDeOtroUsuario_deberiaRechazarCredenciales` → AssertionError | Autenticación (HU-05) | Alta | Alta |
| BUG-02 | 1.1 / R01 | TC-032 | Backend (AuthService) | El endpoint `recoverByName` devuelve el email real del usuario cuando el nombre existe, violando la privacidad del sistema | La respuesta no debe revelar si el email existe ni cuál es | El email `a@test.com` es retornado directamente en la respuesta | 1. Llamar a `recoverByName("Usuario A")`. 2. Verificar que la respuesta no contiene el email del usuario. 3. El sistema lo retorna en claro. | `KnownBugWhiteBoxChecks.recuperacionPorNombre_noDeberiaRevelarEmailRegistrado` → AssertionError | Recuperación de contraseña (HU-08) | Alta | Alta |
| BUG-03 | 1.1 / R01 | TC-033 | Frontend (LoginPage.jsx) | Error ortográfico: aparece el string `'Contrace'` en la etiqueta del campo de contraseña (en lugar de `'Contraseña'`) | Todos los textos deben estar en español correcto | Label muestra "Contrace..." | 1. Abrir login. 2. Verificar etiqueta del campo contraseña. 3. Muestra texto truncado/erróneo. | `KnownBugWhiteBoxChecks.loginPage_noDeberiaContenerErrorOrtograficoEnPassword` → `true` esperaba `false` | UI / Login (HU-05) | Media | Baja |
| BUG-04 | 1.1 / R01 | TC-034 | Frontend (LoginPage.jsx) | Mezcla de idiomas: strings en inglés presentes en la interfaz (`'Login'`, `'Loading...'`, `'session'`) | Todos los textos visibles deben estar en español | Strings en inglés detectados en código fuente | 1. Abrir login. 2. Hacer clic en "ingresar". 3. Observar botón en estado de carga: dice "Loading..." en lugar de "Cargando...". | `KnownBugWhiteBoxChecks.loginPage_deberiaMantenerTextosVisiblesEnEspanol` → 3 fallos de SoftAssertions | UI / Login (HU-05) | Media | Baja |
| BUG-05 | 1.1 / R01 | TC-035 | Frontend (LoginPage.jsx) — modo oscuro | En modo oscuro, el campo de contraseña usa `color: #122430` que es igual al color de fondo, haciendo el texto invisible | El texto debe ser legible (contraste suficiente) | El texto del campo se mimetiza con el fondo oscuro — campo ilegible | 1. Activar modo oscuro en el sistema operativo. 2. Abrir login. 3. Tipear en el campo contraseña. 4. El texto escrito no es visible. | `KnownBugWhiteBoxChecks.loginPage_modoOscuro_deberiaUsarColorDeTextoContrastante` → 2 fallos de SoftAssertions | UI / Accesibilidad (HU-05) | Alta | Media |

---

## Resumen Ejecutivo

| Categoría | Cantidad |
|-----------|---------|
| Tests ejecutados (suite normal) | 37 |
| Tests PASS | 37 |
| Tests FAIL (suite normal) | 0 |
| | |
| Tests de caja blanca de bugs | 5 |
| Tests PASS (caja blanca) | 0 |
| Tests FAIL intencionalmente | 5 |
| | |
| **Bugs documentados** | **5** |
| Bugs de autenticación (backend) | 2 |
| Bugs de UI / frontend | 3 |

### Notas
- Los 5 bugs documentados en esta tabla son **intencionales** (sembrados por el equipo DEV para que los QA los encuentren).
- Los tests de la suite normal pasan porque caracterizan el comportamiento actual del sistema, no el esperado.
- Los tests de `KnownBugWhiteBoxChecks` documentan el comportamiento correcto esperado — fallan porque el bug existe.
- La suite normal se corre con `mvnw.cmd test`. Los bug checks se corren con `mvnw.cmd -Dtest=KnownBugWhiteBoxChecks test`.
