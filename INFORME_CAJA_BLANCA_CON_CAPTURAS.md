# 7test - Informe de pruebas de caja blanca

**Entrega:** Sprint 2, con avance parcial del flujo posterior  
**Materia:** Testing de Aplicaciones - UADE  
**Producto:** 7test - Sistema de gestion de evaluaciones universitarias  
**Fecha de preparacion:** 28/05/2026  
**Tipo de pruebas:** unitarias de caja blanca  
**Herramientas:** Java 21, JUnit 5, Mockito, AssertJ, Maven Surefire  

## 1. Resumen ejecutivo

Este informe documenta la estrategia y el avance de las pruebas de caja blanca realizadas sobre el backend de 7test.

El alcance principal corresponde al Sprint 2: gestion de examenes por profesor, incluyendo creacion, configuracion, publicacion, cierre y eliminacion de examenes. Dentro de la misma suite se incluye tambien un avance parcial del flujo posterior, correspondiente al inicio, guardado y entrega de examenes por parte del alumno.

La suite fue ejecutada localmente con Maven Wrapper y Java 21. El resultado final fue exitoso:

| Total tests | Failures | Errors | Skipped | Estado |
|---:|---:|---:|---:|---|
| 55 | 0 | 0 | 0 | BUILD SUCCESS |

**[CAPTURA 1 - Resultado final de ejecucion]**  
Pegar aca la captura donde se ve `Tests run: 55, Failures: 0, Errors: 0, Skipped: 0` y `BUILD SUCCESS`.

> Imagen recomendada: la captura final de PowerShell donde aparece el resumen verde con `BUILD SUCCESS`.

## 2. Contexto y metodologia de testing

Segun la estrategia declarada en Sprint 1, las pruebas unitarias de caja blanca del backend se realizan con JUnit 5 y Mockito. Para esta entrega se mantiene esa estrategia y se amplia la cobertura hacia el modulo de examenes.

| Capa | Herramienta prevista | Estado en esta entrega |
|---|---|---|
| Backend Java | JUnit 5 + Mockito + AssertJ | Implementado y ejecutado |
| Reportes de ejecucion | Maven Surefire | Implementado y generado |
| Frontend React | Jest + React Testing Library | Pendiente |

La modalidad es caja blanca porque los casos se disenaron con acceso al codigo fuente, identificando ramas internas, validaciones, excepciones y cambios de estado.

## 3. Fundamentacion academica

### 3.1 Que son las pruebas de caja blanca

Las pruebas de caja blanca son una tecnica de testing en la que quien disena los casos de prueba conoce la estructura interna del sistema. A diferencia de las pruebas de caja negra, que validan el comportamiento observable desde entradas y salidas, la caja blanca analiza directamente el codigo fuente, sus decisiones internas, ramas condicionales, validaciones, excepciones, caminos posibles y cambios de estado.

En este trabajo se aplican como pruebas unitarias sobre servicios del backend. Esto permite verificar, por ejemplo, que un examen solo pueda publicarse si cumple las reglas internas definidas por el codigo: tener temas, tener preguntas, sumar exactamente diez puntos por tema y contar con enunciados y respuestas modelo completas.

El valor de este enfoque es que permite detectar errores de logica antes de llegar a la interfaz o a pruebas de integracion. Tambien ayuda a justificar formalmente que se cubrieron caminos positivos, caminos negativos y casos borde.

### 3.2 Por que las ejecuta el equipo de desarrollo

En este proyecto las pruebas de caja blanca son responsabilidad del equipo de desarrollo porque requieren conocimiento interno del codigo, de la arquitectura y de las dependencias entre clases. Para escribirlas y mantenerlas es necesario saber que servicio contiene cada regla, que repositorios intervienen, que estados son validos y que excepciones debe lanzar cada rama.

El equipo de QA, en cambio, se concentra principalmente en validar el sistema desde el punto de vista funcional y de usuario, es decir, con una mirada mas cercana a caja negra: probar flujos, pantallas, datos de entrada, mensajes, permisos y resultados observables. Esa separacion es conveniente porque evita duplicar esfuerzos y permite que cada equipo pruebe desde una perspectiva distinta.

Por ese motivo, en esta entrega el equipo de desarrollo ejecuta y documenta la suite de caja blanca. Los resultados sirven como evidencia tecnica de que la logica interna del backend fue validada antes o en paralelo a las pruebas funcionales del equipo de QA.

