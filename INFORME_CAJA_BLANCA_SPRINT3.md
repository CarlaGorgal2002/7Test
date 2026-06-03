# 7test — Informe de Pruebas de Caja Blanca

**Entrega:** Sprint 3, con cobertura acumulada de flujos anteriores
**Materia:** Testing de Aplicaciones — UADE
**Producto:** 7test — Sistema de gestión de evaluaciones universitarias
**Fecha:** 03/06/2026
**Tipo de pruebas:** Unitarias de caja blanca
**Herramientas:** Java 21, JUnit 5, Mockito, AssertJ, Maven Surefire

---

## 1. Resumen ejecutivo

Este informe documenta la estrategia y el resultado de las pruebas de caja blanca ejecutadas sobre el backend de 7test en el Sprint 3.

El alcance cubre acumulativamente los flujos de Sprint 2 y Sprint 3: gestión de exámenes por el profesor (creación, configuración, publicación, cierre, eliminación) y el flujo de entregas por parte del alumno (inicio, guardado de respuestas y entrega). La suite fue ejecutada con Maven Wrapper y Java 21.

Durante esta iteración se detectaron y corrigieron **dos defectos reales** identificados a partir de la ejecución del pipeline de CI/CD (ver sección 6).

El resultado final tras los arreglos fue exitoso:

| Total tests | Failures | Errors | Skipped | Estado |
|-------------|----------|--------|---------|--------|
| 59 | 0 | 0 | 0 | BUILD SUCCESS |

[[[ACÁ VA captura_01_build_success.png]]]

La ejecución completa finaliza correctamente con 59 tests ejecutados, 0 failures, 0 errors y 0 skipped. Maven informa BUILD SUCCESS, confirmando que la suite automatizada de caja blanca quedó verde.

---

## 2. Contexto y metodología de testing

Según la estrategia declarada en Sprint 1, las pruebas unitarias de caja blanca del backend se realizan con JUnit 5 y Mockito. En esta entrega se mantiene esa estrategia, se incorpora la ejecución dentro del pipeline de CI/CD configurado con GitHub Actions, y se amplía la cobertura hacia el módulo de entregas de alumnos.

| Capa | Herramienta prevista | Estado en esta entrega |
|------|---------------------|----------------------|
| Backend Java | JUnit 5 + Mockito + AssertJ | Implementado y ejecutado |
| Reportes de ejecución | Maven Surefire | Implementado y generado |
| Pipeline CI/CD | GitHub Actions (backend-ci.yml) | Implementado y ejecutado |
| Frontend React | Jest + React Testing Library | Pendiente |

La modalidad es caja blanca porque los casos se diseñaron con acceso al código fuente, identificando ramas internas, validaciones, excepciones y cambios de estado.

---

## 3. Fundamentación académica

### 3.1 ¿Qué son las pruebas de caja blanca?

Las pruebas de caja blanca son una técnica de testing en la que quien diseña los casos conoce la estructura interna del sistema. A diferencia de las pruebas de caja negra, que validan el comportamiento observable desde entradas y salidas, la caja blanca analiza directamente el código fuente, sus decisiones internas, ramas condicionales, validaciones, excepciones, caminos posibles y cambios de estado.

En este trabajo se aplican como pruebas unitarias sobre servicios del backend. Esto permite verificar, por ejemplo, que un examen solo pueda publicarse si cumple las reglas internas definidas por el código: tener temas, tener preguntas, sumar exactamente diez puntos por tema y contar con enunciados y respuestas modelo completas.

El valor de este enfoque es que permite detectar errores de lógica antes de llegar a la interfaz o a pruebas de integración. También ayuda a justificar formalmente que se cubrieron caminos positivos, negativos y casos borde.

### 3.2 ¿Por qué las ejecuta el equipo de desarrollo?

En este proyecto las pruebas de caja blanca son responsabilidad del equipo de desarrollo porque requieren conocimiento interno del código, de la arquitectura y de las dependencias entre clases. Para escribirlas y mantenerlas es necesario saber qué servicio contiene cada regla, qué repositorios intervienen, qué estados son válidos y qué excepciones debe lanzar cada rama.

