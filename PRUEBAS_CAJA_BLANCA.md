# Pruebas de caja blanca - 7test

Este documento explica como defender y ejecutar las pruebas de caja blanca del milestone. La idea es separar dos cosas:

- **Suite normal:** pruebas unitarias de servicios con JUnit 5 + Mockito. Debe quedar verde para demostrar que los tests estan bien escritos y son repetibles.
- **Suite de bugs conocidos:** checks que expresan el comportamiento correcto esperado. Se ejecutan aparte y fallan a proposito porque la app tiene defectos sembrados para la materia.

## Contexto usado

De los documentos del sprint y release:

- El alcance de esta entrega esta centrado en login, logout, recuperacion de contrasena, gestion de usuarios y politica de contrasenas.
- La release declara pruebas unitarias con **JUnit 5 + Mockito** para `AuthService`, `UserService` y `PasswordPolicyService`.
- HU-05 pide login unificado con errores genericos y sin revelar informacion sensible.
- HU-08 pide que la recuperacion no revele si el usuario existe.
- Los PMs piden reportar defectos con: `BUG-ID`, version/run, `TC-ID`, ambiente, descripcion, resultado esperado, resultado obtenido, STR, evidencia, etapa, prioridad y criticidad.

## Donde estan los tests

- `src/test/java/com/seventest/application/service/AuthServiceTest.java`
- `src/test/java/com/seventest/application/service/UserServiceTest.java`
- `src/test/java/com/seventest/application/service/PasswordPolicyServiceTest.java`
- `src/test/java/com/seventest/whitebox/KnownBugWhiteBoxChecks.java`

`KnownBugWhiteBoxChecks` no termina en `Test`, entonces Maven no la corre en la suite normal. Esto permite tener una suite verde y, al mismo tiempo, una suite que muestra los bugs intencionales cuando se la ejecuta explicitamente.

## Como ejecutar

Si tenes JDK 21 instalado y `JAVA_HOME` configurado:

```powershell
.\mvnw.cmd test
```

En esta maquina tambien funciona usando el JDK incluido con IntelliJ:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\jbr'
.\mvnw.cmd test
```

Para ejecutar solo las pruebas que evidencian los bugs conocidos:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\jbr'
.\mvnw.cmd -Dtest=KnownBugWhiteBoxChecks test
```

Ese ultimo comando debe fallar. Esa falla es la evidencia blanca de que el codigo ejecuta ramas incorrectas o expone datos que no deberia.

## Que cubre la suite normal

### AuthService

- Login exitoso de usuario no administrador.
- Login de admin por la rama especial que compara solo contra su propio hash.
- Login de admin con password incorrecta.
- Email inexistente con `InvalidCredentialsException`.
- Usuario inactivo con `UserInactiveException`.
- Cuenta bloqueada con `AccountLockedException`.
- Incremento de intentos fallidos.
- Bloqueo al llegar al limite de intentos.
- Reset de intentos tras login exitoso.
- Logout con token valido e invalido.
- Recuperacion por email existente e inexistente.
- Caracterizacion del bug: usuario A + password de B autentica a A.
- Caracterizacion del bug: recuperacion por nombre devuelve email.

### UserService

- Alta de usuario valida.
- Email duplicado.
- Uso de politica default si no hay politica persistida.
- Validaciones de longitud minima y maxima.
- Validaciones de mayuscula, minuscula, numero y caracter especial.
- Edicion con y sin cambio de password.
- Edicion con email duplicado.
- Usuario inexistente.
- Desactivar/reactivar.
- `findById`.
- Delegacion de filtros y paginado a repositorio.

### PasswordPolicyService

- `get()` retorna politica persistida.
- `get()` retorna default si no hay politica.
- `update()` guarda politica valida.
- `update()` rechaza minimo mayor que maximo.
- `update()` acepta minimo igual a maximo.

## Bugs conocidos y como explicarlos

### WB-BUG-01 - Login cruza email de A con password de B

Codigo observado: `AuthService.login()`.

Rama defectuosa:

```java
validPassword = userRepository.findAll(null, null, UserStatus.ACTIVO, 0, 1000)
        .content().stream()
        .anyMatch(u -> passwordEncoder.matches(password, u.getPasswordHash()));
```

Esperado: comparar la password solo contra `user.getPasswordHash()`.

Obtenido: para usuarios no admin, si la password coincide con cualquier usuario activo, se genera token para el email solicitado.

Evidencia: `KnownBugWhiteBoxChecks.loginNoAdmin_conPasswordDeOtroUsuario_deberiaRechazarCredenciales`.

### WB-BUG-02 - Recuperacion por nombre revela email

Codigo observado: `AuthService.recoverByName()` y `AuthController.recoverByName()`.

Esperado: la recuperacion debe responder de forma indistinguible, exista o no el usuario.

Obtenido: si el nombre existe, retorna el email real.

Evidencia: `KnownBugWhiteBoxChecks.recuperacionPorNombre_noDeberiaRevelarEmailRegistrado`.

### WB-BUG-03 - Error ortografico

Codigo observado: `frontend/src/pages/LoginPage.jsx`.

Esperado: label `Contraseña`.

Obtenido: aparece una variante incorrecta con `Contrace...`.

Evidencia: `KnownBugWhiteBoxChecks.loginPage_noDeberiaContenerErrorOrtograficoEnPassword`.

### WB-BUG-04 - Mezcla de espanol e ingles

Codigo observado: `frontend/src/pages/LoginPage.jsx`.

Esperado: textos visibles consistentes en espanol.

Obtenido: boton/estado usan `Login`, `Loading...` y un fallback con `session`.

Evidencia: `KnownBugWhiteBoxChecks.loginPage_deberiaMantenerTextosVisiblesEnEspanol`.

### WB-BUG-05 - Modo oscuro deja texto invisible

Codigo observado: estilos `.dm-input` en `frontend/src/pages/LoginPage.jsx`.

Esperado: texto con contraste frente al fondo.

Obtenido: el input oscuro usa fondo `#122430` y texto `#122430`, por eso lo escrito se mimetiza.

Evidencia: `KnownBugWhiteBoxChecks.loginPage_modoOscuro_deberiaUsarColorDeTextoContrastante`.

## Como presentarlo en clase

1. Mostrar primero `.\mvnw.cmd test`: prueba que la suite unitaria base corre y que los tests estan aislados con mocks.
2. Mostrar despues `.\mvnw.cmd -Dtest=KnownBugWhiteBoxChecks test`: prueba que los defectos intencionales aparecen cuando se valida el comportamiento esperado.
3. Para cada fallo, reportar con el formato de PMs: resultado esperado vs resultado obtenido, y usar el nombre del test como `TC-ID`.
4. Aclarar que no se corrige el codigo productivo porque esos defectos fueron sembrados para que los equipos QA los descubran por caja negra.