### 3.3 Por que se eligieron estas herramientas

Se eligio JUnit 5 porque es el framework estandar para pruebas unitarias en aplicaciones Java modernas y se integra directamente con Maven y Spring Boot. Permite definir casos de prueba claros, repetibles y automatizables.

Se eligio Mockito porque permite reemplazar dependencias externas por mocks. En este caso, los servicios se prueban sin depender de una base de datos real, sin levantar servidor y sin usar la interfaz grafica. Esto es importante para una prueba unitaria de caja blanca: el foco queda puesto en la unidad bajo prueba y no en infraestructura externa.

Se utilizo AssertJ porque ofrece aserciones mas expresivas y legibles que facilitan verificar estados, valores y excepciones esperadas. Por ejemplo, permite expresar claramente que un metodo debe lanzar `IllegalArgumentException` cuando un tema no suma diez puntos.

Finalmente, Maven Surefire se utiliza porque es el mecanismo que ejecuta automaticamente los tests durante el ciclo de build de Maven y genera reportes verificables en `target/surefire-reports/`. Esto permite anexar evidencia de ejecucion, no solo describir los casos de prueba.

### 3.4 Como se usaron concretamente las herramientas

Las herramientas de testing no se usaron como aplicaciones visuales separadas, sino como librerias dentro del proyecto Java. El codigo de las pruebas fue abierto y revisado en VS Code, pero la ejecucion real se realizo desde PowerShell mediante Maven Wrapper con el comando `.\mvnw.cmd test`.

En los archivos de test se observa el uso concreto de cada herramienta:

- `@Test` pertenece a JUnit 5 y marca los metodos que deben ejecutarse como casos de prueba.
- `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks` y `when(...)` pertenecen a Mockito y permiten crear dependencias simuladas.
- `assertThat(...)` y `assertThatThrownBy(...)` pertenecen a AssertJ y permiten verificar resultados esperados y excepciones.

Al ejecutar `.\mvnw.cmd test`, Maven Surefire detecta las clases de test, JUnit ejecuta los metodos anotados con `@Test`, Mockito crea los mocks necesarios, AssertJ evalua las aserciones y Surefire genera los reportes de resultado en `target/surefire-reports/`.

## 4. Estrategia aplicada

La suite se organiza en tres grupos:

| Grupo | Proposito | Clases |
|---|---|---|
| Pruebas principales Sprint 2 | Validar gestion y publicacion de examenes | `ExamServiceTest` |
| Avance parcial posterior | Validar inicio, guardado y entrega de examenes | `ExamSubmissionServiceTest` |
| Regresion Sprint 1 | Asegurar que usuarios, autenticacion y politicas sigan funcionando | `AuthServiceTest`, `UserServiceTest`, `PasswordPolicyServiceTest` |

Los servicios se prueban aislados, usando mocks para los repositorios. Esto permite validar la logica interna sin depender de base de datos real, servidor desplegado ni interfaz grafica.

**[CAPTURA 2 - Configuracion de Mockito en ExamServiceTest]**  
Pegar aca la captura donde se ven `@ExtendWith(MockitoExtension.class)`, `@Mock` y `@InjectMocks` en `ExamServiceTest`.

> Imagen recomendada: la captura del archivo `ExamServiceTest.java` donde aparecen los mocks y el primer test.

## 5. Alcance cubierto

### 5.1 Servicio principal: `ExamService`

`ExamService` contiene la logica principal del Sprint 2:

- crear examen en borrador;
- agregar temas;
- agregar y editar preguntas;
- validar que cada tema sume exactamente 10 puntos;
- validar que no se publiquen preguntas vacias;
- validar arboles y tablas de decision vacios;
- publicar examen;
- cerrar examen;
- eliminar examen segun estado.

El reporte de ejecucion muestra:

```text
Tests run: 13, Failures: 0, Errors: 0, Skipped: 0
```

**[CAPTURA 3 - Resultado de ExamServiceTest]**  
Pegar aca la captura del archivo `com.seventest.application.service.ExamServiceTest.txt`.

> Imagen recomendada: la captura del Bloc de notas donde se ve `Tests run: 13`.

### 5.2 Avance parcial: `ExamSubmissionService`