El equipo de QA, en cambio, se concentra principalmente en validar el sistema desde el punto de vista funcional y de usuario, con una mirada más cercana a caja negra: probar flujos, pantallas, datos de entrada, mensajes, permisos y resultados observables. Esa separación es conveniente porque evita duplicar esfuerzos y permite que cada equipo pruebe desde una perspectiva distinta.

Por ese motivo, en esta entrega el equipo de desarrollo ejecuta y documenta la suite de caja blanca. Los resultados sirven como evidencia técnica de que la lógica interna del backend fue validada antes o en paralelo a las pruebas funcionales del equipo de QA.

### 3.3 ¿Por qué se eligieron estas herramientas?

**JUnit 5** es el framework estándar para pruebas unitarias en aplicaciones Java modernas y se integra directamente con Maven y Spring Boot. Permite definir casos de prueba claros, repetibles y automatizables.

**Mockito** permite reemplazar dependencias externas por mocks. En este caso, los servicios se prueban sin depender de una base de datos real, sin levantar servidor y sin usar la interfaz gráfica. Esto es importante para una prueba unitaria de caja blanca: el foco queda puesto en la unidad bajo prueba y no en infraestructura externa.

**AssertJ** ofrece aserciones más expresivas y legibles que facilitan verificar estados, valores y excepciones esperadas.

**Maven Surefire** ejecuta automáticamente los tests durante el ciclo de build de Maven y genera reportes verificables en `target/surefire-reports/`. Esto permite anexar evidencia de ejecución, no solo describir los casos de prueba.

**GitHub Actions** permite ejecutar la suite completa en un entorno limpio y reproducible ante cada push o pull request a la rama `main`, garantizando integración continua.

### 3.4 ¿Cómo se usaron concretamente las herramientas?

Las herramientas de testing no se usaron como aplicaciones visuales separadas, sino como librerías dentro del proyecto Java. El código de las pruebas fue abierto y revisado en VS Code, pero la ejecución real se realizó desde PowerShell mediante Maven Wrapper con el comando `.\mvnw.cmd test`.

En los archivos de test se observa el uso concreto de cada herramienta:

- `@Test` pertenece a JUnit 5 y marca los métodos que deben ejecutarse como casos de prueba.
- `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks` y `when(...)` pertenecen a Mockito y permiten crear dependencias simuladas.
- `assertThat(...)` y `assertThatThrownBy(...)` pertenecen a AssertJ y permiten verificar resultados esperados y excepciones.

Al ejecutar `.\mvnw.cmd test`, Maven Surefire detecta las clases de test, JUnit ejecuta los métodos anotados con `@Test`, Mockito crea los mocks necesarios, AssertJ evalúa las aserciones y Surefire genera los reportes de resultado en `target/surefire-reports/`.

---

## 4. Estrategia aplicada

La suite se organiza en tres grupos:

| Grupo | Propósito | Clases |
|-------|-----------|--------|
| Pruebas principales Sprint 2/3 | Validar gestión y publicación de exámenes | `ExamServiceTest` |
| Flujo de entregas | Validar inicio, guardado y entrega de exámenes | `ExamSubmissionServiceTest` |
| Regresión Sprint 1 | Asegurar que usuarios, autenticación y políticas sigan funcionando | `AuthServiceTest`, `UserServiceTest`, `PasswordPolicyServiceTest` |

Los servicios se prueban aislados, usando mocks para los repositorios. Esto permite validar la lógica interna sin depender de base de datos real, servidor desplegado ni interfaz gráfica.

[[[ACÁ VA captura_02_mockito_config_ExamServiceTest.png]]]

Se observa el uso de `@ExtendWith(MockitoExtension.class)`, `@Mock` e `@InjectMocks` en `ExamServiceTest`. Esto evidencia que el servicio se prueba de forma aislada, reemplazando sus dependencias por mocks y evitando depender de una base de datos real, servidor desplegado o interfaz gráfica.

---

## 5. Alcance cubierto

### 5.1 Servicio principal: ExamService

`ExamService` contiene la lógica principal del Sprint 2/3:

- Crear examen en borrador
- Agregar temas con asignación de color de paleta del proyecto
- Agregar y editar preguntas
- Validar que cada tema sume exactamente 10 puntos
- Validar que no se publiquen preguntas vacías
- Validar árboles y tablas de decisión vacíos
- Publicar examen
- Cerrar examen
- Eliminar examen según estado

El reporte de ejecución muestra:

```
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.830 s
```

[[[ACÁ VA captura_03_surefire_ExamServiceTest.png]]]

El reporte generado por Maven Surefire muestra que `ExamServiceTest` ejecutó 13 pruebas unitarias de caja blanca, sin failures, errors ni skipped.

### 5.2 Flujo de entregas: ExamSubmissionService

- Iniciar examen publicado con tema válido
- Crear entrega en estado `EN_PROGRESO`
- Rechazar inicio sin topicId
- Rechazar inicio con topicId inexistente
- Guardar respuestas en entrega en progreso
- Rechazar cambios cuando la entrega ya está finalizada
- Entregar examen y pasar a estado `ENTREGADO`
- Validar que no se pueda entregar dos veces

El reporte de ejecución muestra:

```
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.170 s
```

[[[ACÁ VA captura_04_surefire_ExamSubmissionServiceTest.png]]]

---

## 6. Defectos detectados y corregidos durante CI/CD

Durante la configuración del pipeline de integración continua con GitHub Actions, la ejecución automática de la suite detectó **dos defectos reales** que no eran visibles en la ejecución local. A continuación se documenta cada uno.

---

### Defecto 1 — Color incorrecto asignado a temas (`#2563EB` en lugar de `#1956D8`)

**Test que lo detectó:** `ExamServiceTest.agregarTemas_asignaColoresDelSprintDos`

**Síntoma observado en CI:**
```
Error: Failures:
Error:   ExamServiceTest.agregarTemas_asignaColoresDelSprintDos:65
Error:   expected: "#1956D8"
Error:    but was: "#2563EB"
```

**Causa raíz:** La lista de colores de temas definida en `ExamService.java` tenía como primer valor `#2563EB` (azul de la paleta Tailwind CSS), en lugar de `#1956D8` (azul oficial de la paleta del proyecto 7test definida en Sprint 2).

```java
// ANTES (incorrecto):
private static final List<String> TOPIC_COLORS = List.of(
    "#2563EB", "#16A34A", ...
);

// DESPUÉS (correcto):
private static final List<String> TOPIC_COLORS = List.of(
    "#1956D8", "#16A34A", ...
);
```

**Corrección aplicada:** Se actualizó la constante en `src/main/java/com/seventest/application/service/ExamService.java`, línea 39.

**Estado:** ✅ Corregido y verificado. El test pasa correctamente luego del arreglo.

[[[ACÁ VA captura_05_error_color_bug.png — CAPTURA MANUAL: log de GitHub Actions mostrando el error expected "#1956D8" but was "#2563EB"]]]

[[[ACÁ VA captura_06_color_corregido_1956D8.png]]]

---

### Defecto 2 — Fallo de contexto de Spring en entorno CI (`ApplicationTests.contextLoads`)

**Test que lo detectó:** `ApplicationTests.contextLoads`

**Síntoma observado en CI:**
```
Error: ApplicationTests.contextLoads = IllegalState
Failed to load ApplicationContext for [WebMergedContextConfiguration...]
```

**Causa raíz:** La configuración de base de datos en `application.yaml` apuntaba a un archivo H2 en disco:

```yaml
spring:
  datasource:
    url: jdbc:h2:file:/app/data/seventest;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE
```

La ruta `/app/data/seventest` existe en el entorno Docker de producción y en la máquina local de desarrollo, pero no en el runner de GitHub Actions (Ubuntu), lo que impedía que Spring levantara el contexto y fallaba el test de integración.

