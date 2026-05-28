# 7test - Estrategia y avance de pruebas de caja blanca - Sprint 2

**Fecha de preparacion:** 28/05/2026  
**Version evaluada:** Sprint 2, con avance parcial de Sprint 3  
**Tipo de pruebas:** unitarias de caja blanca sobre backend  
**Stack:** Java 21, Spring Boot, JUnit 5, Mockito, AssertJ  

## 1. Objetivo

El objetivo de esta entrega es documentar la estrategia y el avance de las pruebas de caja blanca correspondientes al Sprint 2. La base metodologica es la misma declarada desde Sprint 1: pruebas unitarias con JUnit 5 + Mockito, aislando la unidad bajo prueba mediante mocks y validando ramas internas, precondiciones, estados y casos borde.

El alcance principal del Sprint 2 es la gestion de examenes por parte del profesor:

- Creacion de examenes en estado borrador.
- Carga de temas y preguntas.
- Validacion de puntaje total por tema.
- Publicacion de examenes.
- Bloqueo de edicion cuando el examen ya fue publicado.
- Cierre y eliminacion segun estado del examen.

Adicionalmente, se incluye en este mismo informe un avance parcial ya implementado sobre el flujo posterior previsto para Sprint 3: inicio de examen, guardado de respuestas y entrega por parte del alumno. Se presenta como avance complementario dentro de la misma suite de caja blanca, no como un informe separado.

## 2. Estrategia de caja blanca

La estrategia se basa en revisar el codigo fuente de los servicios y derivar casos desde las decisiones internas de cada metodo. No se prueba la UI ni la base de datos real en estos casos; se prueba la logica de dominio/aplicacion con dependencias reemplazadas por mocks.

### Criterios aplicados

| Criterio | Aplicacion |
|---|---|
| Aislamiento de unidad | Los repositorios se mockean con Mockito para probar solo el servicio. |
| Cobertura de ramas | Se cubren ramas exitosas y ramas con excepcion. |
| Estados del dominio | Se validan transiciones `BORRADOR -> PUBLICADO`, reglas para `CERRADO`, y avance parcial `EN_PROGRESO -> ENTREGADO`. |
| Casos borde | Puntajes que no suman 10, preguntas vacias, arbol/tabla sin contenido, entrega finalizada. |
| Reglas por rol | Se fuerza que solo profesores gestionen examenes y que solo alumnos rindan entregas. |
| Regresion | Se mantiene la suite previa de Sprint 1 para autenticacion, usuarios y politicas. |

### Herramientas

- `@ExtendWith(MockitoExtension.class)` para ejecutar tests unitarios sin levantar Spring.
- `@Mock` para reemplazar repositorios y puertos externos.
- `@InjectMocks` para inyectar los mocks en los servicios bajo prueba.
- AssertJ para verificar resultados y excepciones esperadas.
- `ApplicationTests` como smoke test de contexto Spring.

## 3. Alcance cubierto

### Servicios bajo prueba

| Servicio | Archivo de test | Relacion con la entrega |
|---|---|---|
| `ExamService` | `src/test/java/com/seventest/application/service/ExamServiceTest.java` | Alcance principal de Sprint 2: creacion, edicion, publicacion, cierre y eliminacion de examenes. |
| `ExamSubmissionService` | `src/test/java/com/seventest/application/service/ExamSubmissionServiceTest.java` | Avance parcial de Sprint 3 incluido en la misma suite: inicio, guardado y entrega de examen. |

### Suite de regresion incluida

| Servicio | Archivo de test | Motivo |
|---|---|---|
| `AuthService` | `AuthServiceTest.java` | Login, logout, recuperacion y bloqueo de cuenta. |
| `UserService` | `UserServiceTest.java` | Alta, edicion, activacion/desactivacion y filtros. |
| `PasswordPolicyService` | `PasswordPolicyServiceTest.java` | Reglas de contrasena y limites. |

## 4. Matriz de pruebas disenadas

### Sprint 2 - ExamService

| TC-ID | Metodo / rama | Condicion analizada | Resultado esperado | Estado |
|---|---|---|---|---|
| WB-S2-EX-01 | `create` | Profesor valido crea examen | Se guarda examen en `BORRADOR` con materia default | Implementado |
| WB-S2-EX-02 | `addTopic` | Examen en borrador agrega primer tema | Se asigna color `#1956D8` | Implementado |
| WB-S2-EX-03 | `publish` | Tema suma menos de 10 puntos | Rechaza publicacion con excepcion | Implementado |
| WB-S2-EX-04 | `publish` | Cada tema suma exactamente 10 | Cambia estado a `PUBLICADO` y setea `publishedAt` | Implementado |
| WB-S2-EX-05 | `addQuestion` | Examen ya publicado | Rechaza edicion porque no esta en borrador | Implementado |
| WB-S2-EX-06 | `addQuestion` | Pregunta vacia en borrador | Permite guardar borrador incompleto | Implementado |
| WB-S2-EX-07 | `addQuestion` | Tema supera 10 durante redistribucion | Permite cambio intermedio para luego ajustar | Implementado |
| WB-S2-EX-08 | `publish` | Pregunta sin enunciado | Rechaza publicacion | Implementado |
| WB-S2-EX-09 | `publish` | Arbol de decision vacio | Rechaza publicacion por respuesta modelo incompleta | Implementado |
| WB-S2-EX-10 | `publish` | Tabla de decision vacia | Rechaza publicacion por respuesta modelo incompleta | Implementado |
| WB-S2-EX-11 | `deleteExam` | Examen en borrador | Elimina correctamente | Implementado |
| WB-S2-EX-12 | `deleteExam` | Examen cerrado con entregas | Rechaza eliminacion | Implementado |
| WB-S2-EX-13 | `updateQuestion` | Tema queda arriba de 10 en edicion | Permite estado intermedio para redistribucion | Implementado |