Aunque el foco de la entrega es Sprint 2, tambien se incluye un avance parcial del flujo posterior:

- iniciar examen publicado;
- crear entrega en estado `EN_PROGRESO`;
- guardar respuestas;
- rechazar cambios cuando la entrega ya esta finalizada;
- entregar examen y pasar a estado `ENTREGADO`.

El reporte de ejecucion muestra:

```text
Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
```

**[CAPTURA 4 - Resultado de ExamSubmissionServiceTest]**  
Pegar aca la captura del archivo `com.seventest.application.service.ExamSubmissionServiceTest.txt`.

> Imagen recomendada: la captura del Bloc de notas donde se ve `Tests run: 4`.

## 6. Evidencia de reportes generados

Maven Surefire genera reportes de ejecucion en:

```text
target/surefire-reports/
```

En esta carpeta quedan los `.txt` y `.xml` de cada clase de test ejecutada.

**[CAPTURA 5 - Carpeta target/surefire-reports]**  
Pegar aca la captura del Explorador de Windows mostrando los reportes generados.

> Imagen recomendada: la captura de la carpeta `surefire-reports` donde se ven archivos `.txt` y `TEST-...xml`.

## 7. Matriz metodo / rama / test

Esta matriz conecta cada decision interna del codigo con el test que la valida.

### 7.1 Cobertura de `ExamService`

| Metodo | Rama o condicion interna | Test que lo cubre | Resultado esperado |
|---|---|---|---|
| `create` | Profesor valido crea examen | `crearExamen_profesorValido_guardaBorrador` | Examen en `BORRADOR` |
| `addTopic` | Examen en borrador agrega tema | `agregarTemas_asignaColoresDelSprintDos` | Tema creado con color asignado |
| `publish` | Tema no suma 10 puntos | `publicarExamen_conTemaQueNoSumaDiez_rechazaPublicacion` | Rechaza publicacion |
| `publish` | Todos los temas suman 10 | `publicarExamen_conCadaTemaEnDiez_cambiaEstadoAPublicado` | Estado `PUBLICADO` |
| `addQuestion` | Examen publicado | `agregarPregunta_aExamenPublicado_rechazaEdicion` | Rechaza edicion |
| `publish` | Pregunta sin enunciado | `publicarExamen_conPreguntaVacia_rechazaPublicacion` | Rechaza publicacion |
| `publish` | Arbol de decision vacio | `publicarExamen_conArbolVacio_rechazaPublicacion` | Rechaza respuesta modelo incompleta |
| `publish` | Tabla de decision vacia | `publicarExamen_conTablaVacia_rechazaPublicacion` | Rechaza respuesta modelo incompleta |
| `deleteExam` | Examen borrador | `eliminarExamen_borrador_eliminaCorrectamente` | Elimina correctamente |
| `deleteExam` | Examen cerrado con entregas | `eliminarExamen_cerradoConEntregas_rechaza` | Rechaza eliminacion |

**[CAPTURA 6 - Rama negativa de publicacion]**  
Pegar aca la captura del test `publicarExamen_conTemaQueNoSumaDiez_rechazaPublicacion`.

> Imagen recomendada: la captura donde se ve que el tema suma 4 puntos y se espera excepcion con mensaje relacionado a `10 puntos`.

**[CAPTURA 7 - Rama positiva de publicacion]**  
Pegar aca la captura del test `publicarExamen_conCadaTemaEnDiez_cambiaEstadoAPublicado`.

> Imagen recomendada: la captura donde se ve que los temas suman 10 puntos y se valida `ExamStatus.PUBLICADO`.

### 7.2 Cobertura del avance parcial `ExamSubmissionService`

| Metodo | Rama o condicion interna | Test que lo cubre | Resultado esperado |
|---|---|---|---|
| `start` | Alumno inicia examen publicado | `iniciarExamenPublicado_creaEntregaEnProgresoConTemaAsignado` | Entrega `EN_PROGRESO` |
| `saveAnswers` | Entrega en progreso | `guardarRespuestas_deEntregaEnProgreso_actualizaTexto` | Respuesta actualizada |
| `saveAnswers` | Entrega finalizada | `guardarRespuestas_deEntregaFinalizada_rechazaCambio` | Rechaza modificacion |
| `submit` | Entrega en progreso | `entregarExamen_cambiaEstadoAEntregado` | Estado `ENTREGADO` |

