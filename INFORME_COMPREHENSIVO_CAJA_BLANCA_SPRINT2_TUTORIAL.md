# 7test - Informe de estrategia y avance de pruebas de caja blanca

**Entrega:** Sprint 2, con avance parcial del flujo posterior  
**Materia:** Testing de Aplicaciones - UADE  
**Producto:** 7test - Sistema de gestion de evaluaciones universitarias  
**Fecha de preparacion:** 28/05/2026  
**Equipo:** Desarrollo  
**Tipo de pruebas:** pruebas unitarias de caja blanca  
**Stack tecnico:** Java 21, Spring Boot, JUnit 5, Mockito, AssertJ  

[[[ANTES DE ENTREGAR: SI EL PROFESOR NO PIDIO VER EL TUTORIAL, DEJAR SOLO EL INFORME Y BORRAR O RECORTAR LOS BLOQUES ENTRE TRIPLE CORCHETE. SI QUIEREN MOSTRAR PROCESO DE ELABORACION, DEJARLOS COMO ANEXO INTERNO.]]]

## 1. Resumen ejecutivo

Este informe documenta la estrategia y el avance de las pruebas de caja blanca realizadas sobre el backend de 7test. La entrega se enfoca principalmente en el alcance del Sprint 2: creacion, configuracion, publicacion, cierre y eliminacion de examenes por parte del profesor. Adicionalmente, dentro de la misma suite de pruebas se incluye un avance parcial del flujo posterior, correspondiente al inicio, guardado y entrega de examenes por parte del alumno.

La estrategia utilizada sigue el criterio definido desde las entregas anteriores: pruebas unitarias con JUnit 5 y Mockito, aislando cada servicio de sus dependencias externas. Los casos fueron disenados a partir de la lectura del codigo fuente, identificando ramas internas, validaciones de negocio, transiciones de estado y casos borde.

Resultado de ejecucion de la suite:

| Total tests | Failures | Errors | Skipped | Estado |
|---:|---:|---:|---:|---|
| 55 | 0 | 0 | 0 | BUILD SUCCESS |

[[[CAPTURA 01: ABRI POWERSHELL EN C:\USERS\NAPERO\DESKTOP\7TEST-HOSTEADO. EJECUTA LOS COMANDOS DE LA SECCION 7. SACA CAPTURA DONDE SE VEA EL COMANDO `.\MVNW.CMD TEST`, EL TOTAL `TESTS RUN: 55` Y `BUILD SUCCESS`. GUARDA LA IMAGEN COMO `EVIDENCIAS/CAPTURA-01-BUILD-SUCCESS.PNG`.]]]

## 2. Contexto del producto y de la release

7test es una plataforma web orientada a digitalizar el proceso de evaluacion universitaria. El caso piloto corresponde al primer parcial de la materia Testing de Aplicaciones. El objetivo del sistema es reemplazar el flujo manual en papel por una aplicacion accesible desde navegador.

Segun la release note del proyecto, el modulo de examenes incluye:

- Creacion y gestion de examenes por parte del profesor.
- Gestion de temas y preguntas.
- Validacion de que cada tema sume exactamente diez puntos.
- Publicacion y cierre de examenes.
- Vista de examenes publicados para alumnos.
- Inicio y entrega de examen.
- Supervision de examenes por roles administrativos.

Para esta entrega de caja blanca, el foco principal esta puesto en el modulo de examenes del profesor, que representa el nucleo del Sprint 2. Como avance adicional, se incluye una primera cobertura unitaria del ciclo de entregas.

[[[CAPTURA 02: ABRI LA RELEASE NOTE QUE TE PASARON O EL ARCHIVO `RELEASE_NOTE_V1.2.MD` EN EL REPOSITORIO. SACA CAPTURA DE LA SECCION DONDE APARECEN LAS FUNCIONALIDADES DEL MODULO DE EXAMENES Y LA SECCION DE PRUEBAS UNITARIAS. GUARDA LA IMAGEN COMO `EVIDENCIAS/CAPTURA-02-RELEASE-NOTE.PNG`.]]]

## 3. Objetivo de las pruebas de caja blanca

El objetivo de las pruebas de caja blanca es validar la logica interna del backend sin depender de la interfaz grafica, del servidor desplegado ni de una base de datos real. Para eso se prueban los servicios de aplicacion directamente, reemplazando repositorios y dependencias externas por mocks.

