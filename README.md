# 7test — Sistema de Gestión de Evaluaciones Universitarias

API REST desarrollada con Spring Boot como parte del proyecto de la materia **Testing de Aplicaciones** (UADE).  
Arquitectura hexagonal, base de datos H2 en memoria, autenticación JWT.

---

## Requisitos

| Herramienta | Versión mínima |
|---|---|
| Java (JDK) | 21 |
| Maven | 3.9+ — o usar el wrapper incluido (`./mvnw`) |

No se requiere instalar ninguna base de datos. La aplicación utiliza H2 en memoria que se inicializa automáticamente al arrancar.

---

## Iniciar la aplicación

```bash
./mvnw spring-boot:run
```

La aplicación levanta en `http://localhost:8080`.

> En Windows usar `mvnw.cmd spring-boot:run`

---

## Usuario administrador inicial

Al arrancar por primera vez se crea automáticamente un usuario administrador:

| Campo | Valor |
|---|---|
| Email | `admin@seventest.local` |
| Contraseña | `admin1234` |
| Rol | `ADMINISTRADOR` |

---

## Consola H2

Durante el desarrollo se puede inspeccionar la base de datos en:

```
http://localhost:8080/h2-console
```

| Campo | Valor |
|---|---|
| JDBC URL | `jdbc:h2:mem:seventest` |
| Usuario | `sa` |
| Contraseña | _(vacía)_ |

---

## Documentación de la API

Con la aplicación corriendo, la documentación interactiva está disponible en:

```
http://localhost:8080/swagger-ui/index.html
```

Desde Swagger UI se pueden explorar todos los endpoints, ver los esquemas de request/response y ejecutar llamadas directamente en el navegador.

**Para probar endpoints protegidos:**
1. Ejecutar `POST /api/auth/login` con las credenciales del admin inicial
2. Copiar el `token` de la respuesta
3. Hacer clic en el botón **Authorize** (arriba a la derecha)
4. Ingresar el token con el formato: `Bearer <token>`

La especificación en formato JSON también está disponible en:

```
http://localhost:8080/v3/api-docs
```

---

## Variables de entorno (opcionales)

| Variable | Descripción | Default |
|---|---|---|
| `JWT_SECRET` | Clave secreta para firmar tokens JWT | valor de desarrollo |
| `app.jwt.expiration-ms` | Duración del token en ms | `3600000` (1 hora) |
| `app.security.max-login-attempts` | Intentos antes de bloquear cuenta | `5` |
| `app.security.lockout-duration-minutes` | Minutos de bloqueo | `15` |
| `OPENAI_ENABLED` | Habilita sugerencias tentativas de correccion | `false` |
| `OPENAI_API_KEY` | Clave secreta de OpenAI, solo backend | vacio |
| `OPENAI_MODEL` | Modelo OpenAI configurable | `gpt-5.4-mini` |
| `OPENAI_MAX_RELEVANT_PAGES` | Maximo de paginas relevantes enviadas por pregunta | `8` |
| `OPENAI_MAX_CHARACTERS_PER_PAGE` | Maximo de caracteres enviados por pagina | `6000` |

### Configurar OpenAI localmente

1. Ingresar a [OpenAI API Keys](https://platform.openai.com/api-keys).
2. Crear una clave secreta nueva y no compartirla.
3. Abrir PowerShell en la raiz del proyecto.
4. Definir las variables solo para esa terminal:

```powershell
$env:OPENAI_ENABLED="true"
$env:OPENAI_API_KEY="tu-clave"
$env:OPENAI_MODEL="gpt-5.4-mini"
.\mvnw.cmd spring-boot:run
```

La clave nunca debe colocarse en React, Vite, archivos YAML versionados, Dockerfile ni commits.
Sin clave o con `OPENAI_ENABLED=false`, toda la correccion manual continua funcionando.
La guia de migracion esta en `MIGRACION_CORRECCION_IA_OPENAI.md`.

---

## Estructura del proyecto

```
src/main/java/com/seventest/
├── domain/
│   ├── model/          # Entidades de dominio puras
│   ├── port/in/        # Interfaces de casos de uso
│   ├── port/out/       # Interfaces de repositorios y servicios externos
│   └── exception/      # Excepciones de dominio
├── application/
│   └── service/        # Implementación de los casos de uso
└── infrastructure/
    ├── config/         # Configuración y datos iniciales
    ├── email/          # Adaptador de email (log en desarrollo)
    ├── persistence/    # Entidades JPA, repositorios y adaptadores
    ├── security/       # JWT, filtros y configuración de Spring Security
    └── web/            # Controllers y DTOs REST
```