**[CAPTURA 8 - Tests de ExamSubmissionService]**  
Pegar aca la captura donde se ven los tests de inicio, guardado, rechazo de cambio y entrega.

> Imagen recomendada: la captura completa de `ExamSubmissionServiceTest.java`.

## 8. Flujo de decision analizado: publicacion de examen

El metodo `publish()` de `ExamService` no solo cambia el estado del examen. Antes de publicar, llama a `validateReadyToPublish(exam)`, donde se validan las condiciones internas necesarias.

Flujo analizado:

```text
Inicio
  |
  v
Buscar examen y validar propietario
  |
  v
Tiene al menos un tema?
  |-- No --> Error
  |
  v
Cada tema tiene preguntas?
  |-- No --> Error
  |
  v
Cada tema suma exactamente 10 puntos?
  |-- No --> Error
  |
  v
Cada pregunta tiene enunciado?
  |-- No --> Error
  |
  v
Cada pregunta tiene respuesta modelo?
  |-- No --> Error
  |
  v
Publicar examen
```

**[CAPTURA 9 - Codigo productivo validateReadyToPublish]**  
Pegar aca la captura del metodo `validateReadyToPublish` en `ExamService.java`.

> Imagen recomendada: la captura donde se ven las validaciones de temas, preguntas, suma de 10 puntos, enunciado y respuesta modelo.

## 9. Resultado completo de ejecucion

La suite completa incluida en esta entrega se ejecuto con:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

Resultado por clase:

| Test set | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `AuthServiceTest` | 15 | 0 | 0 | 0 |
| `ExamServiceTest` | 13 | 0 | 0 | 0 |
| `ExamSubmissionServiceTest` | 4 | 0 | 0 | 0 |
| `PasswordPolicyServiceTest` | 5 | 0 | 0 | 0 |
| `UserServiceTest` | 17 | 0 | 0 | 0 |
| `ApplicationTests` | 1 | 0 | 0 | 0 |
| **Total** | **55** | **0** | **0** | **0** |

**[CAPTURA 10 - Salida completa o final de Maven]**  
Pegar aca, si se desea, otra captura de PowerShell donde se vea el final de la corrida completa.

> Esta captura puede ser la misma que la Captura 1. Si el informe queda muy largo, no repetirla.

## 10. Capturas opcionales o de respaldo

Las siguientes capturas pueden dejarse como anexo si se quiere mostrar mas evidencia, pero no son indispensables en el cuerpo principal:

- PowerShell al inicio de la ejecucion, donde se ve `.\mvnw.cmd test`.
- PowerShell durante la ejecucion de `ApplicationTests`.
- PowerShell con comandos usados para abrir reportes.
- Captura repetida del resumen `BUILD SUCCESS`.

**[CAPTURA OPCIONAL A - Inicio de ejecucion Maven]**  
Pegar aca solo si se quiere mostrar que se ejecuto desde el directorio correcto.

**[CAPTURA OPCIONAL B - Ejecucion intermedia de ApplicationTests]**  
Pegar aca solo si se quiere mostrar que tambien se cargo el contexto Spring.

## 11. Limitaciones y pendientes

| Pendiente | Impacto | Propuesta |
|---|---|---|
| No hay medicion automatica con JaCoCo | No se informa porcentaje de cobertura de lineas o ramas | Agregar JaCoCo en una proxima iteracion |
| No hay tests de frontend con Jest/React Testing Library | La UI queda validada manualmente o por inspeccion estatica | Agregar tests de componentes |
| `KnownBugWhiteBoxChecks` corre separado | Documenta bugs conocidos, pero no forma parte de la suite verde | Mantenerlo como suite complementaria |

## 12. Conclusion

La entrega demuestra avance concreto de pruebas de caja blanca sobre el backend de 7test. La suite principal cubre la logica critica de Sprint 2 asociada a la gestion y publicacion de examenes por profesor. Ademas, se incluye un avance parcial del flujo posterior de entregas.

La ejecucion finaliza correctamente con 55 tests ejecutados, 0 fallos y 0 errores. Las evidencias anexadas muestran tanto el resultado de ejecucion como la relacion entre codigo productivo, ramas internas y pruebas unitarias.