En particular, se busca comprobar:

- Que los examenes se creen en estado correcto.
- Que las reglas de edicion solo permitan modificar examenes en borrador.
- Que la publicacion exija integridad minima: temas, preguntas, puntaje y respuestas modelo.
- Que las estructuras especiales, como arboles y tablas de decision, no puedan publicarse vacias.
- Que la eliminacion respete el estado del examen y la existencia de entregas.
- Que el avance parcial del flujo de entregas respete los estados `EN_PROGRESO` y `ENTREGADO`.
- Que las funcionalidades existentes de Sprint 1 sigan pasando como regresion.

## 4. Estrategia aplicada

La estrategia fue dividir la suite en tres grupos:

| Grupo | Proposito | Clases incluidas |
|---|---|---|
| Pruebas principales Sprint 2 | Validar gestion y publicacion de examenes | `ExamServiceTest` |
| Avance parcial posterior | Validar inicio, guardado y entrega de examenes | `ExamSubmissionServiceTest` |
| Regresion Sprint 1 | Asegurar que auth, usuarios y politicas no se rompan | `AuthServiceTest`, `UserServiceTest`, `PasswordPolicyServiceTest` |

La modalidad es caja blanca porque los casos fueron disenados mirando el codigo interno de los servicios. No se parte solo de entradas y salidas visibles para el usuario, sino de ramas concretas del codigo: validaciones, excepciones, estados de dominio, llamados a repositorios y condiciones de borde.

### Herramientas y tecnica

| Herramienta / tecnica | Uso |
|---|---|
| JUnit 5 | Definicion y ejecucion de los casos unitarios |
| Mockito | Reemplazo de repositorios por mocks |
| `@ExtendWith(MockitoExtension.class)` | Habilita Mockito en los tests |
| `@Mock` | Crea dobles de prueba para repositorios |
| `@InjectMocks` | Inyecta los mocks en el servicio real |
| AssertJ | Aserciones legibles para valores y excepciones |
| Maven Surefire | Ejecucion y reportes de tests |

[[[CAPTURA 03: ABRI `SRC/TEST/JAVA/COM/SEVENTEST/APPLICATION/SERVICE/EXAMSERVICETEST.JAVA` EN INTELLIJ O VS CODE. SACA CAPTURA DONDE SE VEAN `@EXTENDWITH(MOCKITOEXTENSION.CLASS)`, `@MOCK` Y `@INJECTMOCKS`. GUARDA COMO `EVIDENCIAS/CAPTURA-03-MOCKITO-EXAMSERVICE.PNG`.]]]

## 5. Alcance de clases bajo prueba

### 5.1 Servicio principal del Sprint 2

| Servicio | Archivo de test | Responsabilidad |
|---|---|---|
| `ExamService` | `src/test/java/com/seventest/application/service/ExamServiceTest.java` | Creacion, edicion, publicacion, cierre y eliminacion de examenes |

### 5.2 Avance parcial incluido en la misma suite

| Servicio | Archivo de test | Responsabilidad |
|---|---|---|
| `ExamSubmissionService` | `src/test/java/com/seventest/application/service/ExamSubmissionServiceTest.java` | Inicio, guardado y entrega de examen |

### 5.3 Regresion

| Servicio | Archivo de test | Responsabilidad |
|---|---|---|
| `AuthService` | `AuthServiceTest.java` | Login, logout, recuperacion y bloqueo |
| `UserService` | `UserServiceTest.java` | Alta, edicion, activacion/desactivacion y filtros |
| `PasswordPolicyService` | `PasswordPolicyServiceTest.java` | Reglas de contrasena |
| Contexto Spring | `ApplicationTests.java` | Smoke test de carga de aplicacion |

## 6. Matriz de cobertura metodo / rama / test

Esta matriz conecta explicitamente la logica interna observada con el test que la valida.

### 6.1 Cobertura Sprint 2 - `ExamService`