**Corrección aplicada:** Se creó el archivo `src/test/resources/application.yaml` que sobreescribe la configuración de base de datos exclusivamente para el entorno de tests, usando H2 en memoria:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
```

**Estado:** ✅ Corregido y verificado. El contexto de Spring carga correctamente en CI con H2 en memoria.

[[[ACÁ VA captura_07_error_contextloads.png — CAPTURA MANUAL: log de GitHub Actions mostrando el error "Failed to load ApplicationContext"]]]

[[[ACÁ VA captura_08_test_application_yaml.png]]]

---

## 7. Evidencia de reportes generados

Maven Surefire genera reportes de ejecución en:

```
target/surefire-reports/
```

En esta carpeta quedan los `.txt` y `.xml` de cada clase de test ejecutada.

[[[ACÁ VA captura_09_surefire_reports.png]]]

---

## 8. Matriz método / rama / test

Esta matriz conecta cada decisión interna del código con el test que la valida.

### 8.1 Cobertura de ExamService

| Método | Rama o condición interna | Test que lo cubre | Resultado esperado |
|--------|--------------------------|-------------------|--------------------|
| `create` | Profesor válido crea examen | `crearExamen_profesorValido_guardaBorrador` | Examen en `BORRADOR` |
| `addTopic` | Examen en borrador agrega tema | `agregarTemas_asignaColoresDelSprintDos` | Tema creado con color `#1956D8` |
| `publish` | Tema no suma 10 puntos | `publicarExamen_conTemaQueNoSumaDiez_rechazaPublicacion` | Rechaza publicación |
| `publish` | Todos los temas suman 10 | `publicarExamen_conCadaTemaEnDiez_cambiaEstadoAPublicado` | Estado `PUBLICADO` |
| `addQuestion` | Examen publicado | `agregarPregunta_aExamenPublicado_rechazaEdicion` | Rechaza edición |
| `publish` | Pregunta sin enunciado | `publicarExamen_conPreguntaVacia_rechazaPublicacion` | Rechaza publicación |
| `publish` | Árbol de decisión vacío | `publicarExamen_conArbolVacio_rechazaPublicacion` | Rechaza respuesta modelo incompleta |
| `publish` | Tabla de decisión vacía | `publicarExamen_conTablaVacia_rechazaPublicacion` | Rechaza respuesta modelo incompleta |
| `deleteExam` | Examen borrador | `eliminarExamen_borrador_eliminaCorrectamente` | Elimina correctamente |
| `deleteExam` | Examen cerrado con entregas | `eliminarExamen_cerradoConEntregas_rechaza` | Rechaza eliminación |

### 8.2 Cobertura de ExamSubmissionService

| Método | Rama o condición interna | Test que lo cubre | Resultado esperado |
|--------|--------------------------|-------------------|--------------------|
| `start` | Alumno inicia examen con tema válido | `iniciarExamenPublicado_conTemaValido_creaEntregaEnProgreso` | Entrega `EN_PROGRESO` |
| `start` | Sin topicId | `iniciarExamen_sinTopicId_rechaza` | Rechaza inicio |
| `start` | TopicId inexistente | `iniciarExamen_conTopicIdInexistente_rechaza` | Rechaza inicio |
| `saveAnswers` | Entrega en progreso | `guardarRespuestas_deEntregaEnProgreso_actualizaTexto` | Respuesta actualizada |
| `saveAnswers` | Entrega finalizada | `guardarRespuestas_deEntregaFinalizada_rechazaCambio` | Rechaza modificación |
| `submit` | Entrega en progreso | `entregarExamen_cambiaEstadoAEntregado` | Estado `ENTREGADO` |
| `submit` | Entrega ya finalizada | `entregarExamen_yaEntregado_rechaza` | Rechaza doble entrega |

[[[ACÁ VA captura_10_rama_negativa_publicacion.png]]]

Se observa el test `publicarExamen_conTemaQueNoSumaDiez_rechazaPublicacion`, donde se intenta publicar un examen cuyo tema suma solo 4 puntos. La prueba espera una `IllegalArgumentException` con mensaje relacionado a "10 puntos", validando que el sistema rechaza la publicación cuando no se cumple la regla interna de puntaje.

[[[ACÁ VA captura_11_rama_positiva_publicacion.png]]]

Se observa el test `publicarExamen_conCadaTemaEnDiez_cambiaEstadoAPublicado`, donde los temas cumplen la suma exacta de 10 puntos. La prueba valida que el examen cambia a estado `PUBLICADO` y que `publishedAt` queda asignado, cubriendo el camino exitoso de publicación.

