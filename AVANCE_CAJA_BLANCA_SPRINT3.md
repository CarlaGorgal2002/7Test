# Informe de Avance de Pruebas de Caja Blanca: 100% Cobertura y Pipeline de CI

Este documento detalla el avance en las pruebas unitarias de caja blanca del backend de **7test** tras la implementación de la pipeline de integración continua (CI) y la ampliación de la cobertura de código al **100%**.

---

## 1. Resumen de Ejecución y Comparativa

Se completó una refactorización masiva y expansión de la suite de pruebas unitarias sobre la capa de servicios de aplicación (`com.seventest.application.service.*`), logrando cubrir todos los caminos lógicos posibles, escenarios de error y condiciones límite.

### Comparativa de Cobertura y Pruebas:

| Métrica | Estado Anterior (Sprint 3 Inicial) | Estado Actual (Avance Completado) | Impacto |
| :--- | :---: | :---: | :--- |
| **Total de Tests Unitarios** | 59 | **148** | +89 nuevos escenarios de prueba |
| **Cobertura JaCoCo (Instrucciones)** | No medido de forma automática | **100% (Umbral 1.00)** | Validación total de líneas ejecutadas |
| **Cobertura JaCoCo (Ramas)** | No medido de forma automática | **100% (Umbral 1.00)** | Validación total de bifurcaciones (`if`, ternary, logic) |
| **Estado de Compilación (Verify)** | Local manual | **BUILD SUCCESS** | Control de calidad automatizado integrado |

---

## 2. Detalle de Pruebas Unitarias por Clase de Servicio

Las pruebas fueron reescritas utilizando **JUnit 5 nativo** y **Mockito** para aislamiento total, estructuradas bajo el patrón **Arrange-Act-Assert (AAA)** y siguiendo la nomenclatura descriptiva `nombreMetodo_estadoBajoPrueba_comportamientoEsperado`.

### 2.1 UserServiceTest (22 Tests)
Valida la gestión de usuarios y la política de contraseñas de manera exhaustiva.
- **Creación y Modificación:** Casos de éxito y excepciones por datos inválidos, duplicación de emails y roles inapropiados.
- **Estados:** Reactivación y desactivación de cuentas con verificación de excepciones de usuario inexistente.
- **Validación de Contraseñas (Complejidad Completa):** Cobertura del 100% de ramas condicionales en `validatePassword`, evaluando cada combinación de la política de seguridad (mínimo de mayúsculas, minúsculas, números, caracteres especiales y longitud).

### 2.2 AuthServiceTest (17 Tests)
Valida el flujo de autenticación, seguridad de acceso y recuperación de cuentas.
- **Login y Bloqueos:** Inicio de sesión exitoso, manejo de intentos fallidos incrementales y bloqueo automático de cuentas al superar el límite de reintentos.
- **Manejo de Tokens y Logout:** Cierre de sesión con invalidación de tokens activos.
- **Recuperación:** Recuperación de contraseña y normalización de emails para evitar duplicados por formato de texto.

### 2.3 PasswordPolicyServiceTest (5 Tests)
Asegura la configuración y el dinamismo de las políticas de contraseñas en el backend.
- **Get y Update:** Recuperación de la política activa y cambios dinámicos en los requisitos de seguridad.

### 2.4 ExamServiceTest (66 Tests)
Clase central que contiene las reglas complejas de diseño de exámenes. Se escribieron 66 tests cubriendo todas las condiciones de integridad académica:
- **Gestión de Temas y Preguntas:** Asignación automática de la paleta de colores del proyecto y validación de borrado/modificación de preguntas.
- **Validación para Publicación (`validateReadyToPublish`):**
  - Validación de que el examen tenga al menos un tema y cada tema tenga preguntas.
  - Validación exacta de que la suma de puntajes de preguntas por tema sea exactamente **10 puntos**.
  - Rechazo de preguntas con enunciados o respuestas modelo vacías.
- **Estructuras de Decisión Avanzadas (Árboles y Tablas de Decisión):**
  - Detección de árboles de decisión vacíos y parseo correcto de nodos con texto no blanco.
  - Detección de tablas de decisión vacías y validación de celdas con datos reales.