| Metodo | Rama o condicion interna | Test que lo cubre | Resultado esperado |
|---|---|---|---|
| `create` | Profesor valido crea examen | `crearExamen_profesorValido_guardaBorrador` | Examen en `BORRADOR` |
| `addTopic` | Se agrega primer tema en borrador | `agregarTemas_asignaColoresDelSprintDos` | Color `#1956D8` asignado |
| `publish` | Tema no suma 10 puntos | `publicarExamen_conTemaQueNoSumaDiez_rechazaPublicacion` | Excepcion por puntaje |
| `publish` | Todos los temas suman 10 | `publicarExamen_conCadaTemaEnDiez_cambiaEstadoAPublicado` | Estado `PUBLICADO` |
| `addQuestion` | Examen publicado | `agregarPregunta_aExamenPublicado_rechazaEdicion` | Rechaza edicion |
| `addQuestion` | Pregunta vacia en borrador | `agregarPregunta_enBorradorPermiteEspaciosVacios` | Permite borrador incompleto |
| `addQuestion` | Tema supera 10 durante edicion | `agregarPregunta_siTemaSuperaDiez_permiteCambioParaRedistribucion` | Permite redistribucion |
| `publish` | Pregunta sin enunciado | `publicarExamen_conPreguntaVacia_rechazaPublicacion` | Rechaza publicacion |
| `publish` | Arbol de decision vacio | `publicarExamen_conArbolVacio_rechazaPublicacion` | Rechaza respuesta modelo vacia |
| `publish` | Tabla de decision vacia | `publicarExamen_conTablaVacia_rechazaPublicacion` | Rechaza respuesta modelo vacia |
| `deleteExam` | Examen borrador | `eliminarExamen_borrador_eliminaCorrectamente` | Elimina correctamente |
| `deleteExam` | Examen cerrado con entregas | `eliminarExamen_cerradoConEntregas_rechaza` | Rechaza eliminacion |
| `updateQuestion` | Tema queda arriba de 10 en edicion | `editarPregunta_siTemaSuperaDiez_permiteCambioParaRedistribucion` | Permite estado intermedio |

[[[CAPTURA 04: EN EL ARCHIVO `EXAMSERVICETEST.JAVA`, BUSCA LOS TESTS `PUBLICAREXAMEN_CONTEMAQUENOSUMADIEZ_RECHAZAPUBLICACION` Y `PUBLICAREXAMEN_CONCADATEMAENDIEZ_CAMBIAESTADOAPUBLICADO`. SACA CAPTURA DONDE SE VEAN AMBOS O AL MENOS UNO DE CADA RAMA: UNA RAMA DE ERROR Y UNA RAMA EXITOSA. GUARDA COMO `EVIDENCIAS/CAPTURA-04-RAMAS-PUBLISH.PNG`.]]]

### 6.2 Avance parcial posterior - `ExamSubmissionService`

| Metodo | Rama o condicion interna | Test que lo cubre | Resultado esperado |
|---|---|---|---|
| `start` | Alumno inicia examen publicado | `iniciarExamenPublicado_creaEntregaEnProgresoConTemaAsignado` | Entrega `EN_PROGRESO` |
| `saveAnswers` | Entrega en progreso | `guardarRespuestas_deEntregaEnProgreso_actualizaTexto` | Respuesta actualizada |
| `saveAnswers` | Entrega ya finalizada | `guardarRespuestas_deEntregaFinalizada_rechazaCambio` | Rechaza modificacion |
| `submit` | Entrega en progreso | `entregarExamen_cambiaEstadoAEntregado` | Estado `ENTREGADO` |

[[[CAPTURA 05: ABRI `SRC/TEST/JAVA/COM/SEVENTEST/APPLICATION/SERVICE/EXAMSUBMISSIONSERVICETEST.JAVA`. SACA CAPTURA DONDE SE VEAN LOS TESTS DE `SAVEANSWERS`: UNO QUE ACTUALIZA RESPUESTA Y OTRO QUE RECHAZA CAMBIOS SI ESTA ENTREGADO. GUARDA COMO `EVIDENCIAS/CAPTURA-05-EXAMSUBMISSION.PNG`.]]]

## 7. Ejecucion de la suite

La suite fue ejecutada localmente con Java 21 utilizando Maven Wrapper.

Comando utilizado:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

Resultado observado:

```text
Tests run: 55, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

[[[CAPTURA 06: ABRI POWERSHELL EN `C:\USERS\NAPERO\DESKTOP\7TEST-HOSTEADO`. PEGA Y EJECUTA LOS TRES COMANDOS DE ARRIBA. ESPERA A QUE TERMINE. SACA CAPTURA DE LA PARTE FINAL DONDE DIGA `TESTS RUN: 55, FAILURES: 0, ERRORS: 0, SKIPPED: 0` Y `BUILD SUCCESS`. GUARDA COMO `EVIDENCIAS/CAPTURA-06-MVN-TEST.PNG`.]]]

### 7.1 Resultado por clase de test

| Test set | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `AuthServiceTest` | 15 | 0 | 0 | 0 |
| `ExamServiceTest` | 13 | 0 | 0 | 0 |
| `ExamSubmissionServiceTest` | 4 | 0 | 0 | 0 |
| `PasswordPolicyServiceTest` | 5 | 0 | 0 | 0 |
| `UserServiceTest` | 17 | 0 | 0 | 0 |
| `ApplicationTests` | 1 | 0 | 0 | 0 |
| **Total** | **55** | **0** | **0** | **0** |

## 8. Evidencia generada

Maven Surefire genera reportes de ejecucion en:

```text
target/surefire-reports/
```

Archivos relevantes:

| Archivo | Evidencia |
|---|---|
| `com.seventest.application.service.ExamServiceTest.txt` | Resultado de pruebas unitarias de gestion de examenes |
| `com.seventest.application.service.ExamSubmissionServiceTest.txt` | Resultado del avance parcial de entregas |
| `com.seventest.application.service.AuthServiceTest.txt` | Regresion de autenticacion |
| `com.seventest.application.service.UserServiceTest.txt` | Regresion de usuarios |
| `com.seventest.application.service.PasswordPolicyServiceTest.txt` | Regresion de politica de contrasenas |
| `com.seventest.ApplicationTests.txt` | Smoke test del contexto Spring |

[[[CAPTURA 07: ABRI EL EXPLORADOR DE WINDOWS EN `C:\USERS\NAPERO\DESKTOP\7TEST-HOSTEADO\TARGET\SUREFIRE-REPORTS`. SACA CAPTURA DONDE SE VEAN LOS ARCHIVOS `.TXT` Y `TEST-...XML`. GUARDA COMO `EVIDENCIAS/CAPTURA-07-SUREFIRE-REPORTS.PNG`.]]]

[[[CAPTURA 08: ABRI `TARGET\SUREFIRE-REPORTS\COM.SEVENTEST.APPLICATION.SERVICE.EXAMSERVICETEST.TXT`. SACA CAPTURA DONDE SE VEA `TESTS RUN: 13, FAILURES: 0, ERRORS: 0, SKIPPED: 0`. GUARDA COMO `EVIDENCIAS/CAPTURA-08-EXAMSERVICE-RESULT.PNG`.]]]

[[[CAPTURA 09: ABRI `TARGET\SUREFIRE-REPORTS\COM.SEVENTEST.APPLICATION.SERVICE.EXAMSUBMISSIONSERVICETEST.TXT`. SACA CAPTURA DONDE SE VEA `TESTS RUN: 4, FAILURES: 0, ERRORS: 0, SKIPPED: 0`. GUARDA COMO `EVIDENCIAS/CAPTURA-09-EXAMSUBMISSION-RESULT.PNG`.]]]

## 9. Flujo de decision analizado: publicacion de examen

El metodo `publish()` de `ExamService` concentra una parte importante de la logica critica del Sprint 2. Para publicar un examen, el servicio no solo cambia el estado, sino que primero valida condiciones internas del modelo.

```text
Inicio
  |
  v
Buscar examen y validar propietario
  |
  v
Tiene al menos un tema?
  |-- No --> Error: el examen debe tener al menos un tema
  |
  v
Por cada tema:
  |
  v
Tiene preguntas?
  |-- No --> Error: cada tema debe tener preguntas
  |
  v
El total del tema suma exactamente 10?
  |-- No --> Error: cada tema debe sumar exactamente 10 puntos
  |
  v
Por cada pregunta:
  |
  v
Tiene enunciado?
  |-- No --> Error: falta enunciado
  |
  v
Tiene respuesta modelo completa?
  |-- No --> Error: falta respuesta modelo
  |
  v