[[[ACÁ VA captura_12_ExamSubmissionServiceTest.png]]]

Se observan los casos unitarios del flujo de entregas: inicio de examen publicado, guardado de respuestas en estado `EN_PROGRESO`, rechazo de cambios sobre una entrega finalizada y entrega final del examen.

---

## 9. Flujo de decisión analizado: publicación de examen

El método `publish()` de `ExamService` no solo cambia el estado del examen. Antes de publicar, llama a `validateReadyToPublish(exam)`, donde se validan las condiciones internas necesarias.

**Flujo analizado:**

```
Inicio
  |
  v
Buscar examen y validar propietario
  |
  v
¿Tiene al menos un tema?
  |-- No --> IllegalArgumentException("El examen debe tener al menos un tema")
  |
  v
¿Cada tema tiene preguntas?
  |-- No --> IllegalArgumentException("Cada tema debe tener al menos una pregunta")
  |
  v
¿Cada tema suma exactamente 10 puntos?
  |-- No --> IllegalArgumentException("Cada tema debe sumar exactamente 10 puntos")
  |
  v
¿Cada pregunta tiene enunciado?
  |-- No --> IllegalArgumentException("Todas las preguntas deben tener enunciado antes de publicar")
  |
  v
¿Cada pregunta tiene respuesta modelo?
  |-- No --> IllegalArgumentException("Todas las preguntas deben tener respuesta modelo antes de publicar")
  |
  v
Publicar examen → status = PUBLICADO, publishedAt = now()
```

[[[ACÁ VA captura_13_validateReadyToPublish.png]]]

Se observa el método `validateReadyToPublish` de `ExamService`, donde se validan las condiciones internas necesarias antes de publicar un examen: existencia de temas, existencia de preguntas, suma exacta de 10 puntos por tema, enunciado completo y respuesta modelo completa.

---

## 10. Resultado completo de ejecución

La suite completa se ejecutó con:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

Resultado por clase:

| Test set | Tests | Failures | Errors | Skipped |
|----------|-------|----------|--------|---------|
| `AuthServiceTest` | 15 | 0 | 0 | 0 |
| `ExamServiceTest` | 13 | 0 | 0 | 0 |
| `ExamSubmissionServiceTest` | 8 | 0 | 0 | 0 |
| `PasswordPolicyServiceTest` | 5 | 0 | 0 | 0 |
| `UserServiceTest` | 17 | 0 | 0 | 0 |
| `ApplicationTests` | 1 | 0 | 0 | 0 |
| **Total** | **59** | **0** | **0** | **0** |

[[[ACÁ VA captura_01_build_success.png]]]

---

## 11. Limitaciones y pendientes

| Pendiente | Impacto | Propuesta |
|-----------|---------|-----------|
| No hay medición automática con JaCoCo | No se informa porcentaje de cobertura de líneas o ramas | Agregar plugin JaCoCo en `pom.xml` en próxima iteración |
| No hay tests de frontend con Jest/RTL | La UI queda validada manualmente o por inspección estática | Agregar tests de componentes React en Sprint 4 |
| `KnownBugWhiteBoxChecks` corre separado | Documenta bugs conocidos, pero no forma parte de la suite verde | Mantenerlo como suite complementaria |
| Pipeline CI/CD requiere repositorio de snapshots de Spring | Spring Boot 4.1.0-SNAPSHOT no está en Maven Central | Configuración aplicada en `backend-ci.yml` con `repo.spring.io/snapshot` |

---

## 12. Conclusión

La entrega demuestra avance concreto de pruebas de caja blanca sobre el backend de 7test, ahora ejecutadas también dentro de un pipeline de integración continua con GitHub Actions.

La ejecución del CI/CD resultó valiosa: permitió detectar dos defectos reales que pasaban inadvertidos en la ejecución local — el color incorrecto `#2563EB` en la asignación de temas, y la configuración de H2 no compatible con el entorno de CI. Ambos fueron corregidos y verificados.

La suite finaliza correctamente con 59 tests ejecutados, 0 fallos y 0 errores. Las evidencias anexadas muestran tanto el resultado de ejecución como la relación entre código productivo, ramas internas y pruebas unitarias.