- **Eliminación Segura:** Impedimento de eliminar exámenes publicados o cerrados con entregas existentes.

### 2.5 ExamSubmissionServiceTest (37 Tests)
Valida la interacción del alumno con las evaluaciones y la posterior calificación por parte del profesor.
- **Inicio de Examen:** Validación de estado `PUBLICADO`, fecha de disponibilidad (`availableFrom`) en el futuro/pasado, selección obligatoria de temas válidos y prevención de doble inicio.
- **Guardado y Entrega:** Guardado dinámico de respuestas (previniendo modificaciones una vez entregado), manejo de respuestas duplicadas y entrega con cambio de estado a `ENTREGADO`.
- **Calificación por el Profesor:**
  - Control de rangos de puntaje (no negativo y menor/igual al puntaje máximo de la pregunta).
  - Comentarios opcionales (limpieza de espacios y nulos).
  - Manejo de calificaciones de preguntas no pertenecientes al tema asignado (`max == null`).
  - Restricción de acceso para que los profesores solo califiquen entregas de sus propios exámenes.

---

## 3. Automatización de Cobertura con JaCoCo

El plugin de JaCoCo fue configurado en el `pom.xml` para aplicar reglas estrictas de bloqueo en la fase `verify` de Maven.

### Exclusiones Configuradas:
Para evitar falsos negativos de cobertura en código meramente declarativo u operacional, se excluyeron los siguientes paquetes:
- Clases de configuración e inicio de Spring Boot (`Application.class`).
- Capa de datos y modelos JPA (`com/seventest/domain/model/**`).
- Capa de excepciones personalizadas (`com/seventest/domain/exception/**`).
- Interfaces y puertos de la arquitectura hexagonal (`com/seventest/domain/port/**`).
- Puertos de infraestructura, controladores y DTOs (`com/seventest/infrastructure/**`).

Esto permite enfocar la cobertura de forma pura y estricta en el paquete de lógica de negocio: **`com.seventest.application.service.*`**, donde se exige y alcanza el **100%** de cobertura en instrucciones y ramas.

---

## 4. Pipeline de Integración Continua (CI)

Se configuró el workflow en `.github/workflows/ci.yml` que valida automáticamente cada Pull Request antes de su merge en `main`:

```mermaid
graph TD
    PR[Creación de Pull Request] --> Checkout[1. Descarga del código]
    Checkout --> SetupJava[2. Configuración de JDK 21]
    SetupJava --> MavenVerify[3. mvnw clean verify - Tests & JaCoCo 100%]
    MavenVerify --> StartBoot[4. Arranque de Spring Boot en 2do Plano con H2]
    StartBoot --> PingHealth[5. Pings de Salud HTTP a /v3/api-docs]
    PingHealth --> StopBoot[6. Apagado del Servidor]
    StopBoot --> BuildSuccess[PR Lista para Fusión (BUILD SUCCESS)]
```

### Flujo de Verificación del Backend:
1. **Compilación y Tests:** Ejecuta `./mvnw clean verify` para validar compilar, ejecutar todos los tests unitarios y corroborar la cobertura de JaCoCo.
2. **Prueba de Arranque (Boot Check):** Inicia la aplicación usando una base de datos H2 en memoria (`jdbc:h2:mem:testdb`) para validar que la aplicación levante sin dependencias externas.
3. **Verificación de Salud:** Realiza llamadas de HTTP a `/v3/api-docs` durante un máximo de 60 segundos. Si el servidor responde HTTP 200, confirma el inicio seguro. Si falla o se detiene, muestra los logs en consola y cancela el merge.

---

## 5. Conclusión

La suite de caja blanca del backend de **7test** ha alcanzado su madurez máxima en este Sprint:
- Se expandió la cantidad de pruebas a **148 casos unitarios**, cubriendo todas las ramas de lógica del negocio.
- Se implementó la automatización de cobertura de código al **100%** con JaCoCo, eliminando dependencias de código muerto.
- Se integró una pipeline de CI robusta que valida tanto la cobertura de código como la salud y arranque exitoso de la aplicación en cada PR.