Publicar examen:
status = PUBLICADO
publishedAt = now
```

Esta decision se cubre con casos positivos y negativos:

- Caso positivo: `publicarExamen_conCadaTemaEnDiez_cambiaEstadoAPublicado`.
- Caso negativo por puntaje: `publicarExamen_conTemaQueNoSumaDiez_rechazaPublicacion`.
- Caso negativo por enunciado vacio: `publicarExamen_conPreguntaVacia_rechazaPublicacion`.
- Caso negativo por arbol vacio: `publicarExamen_conArbolVacio_rechazaPublicacion`.
- Caso negativo por tabla vacia: `publicarExamen_conTablaVacia_rechazaPublicacion`.

[[[CAPTURA 10: EN `SRC/MAIN/JAVA/COM/SEVENTEST/APPLICATION/SERVICE/EXAMSERVICE.JAVA`, BUSCA EL METODO `VALIDATEREADYTOPUBLISH`. SACA CAPTURA DONDE SE VEAN LAS VALIDACIONES DE TEMAS, PUNTAJE, ENUNCIADO Y RESPUESTA MODELO. GUARDA COMO `EVIDENCIAS/CAPTURA-10-CODIGO-PUBLISH.PNG`.]]]

## 10. Capturas sugeridas para anexar

Para que la entrega tenga evidencia suficiente, se recomienda anexar estas capturas:

| Captura | Contenido | Archivo sugerido |
|---|---|---|
| 01 | Resultado general de Maven con `BUILD SUCCESS` | `captura-01-build-success.png` |
| 02 | Release note con alcance del modulo de examenes | `captura-02-release-note.png` |
| 03 | Uso de Mockito en `ExamServiceTest` | `captura-03-mockito-examservice.png` |
| 04 | Tests de ramas positiva/negativa de `publish()` | `captura-04-ramas-publish.png` |
| 05 | Tests de `ExamSubmissionService` | `captura-05-examsubmission.png` |
| 06 | Salida final de `mvnw.cmd test` | `captura-06-mvn-test.png` |
| 07 | Carpeta `target/surefire-reports` | `captura-07-surefire-reports.png` |
| 08 | Resultado `ExamServiceTest.txt` | `captura-08-examservice-result.png` |
| 09 | Resultado `ExamSubmissionServiceTest.txt` | `captura-09-examsubmission-result.png` |
| 10 | Codigo productivo de validacion de publicacion | `captura-10-codigo-publish.png` |

[[[PARA ARMAR LAS CAPTURAS EN WINDOWS: PRESIONA `WIN + SHIFT + S`, SELECCIONA EL AREA, ABRI PAINT O PEGALO DIRECTO EN WORD/GOOGLE DOCS. SI VAS A GUARDAR IMAGENES, CREA UNA CARPETA LLAMADA `EVIDENCIAS` EN EL REPOSITORIO Y GUARDA AHI CADA CAPTURA CON LOS NOMBRES SUGERIDOS.]]]

## 11. Limitaciones y pendientes

| Pendiente | Impacto | Propuesta |
|---|---|---|
| No hay medicion automatica de cobertura con JaCoCo | No se informa porcentaje real de lineas o ramas | Agregar plugin JaCoCo y umbral minimo |
| No hay tests unitarios de frontend | La UI se valida principalmente en forma manual | Agregar React Testing Library |
| `ApplicationTests` levanta contexto con H2 | Puede depender del entorno local | Crear perfil `test` con H2 aislado |
| Suite de bugs conocidos corre separada | No forma parte de la suite verde | Mantenerla como documentacion ejecutable de defectos |

## 12. Conclusion

La entrega presenta una estrategia de caja blanca basada en pruebas unitarias aisladas, derivadas de la estructura interna del codigo. El foco principal corresponde al Sprint 2: gestion de examenes por profesor, validaciones previas a la publicacion y restricciones de edicion segun estado. Dentro de la misma suite se incluye tambien un avance parcial del flujo posterior, cubriendo inicio, guardado y entrega de examenes.

La suite ejecuta 55 pruebas automatizadas con JUnit 5 + Mockito: 13 directamente asociadas a `ExamService`, 4 al avance de `ExamSubmissionService`, 37 de regresion de Sprint 1 y 1 smoke test de contexto Spring. La ejecucion finaliza con `BUILD SUCCESS`, sin fallos ni errores.

[[[REVISION FINAL ANTES DE ENTREGAR: VERIFICA QUE EL PDF O DOCUMENTO FINAL TENGA EL INFORME, LA TABLA DE RESULTADOS, LA MATRIZ METODO/RAMA/TEST, EL FLUJO DE DECISION Y AL MENOS 4 CAPTURAS: BUILD SUCCESS, EXAMSERVICETEST, SUREFIRE REPORTS Y CODIGO DE VALIDACION DE PUBLICACION.]]]

