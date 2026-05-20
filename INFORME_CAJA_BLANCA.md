# Informe de pruebas de caja blanca - 7test

Fecha de preparacion: 20/05/2026

## Objetivo

Validar por caja blanca los servicios principales definidos para el milestone: autenticacion, gestion de usuarios y politica de contrasenas. La estrategia usa JUnit 5 + Mockito para aislar dependencias y cubrir ramas internas del codigo sin depender de la UI ni de la base de datos real.

Tambien se prepararon checks separados para evidenciar los bugs sembrados a proposito. Esos checks describen el comportamiento correcto esperado y fallan porque el codigo productivo mantiene los defectos intencionales para la actividad de testing.

## Archivos creados o modificados

- `src/test/java/com/seventest/application/service/AuthServiceTest.java`
- `src/test/java/com/seventest/application/service/UserServiceTest.java`
- `src/test/java/com/seventest/application/service/PasswordPolicyServiceTest.java`
- `src/test/java/com/seventest/whitebox/KnownBugWhiteBoxChecks.java`
- `PRUEBAS_CAJA_BLANCA.md`
- `INFORME_CAJA_BLANCA.md`

## Ejecucion de la suite normal

Comando usado:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\jbr'
.\mvnw.cmd test
```

Resultado:

| Test set | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `AuthServiceTest` | 15 | 0 | 0 | 0 |
| `UserServiceTest` | 17 | 0 | 0 | 0 |
| `PasswordPolicyServiceTest` | 5 | 0 | 0 | 0 |
| `ApplicationTests` | 1 | 0 | 0 | 0 |
| **Total** | **38** | **0** | **0** | **0** |

Conclusion: la suite unitaria normal queda verde.

## Cobertura logica realizada

### AuthService

- Login correcto de usuario no administrador.
- Login correcto de administrador por su rama especial.
- Rechazo de administrador con password incorrecta.
- Rechazo de email inexistente.
- Rechazo de usuario inactivo.
- Rechazo de cuenta bloqueada.
- Incremento de intentos fallidos.
- Bloqueo al llegar al maximo de intentos.
- Reset de intentos fallidos tras login exitoso.
- Logout con token valido.
- Logout con token invalido.
- Recuperacion por email existente.
- Recuperacion por email inexistente.
- Caracterizacion del bug de password cruzada.
- Caracterizacion del bug de recuperacion por nombre.

### UserService

- Creacion valida de usuario.
- Rechazo de email duplicado.
- Uso de politica default.
- Validacion de longitud minima y maxima.
- Validacion de mayuscula, minuscula, numero y caracter especial.
- Edicion con cambio de email.
- Edicion sin cambio de password.
- Edicion con nueva password hasheada.
- Edicion con password en blanco.
- Rechazo de email duplicado al editar.
- Rechazo de id inexistente.
- Desactivacion y reactivacion.
- Busqueda por id.
- Delegacion de filtros y paginado.

### PasswordPolicyService

- Retorno de politica persistida.
- Retorno de politica default.
- Actualizacion valida.
- Rechazo de minimo mayor que maximo.
- Aceptacion de minimo igual a maximo.

## Ejecucion de bugs conocidos

Comando usado:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\jbr'
.\mvnw.cmd -Dtest=KnownBugWhiteBoxChecks test
```

Resultado esperado y obtenido: falla intencionalmente con 5 tests fallidos.

| TC-ID | Bug evidenciado | Resultado esperado | Resultado obtenido |
|---|---|---|---|
| `loginNoAdmin_conPasswordDeOtroUsuario_deberiaRechazarCredenciales` | Usuario A + password de B | Debe lanzar `InvalidCredentialsException` | Autentica y genera token para A |
| `recuperacionPorNombre_noDeberiaRevelarEmailRegistrado` | Recuperacion revela email | No debe revelar si el usuario existe | Devuelve `a@test.com` |
| `loginPage_noDeberiaContenerErrorOrtograficoEnPassword` | Error ortografico | No debe aparecer `Contrace...` | El label contiene `Contrace...` |
| `loginPage_deberiaMantenerTextosVisiblesEnEspanol` | Mezcla idioma espanol/ingles | Textos visibles en espanol | Aparecen `Login`, `Loading...` y `session` |
| `loginPage_modoOscuro_deberiaUsarColorDeTextoContrastante` | Modo oscuro sin contraste | Texto del input debe contrastar | Texto y fondo usan `#122430` |

## Explicacion tecnica de los defectos

### Login con password cruzada

Archivo: `src/main/java/com/seventest/application/service/AuthService.java`

La rama no administradora valida la password contra todos los usuarios activos:

```java
validPassword = userRepository.findAll(null, null, UserStatus.ACTIVO, 0, 1000)
        .content().stream()
        .anyMatch(u -> passwordEncoder.matches(password, u.getPasswordHash()));
```

Por eso alcanza con usar una password valida de cualquier usuario activo para entrar a la cuenta del email elegido.

### Recuperacion por nombre

Archivos:

- `src/main/java/com/seventest/application/service/AuthService.java`
- `src/main/java/com/seventest/infrastructure/web/controller/AuthController.java`

El metodo `recoverByName` busca por nombre completo y retorna el email real del usuario encontrado. Esto contradice el criterio de HU-08, porque la respuesta deberia ser indistinguible exista o no exista el usuario.

### Bugs visuales y de texto

Archivo: `frontend/src/pages/LoginPage.jsx`

Los checks inspeccionan directamente el componente React y detectan:

- Label de password con `Contrace...`.
- Textos visibles en ingles.
- CSS de modo oscuro con `background-color: #122430`, `color: #122430` y `-webkit-text-fill-color: #122430`.

## Nota para defensa

No se corrige el codigo productivo porque esos defectos fueron sembrados para que otros grupos los encuentren por caja negra. En caja blanca se dejan documentados y reproducibles mediante una clase aparte que no corre en la suite normal.