### Avance parcial Sprint 3 - ExamSubmissionService

| TC-ID | Metodo / rama | Condicion analizada | Resultado esperado | Estado |
|---|---|---|---|---|
| WB-S3-SUB-01 | `start` | Alumno inicia examen publicado | Crea entrega `EN_PROGRESO` con respuestas vacias | Implementado |
| WB-S3-SUB-02 | `saveAnswers` | Entrega en progreso | Actualiza texto de respuesta | Implementado |
| WB-S3-SUB-03 | `saveAnswers` | Entrega ya finalizada | Rechaza modificacion | Implementado |
| WB-S3-SUB-04 | `submit` | Entrega en progreso | Cambia a `ENTREGADO` y setea `submittedAt` | Implementado |

## 5. Resultado de ejecucion

Comando utilizado para ejecutar la suite completa incluida en esta entrega:

```powershell
$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\jbr'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
.\mvnw.cmd test
```

Resultado:

| Test set | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `AuthServiceTest` | 15 | 0 | 0 | 0 |
| `ExamServiceTest` | 13 | 0 | 0 | 0 |
| `ExamSubmissionServiceTest` | 4 | 0 | 0 | 0 |
| `PasswordPolicyServiceTest` | 5 | 0 | 0 | 0 |
| `UserServiceTest` | 17 | 0 | 0 | 0 |
| `ApplicationTests` | 1 | 0 | 0 | 0 |
| **Total** | **55** | **0** | **0** | **0** |

Estado general: **BUILD SUCCESS**.

Evidencia local generada por Maven Surefire:

```text
target/surefire-reports/
```

## 6. Avance respecto de Sprint 1

En Sprint 1 la caja blanca se concentro en autenticacion, usuarios y politicas de contrasena. Para esta entrega se avanzo sobre la logica principal incorporada en Sprint 2 y se dejo cubierto un primer bloque del flujo posterior.

| Area | Sprint 1 | Entrega actual |
|---|---:|---:|
| Autenticacion / usuarios / politicas | Cubierto | Regresion mantenida |
| Creacion de examenes en borrador | No incluido | Cubierto |
| Alta de temas y preguntas | No incluido | Cubierto |
| Validacion de 10 puntos por tema | No incluido | Cubierto |
| Publicacion de examenes | No incluido | Cubierto |
| Bloqueo de edicion post-publicacion | No incluido | Cubierto |
| Inicio y entrega de examen | No incluido | Avance parcial cubierto |

## 7. Riesgos detectados y pendientes

| Riesgo / pendiente | Impacto | Propuesta |
|---|---|---|
| No hay medicion automatica de cobertura con JaCoCo | No se puede informar porcentaje de lineas/ramas con evidencia numerica | Agregar plugin JaCoCo y umbral minimo por paquete. |
| No hay tests unitarios de frontend | La logica visual del panel de profesor queda validada solo manualmente | Agregar React Testing Library para flujos principales. |
| `ApplicationTests` levanta contexto con H2 file | La prueba es mas lenta y puede depender del entorno | Configurar perfil `test` con H2 en memoria aislado. |
| Suite de bugs conocidos corre separada | Es util como evidencia, pero no forma parte de la suite verde | Mantenerla documentada como suite de deteccion/regresion negativa. |

## 8. Conclusion

Este informe unifica la estrategia y el avance de caja blanca de la entrega actual. El foco principal corresponde a Sprint 2: gestion de examenes por profesor, validaciones previas a la publicacion y restricciones de edicion segun estado. Dentro de la misma suite se incluye tambien un avance parcial del flujo posterior previsto para Sprint 3, cubriendo inicio, guardado y entrega de examenes.

La suite ejecuta 55 pruebas automatizadas con JUnit 5 + Mockito: 13 directamente asociadas a `ExamService`, 4 al avance de `ExamSubmissionService`, 37 de regresion de Sprint 1 y 1 smoke test de contexto Spring. La estrategia sigue el criterio definido desde Sprint 1: pruebas unitarias aisladas, derivadas de la estructura interna del codigo, con foco en ramas, estados y validaciones de negocio.
