# Plan de avance del TP - 7test

Fecha de preparacion: 31/05/2026  
Base revisada: requerimiento del cliente/docente, historias PM Milestone 3 v2, feedback QA/DEV, Figma tentativo y estado real del repositorio.

## 1. Lectura ejecutiva

El TP ya tiene una base bastante mas avanzada que una primera version: login por rol, administracion de usuarios, politicas de contrasena, creacion de examenes, temas, preguntas, publicacion, cierre, resolucion por alumno, guardado automatico, entrega final y panel simple de entregas para profesor.

El siguiente avance no deberia consistir en copiar el Figma pantalla por pantalla ni en intentar cumplir todas las historias al 100% si eso no responde al cliente. El cliente/docente pidio una plataforma para crear, administrar y evaluar el primer parcial de Testing de Aplicaciones en UADE. Tambien marco dos reglas fuertes:

- Los examenes los crea el docente, no una IA.
- La plataforma no tiene la palabra final en la nota; la palabra final la tiene el docente.

Por eso, el criterio de avance recomendado es:

- Cerrar bien la rendicion de punta a punta.
- Ajustar las historias PM a lo que realmente aporta al MVP.
- No prometer tiempo real estricto, offline robusto, WebSockets, sala de espera compleja ni asignacion formal por curso si no estan modelados.
- Priorizar despues correccion/notas, porque "evaluar" es parte central del problema original del cliente.
- Documentar explicitamente que Figma es referencia visual tentativa, no especificacion obligatoria.

## 2. Fuentes contrastadas

### 2.1. Documento del cliente / clase

Puntos clave extraidos:

- El proceso tiene que ser iterativo: no tiene que salir perfecto, hay que empezar y mejorar.
- Dev, PM/PO y QA deben trabajar al mismo nivel, con la solucion en el centro.
- No hay que adornar lo que no se hizo: en la presentacion conviene mostrar proceso, solucion y pendientes.
- El problema de negocio es que corregir parciales manuscritos es arcaico y consume tiempo.
- El MVP arranca con el primer parcial de Testing de Aplicaciones en UADE.
- El flujo manual actual incluye disenar examen, imprimir, repartir, retirar, corregir en casa, devolver para revision, cargar notas y archivar.
- El valor del producto esta en crear, administrar y evaluar examenes, mejorando la experiencia del estudiante y de la universidad.

### 2.2. Historias PM Milestone 3 v2

El documento PM ya corrigio varias cosas razonables:

- Deja fuera WebSockets/SSE.
- Deja fuera monitoreo instantaneo en tiempo real.
- Deja fuera sala de espera separada.
- Deja fuera offline robusto con IndexedDB.
- Deja fuera editor generico de diagramas.
- Deja fuera estado persistido `EXCEDIDO_DE_TIEMPO`.
- Deja fuera correccion/notas para esta milestone.
- Reemplaza asignacion automatica de tema por seleccion manual del alumno.

El punto delicado es que la Definition of Done del PDF dice que todas las historias se completan cuando todos sus criterios estan cumplidos. Para este TP conviene negociar esa DoD: algunas historias estan bien como objetivo, pero no todas merecen 100% de implementacion si contradicen el alcance realista o el pedido original del cliente.

### 2.3. Feedback QA/DEV

Feedback accionable detectado:

- Al crear examen, hoy no aparece un Tema A por default: hay que tocar "Agregar tema".
- QA pidio timer visible por tema/alumno.
- El arbol de decisiones puede exceder el encuadre visual.
- Se pidio poder borrar temas una vez creados.
- Los colores de temas se repiten despues del tercer tema.
- Se propuso separar usuarios de prueba por grupo para evitar que se pisen.
- Se propuso restringir dominios a `uade.edu.ar`.
- Se cuestiono mostrar cantidad de temas al estudiante.
- Hay bugs intencionales de seguridad/UX para QA; no todos deben corregirse.

Contraste con el codigo actual:

- Borrar temas ya existe en backend y frontend para examenes en borrador.
- Los colores efectivamente rotan sobre una paleta corta de tres colores.
- El arbol/tabla tienen contenedores con scroll, pero conviene validar visualmente porque QA reporto desborde.
- La app no tiene timer.
- La app no tiene seleccion manual de tema.
- La cantidad de temas se muestra en el panel del alumno.
- No hay restriccion de dominio `uade.edu.ar`.

### 2.4. Figma / imagen

El Figma muestra intenciones utiles:

- Pantallas diferenciadas por rol.
- Seleccion de tema con botones grandes.
- Estados visuales y botones de accion claros.
- Flujo alumno/profesor separado.
- Uso de modales o confirmaciones.

Pero debe tratarse como guia tentativa. No hace falta que la UI quede identica. Lo importante para el TP es que el flujo sea testeable, defendible y consistente con la app existente.

## 3. Estado real del sistema hoy

### 3.1. Ya implementado y defendible

- Login con JWT y redireccion por rol.
- Rutas privadas con validacion del token contra `/auth/me`.
- Panel administrador para usuarios, busqueda, alta, edicion, activacion/desactivacion y politica de contrasenas.
- Panel profesor para crear examenes en borrador.
- Carga de materia/descripcion/duracion.
- Agregado, renombrado y eliminacion de temas en borrador.
- Agregado, edicion y eliminacion de preguntas.
- Plantilla base con teoricas, tabla de decision y arbol de decision.
- Validacion de publicacion: cada tema debe sumar 10 puntos y tener contenido.
- Publicar examen.
- Cerrar examen.
- Panel alumno con examenes publicados.
- Inicio de entrega por alumno.
- Guardado automatico online con debounce.
- Guardado manual.
- Entrega final.
- Bloqueo de edicion post-entrega.
- Panel profesor con entregas creadas para un examen.
- Vista director/admin de supervision por estado.
- Tests unitarios backend para usuarios, auth, politicas, examenes y entregas.

### 3.2. Implementado parcialmente

- Duracion del examen: existe en backend y en creacion del profesor, pero no esta usada para timer ni se edita claramente en el formulario de datos del borrador.
- Fecha/hora de referencia: existe en backend como `availableFrom`, pero no hay input visible en frontend.
- Panel profesor de entregas: existe refresh manual y lista entregas, pero faltan contadores, tiempo restante y etiqueta derivada de excedido.
- Vista historica del alumno: puede abrir entregas propias, pero el dashboard esta pensado principalmente como lista de examenes publicados.
- Editores de tabla/arbol: existen, pero conviene revisar encuadre responsive.
- Confirmaciones: hay confirmacion para borrar tema/examen, pero faltan confirmaciones claras para publicar/cerrar/entregar segun HU.

### 3.3. No implementado

- Seleccion manual de tema por alumno antes de iniciar.
- Cambio de tema antes de confirmar inicio.
- Endpoint de inicio que reciba `topicId`.
- Timer informativo en alumno.
- Timer/tiempo restante en profesor.
- Etiqueta derivada "Excedido de tiempo".
- Ordenamiento formal del dashboard alumno por disponible/en progreso/historico.
- Mostrar examenes cerrados al alumno de manera completa.
- Contadores por estado en panel profesor.
- Polling automatico opcional.
- Restriccion de email a dominio UADE.
- Correccion de entregas, notas, publicacion de notas.
- Sugerencia/mock de IA para correccion.
- Cursos, inscripciones o asignacion formal de examenes.

### 3.4. Problemas a no confundir

Hay bugs intencionales para QA, por ejemplo auth bypass, contraste de modo oscuro y textos mezclados. Esos no deben corregirse si siguen siendo parte del acuerdo pedagogico del TP.

Tambien hay problemas que no parecen intencionales y conviene corregir:

- El panel profesor parece invertir el label de estado de entrega: si la entrega no es `ENTREGADO`, muestra "Entregado"; si es `ENTREGADO`, muestra "En progreso".
- La asignacion automatica de tema usa `hashCode() % cantidadTemas`; con hash negativo podria devolver indice negativo. Si se implementa seleccion manual, este problema desaparece.
- La duracion esta marcada como positiva si viene, pero no estrictamente obligatoria en backend.
- El README y algunos docs viejos tienen credenciales/URLs desactualizadas frente a `CONTEXTO_PROYECTO.md`.

## 4. Criterio recomendado para avanzar

### 4.1. Lo que si deberia entrar en Milestone 3

Debe entrar porque cierra la rendicion de punta a punta o corrige una contradiccion fuerte con las historias PM v2:

- Seleccion manual de tema por alumno.
- Bloqueo de cambio de tema despues de iniciar entrega.
- Dashboard alumno mas claro, con estados visibles.
- Timer informativo en alumno.
- Publicar/Iniciar y cerrar con confirmacion.
- Panel profesor con estados correctos, refresh manual y contadores.
- Confirmacion antes de entregar.
- Ajuste de tests backend para la nueva regla de seleccion manual.
- Documentacion de alcance y limitaciones.

### 4.2. Lo que puede quedar parcial sin romper el MVP

Puede quedar parcial si se explica bien:

- Polling automatico del panel profesor.
- Vista de examenes cerrados sin asignacion por curso.
- Indicador respondida/no respondida por pregunta.
- Navegacion anterior/siguiente entre preguntas.
- Copia local en navegador ante caida temporal.
- Colores unicos para mas de tres temas.
- Fecha/hora como disponibilidad real bloqueante.
- "No iniciado" en panel profesor, porque no existe un universo de alumnos esperados.

### 4.3. Lo que deberia ir a milestone posterior

Conviene postergar:

- Cursos, comisiones, inscripciones y asignacion formal.
- WebSockets/SSE.
- Sala de espera independiente.
- Offline robusto con IndexedDB.
- Editor generico de diagramas.
- Estado persistido `EXCEDIDO_DE_TIEMPO`.
- Correccion manual con rubrica.
- Sugerencia IA/mock de correccion.
- Publicacion de notas y vista de notas del alumno.
- Flujo de aprobacion del director de catedra.
- Persistencia productiva con PostgreSQL/Supabase.

## 5. Matriz por historia PM Milestone 3

| Historia | Estado real | Recomendacion | Debe estar 100% |
|---|---|---|---|
| HU-28 Dashboard alumno | Parcial. Lista publicados y entregas propias, pero falta orden/estados/historico robusto. | Ajustar UI: estados `Disponible`, `En progreso`, `Entregado`; ordenar; no mostrar cantidad de temas si se considera innecesario. | No. Aceptar version MVP sin asignacion por curso. |
| HU-29 Seleccion manual de tema | No implementada. Hoy el backend asigna tema automaticamente. | Prioridad maxima. Cambiar backend y frontend para iniciar con `topicId`. | Si, es la brecha principal. |
| HU-30 Cambio de tema antes del inicio | No implementada como flujo, pero sale naturalmente si se agrega seleccion previa. | Permitir cambiar seleccion en UI hasta confirmar inicio. Luego bloquear. | Si, pero acotada a pre-inicio. |
| HU-31 Indicador disponibilidad | Parcial. Publicado/cerrado existen, pero alumno solo ve publicados. | Mostrar disponibilidad en pantalla de seleccion. Refresh manual suficiente. | Parcial razonable. |
| HU-32 Fecha y duracion | Parcial. Duracion existe; fecha existe en backend pero no UI. | Hacer duracion obligatoria en UI/API; agregar fecha/hora opcional visible. | Duracion si; fecha puede quedar opcional. |
| HU-33 Publicar/Iniciar y cerrar | Mayormente hecho. Falta confirmacion y wording. | Usar `Publicar/Iniciar`; confirmar publicar y cerrar. No crear estado nuevo. | Casi si, porque ya esta cerca. |
| HU-34 Panel de entregas | Parcial. Hay lista y refresh manual, pero falta corregir estado, contadores, timer/excedido. | Corregir label invertido, agregar contadores y etiqueta derivada si hay timer. | No al 100%; sin `No iniciado` real. |
| HU-35 Timer informativo | No implementado. | Implementar en cliente con `publishedAt + durationMinutes`. No bloquear entrega. | Si para M3 defendible. |
| HU-36 Texto libre | Mayormente hecho. | Mantener. Agregar confirmacion/estado visual; opcionales pueden quedar afuera. | Si en lo obligatorio. |
| HU-37 Arbol/tabla | Mayormente hecho. | Revisar overflow, modo lectura y autosave. No hacer editor generico. | Si en lo obligatorio, con QA visual. |
| HU-38 Envio examen | Mayormente hecho. | Agregar confirmacion, asegurar guardado previo, corregir panel profesor. | Si en lo obligatorio. |

## 6. Backlog priorizado

### P0 - Imprescindible para defender Milestone 3

1. Corregir estado de entregas en panel profesor.
   - Hoy el label parece invertido.
   - Resultado esperado: `EN_PROGRESO` se ve como "En progreso"; `ENTREGADO` se ve como "Entregado".

2. Implementar seleccion manual de tema.
   - Backend: `start` debe recibir `topicId`.
   - Validar que el tema exista y pertenezca al examen publicado.
   - Si el alumno ya tiene entrega, devolver la entrega existente y no permitir cambiar tema.
   - Frontend: pantalla previa con botones grandes por tema.
   - No mostrar preguntas antes de confirmar inicio.

3. Actualizar tests de entrega.
   - Test de inicio con tema elegido.
   - Test de rechazo por tema inexistente.
   - Test de que una entrega existente mantiene su tema.
   - Test de que no se puede iniciar examen no publicado.

4. Agregar confirmacion antes de entregar.
   - Mensaje claro: despues de entregar no se podran modificar respuestas.
   - Antes de entregar, ejecutar guardado pendiente.

5. Agregar timer informativo en vista alumno.
   - Calculo: `publishedAt + durationMinutes`.
   - Mostrar tiempo restante durante resolucion.
   - Si llega a cero, mostrar aviso visual.
   - No bloquear escritura ni entrega.

6. Mejorar dashboard alumno.
   - Mostrar estado visible por examen/entrega.
   - Ordenar: en progreso/disponibles primero, entregados despues.
   - Cambiar "Iniciar" por flujo de seleccion de tema.
   - Evaluar no mostrar cantidad de temas al alumno.

7. Agregar confirmacion a publicar/cerrar.
   - Publicar/Iniciar: explicar que no se podra volver a borrador.
   - Cerrar: explicar que impide nuevas entregas.

8. Actualizar documentacion.
   - Release note Milestone 3.
   - Guia QA para nuevo flujo.
   - Contexto del proyecto.
   - Aclarar alcance no incluido.

### P1 - Importante, pero negociable

1. Panel profesor con contadores.
   - Total con entrega creada.
   - En progreso.
   - Entregadas.
   - Excedidas de tiempo derivadas, si aplica.

2. Tiempo restante en panel profesor.
   - Mismo calculo que alumno.
   - Mostrar como informacion del examen, no por alumno.

3. Polling automatico opcional.
   - Cada 10 o 15 segundos solo cuando el panel de entregas esta visible.
   - Mantener boton "Actualizar" como mecanismo obligatorio.

4. Fecha/hora opcional en UI de profesor.
   - Agregar input `datetime-local`.
   - Mostrar al alumno si existe.
   - No usar como bloqueo fuerte en MVP.

5. Duracion obligatoria.
   - En frontend ya hay default 120.
   - En backend conviene agregar `@NotNull` o validacion de servicio si PM lo exige.

6. Revisar overflow del arbol de decision.
   - Probar con viewport chico.
   - Asegurar scroll interno y que no rompa el layout.

7. Crear usuarios de prueba por grupo.
   - Documentarlo para QA.
   - Evita que varios grupos se pisen en H2/Render.

### P2 - Siguiente milestone por valor de negocio

1. Correccion manual de entregas.
   - Profesor ve respuestas de cada alumno.
   - Asigna puntaje por pregunta.
   - Estado: pendiente de corregir, corregido, nota publicada.

2. Sugerencia IA mock.
   - No IA real.
   - Comparar respuesta del alumno contra respuesta modelo y sugerir 0 / 0.25 / 0.5 / 0.75 / 1 del puntaje.
   - El docente siempre puede modificar.

3. Publicacion de notas.
   - Profesor publica nota final.
   - Alumno ve nota solo cuando fue publicada.

4. Reporte/exportacion.
   - Lista de alumnos, estado, nota, fecha de entrega.
   - Export simple CSV puede alcanzar.

### P3 - Escalabilidad / producto completo

1. Cursos, comisiones e inscripciones.
2. Asignacion de examenes por curso o alumno.
3. Estado "No iniciado" real, basado en alumnos esperados.
4. PostgreSQL persistente en produccion.
5. Recuperacion de contrasena por email real.
6. Auditoria de acciones.
7. Soporte multi-materia, multi-universidad y eventualmente multi-idioma.
8. Accesibilidad y revision UX completa.

## 7. Plan tecnico por area

### 7.1. Backend

Cambios recomendados:

- Crear request `StartExamRequest` con `topicId`.
- Cambiar `ExamSubmissionUseCase.start(studentEmail, examId)` a `start(studentEmail, examId, topicId)`.
- En `ExamSubmissionService` reemplazar `chooseTopicForStudent` por `requireSelectedTopic(exam, topicId)`.
- Validar:
  - Usuario tiene rol `ALUMNO`.
  - Examen existe.
  - Examen esta `PUBLICADO`.
  - Tema existe en el examen.
  - Si ya hay entrega para alumno/examen, se devuelve la existente sin cambiar el tema.
- Mantener estados simples: `EN_PROGRESO` y `ENTREGADO`.
- No agregar `EXCEDIDO_DE_TIEMPO` persistido.
- Si se decide hacer duracion obligatoria, validar en DTO y service.
- Si se agrega fecha/hora en frontend, ya existe `availableFrom`; usarlo solo como dato informativo.

Tests backend:

- `start_conTemaValido_creaEntregaConEseTema`.
- `start_sinTemaOConTemaInvalido_rechaza`.
- `start_conEntregaExistente_noCambiaTema`.
- `saveAnswers_rechazaPreguntaDeOtroTema`.
- `submit_idempotente_o_rechaza_segun_decision`.
- `publish_seteaPublishedAt`.
- `close_noPermiteNuevasEntregas` si se cambia la regla para entregas cerradas.

### 7.2. Frontend alumno

Cambios recomendados:

- Separar estados de pantalla:
  - Dashboard/listado.
  - Seleccion de tema.
  - Resolucion de entrega.
  - Modo lectura post-entrega.
- En dashboard:
  - Mostrar titulo, materia, duracion y estado.
  - Quitar o bajar prioridad a cantidad de temas.
  - Boton "Seleccionar tema" en vez de "Iniciar" si no hay entrega.
  - Boton "Continuar" si hay entrega en progreso.
  - Boton "Ver entrega" si entregado.
- En seleccion:
  - Mostrar botones grandes por tema.
  - No mostrar preguntas.
  - Permitir cambiar seleccion antes de comenzar.
  - Boton "Comenzar examen" deshabilitado hasta seleccionar tema.
  - Enviar `topicId` al backend al confirmar.
- En resolucion:
  - Timer visible en header.
  - Badge de estado.
  - Guardado automatico y manual como hoy.
  - Confirmacion antes de entregar.
  - Si timer llega a cero, aviso visual no bloqueante.
- En modo lectura:
  - Inputs y editores readonly.
  - Mensaje de entrega finalizada.

### 7.3. Frontend profesor

Cambios recomendados:

- Corregir label/estilo de estado en entregas.
- Agregar contadores arriba del panel.
- Mantener boton "Actualizar".
- Opcional: polling cada 10/15 segundos.
- Mostrar timer/tiempo restante si el examen esta publicado y tiene duracion.
- Mostrar "Excedido de tiempo" como etiqueta derivada:
  - Si `now > publishedAt + durationMinutes`.
  - Solo para entregas `EN_PROGRESO`.
  - No modificar backend.
- Agregar confirmacion antes de publicar/iniciar.
- Agregar confirmacion antes de cerrar.
- Agregar input de fecha/hora opcional si hay tiempo.
- Considerar crear Tema A automaticamente al crear examen o, minimo, mantener el flujo actual pero documentarlo.

### 7.4. Frontend director/admin

Para M3 alcanza con supervision simple por estado. No conviene invertir mucho aca hasta tener correccion/notas.

Mejoras posibles:

- Mostrar fecha de publicacion.
- Mostrar cantidad de entregas cuando el backend lo soporte.
- Mostrar filtro por profesor o materia si crece el volumen.

### 7.5. QA y documentacion

Actualizar o crear:

- Release note v1.3 / Milestone 3.
- Guia QA Milestone 3.
- Casos de prueba:
  - Login por rol.
  - Crear examen.
  - Agregar temas/preguntas.
  - Publicar.
  - Seleccionar tema.
  - Cambiar tema antes de empezar.
  - Iniciar entrega.
  - Guardar respuestas.
  - Responder tabla/arbol.
  - Entregar.
  - Ver entrega readonly.
  - Ver entrega en panel profesor.
  - Cerrar examen.
  - Intentar iniciar cerrado.
  - Timer informativo.
- Bugs esperados/no esperados:
  - Separar bugs intencionales de bugs a corregir.

## 8. Decision recomendada por punto conflictivo

### 8.1. Figma

Decision: usar como referencia visual.  
No bloquear desarrollo por pixel perfect.

### 8.2. Seleccion manual vs asignacion automatica

Decision: implementar seleccion manual.  
Motivo: PM v2 lo marca como cambio explicito de HU-24 y responde a una practica plausible de aula.

### 8.3. Sala de espera

Decision: no crear pantalla separada.  
Motivo: no aporta proporcionalmente al MVP. La pantalla de seleccion puede mostrar disponibilidad.

### 8.4. Tiempo real

Decision: refresh manual obligatorio; polling opcional.  
Motivo: WebSockets/SSE estan fuera de scope y no son necesarios para demo.

### 8.5. Timer

Decision: implementar timer informativo en cliente.  
Motivo: es visible, testeable y no obliga a cambiar estados backend.

### 8.6. "No iniciado"

Decision: no prometerlo para MVP.  
Motivo: sin cursos/inscripciones/asignacion no hay universo cerrado de alumnos esperados.

### 8.7. Examen cerrado para alumno

Decision: impedir nuevas entregas.  
Si un alumno ya inicio antes del cierre, definir explicitamente si puede entregar. Recomendacion MVP: permitir entregar lo ya iniciado, porque cerrar se usa para impedir nuevos inicios, no para perder trabajo ya cargado.

### 8.8. Offline

Decision: mantener guardado online.  
Opcional: `localStorage` simple para borrador local, pero no como DoD.

### 8.9. Restriccion `uade.edu.ar`

Decision: postergar o aplicar solo a usuarios reales, no a seeds/dev.  
Motivo: puede complicar QA con correos de prueba. Si se implementa, debe estar documentado.

### 8.10. Correccion y notas

Decision: siguiente milestone prioritaria.  
Motivo: el cliente pidio crear, administrar y evaluar. Sin correccion/notas, el producto todavia no cierra el ciclo de negocio.

## 9. Lista de chequeo de implementacion

### Preparacion

- Crear branch de trabajo para Milestone 3.
- Ejecutar tests actuales y dejar evidencia.
- Revisar que bugs intencionales sigan documentados.
- Definir si se va a tocar o no dominio `uade.edu.ar`.

### Backend

- Agregar request con `topicId` para iniciar entrega.
- Cambiar use case y controller de inicio.
- Validar tema seleccionado.
- Eliminar dependencia de asignacion por hash.
- Agregar tests nuevos.
- Ajustar Swagger/OpenAPI automaticamente via DTO.

### Frontend alumno

- Agregar estado `selectedExamForStart`.
- Agregar estado `selectedTopicId`.
- Crear pantalla de seleccion de tema.
- Enviar `{ topicId }` al iniciar.
- Ocultar preguntas antes de inicio.
- Agregar timer.
- Agregar confirmacion de entrega.
- Ajustar dashboard y estados.

### Frontend profesor

- Corregir ternario del estado de entregas.
- Agregar contadores.
- Agregar confirmacion publicar/cerrar.
- Mostrar tiempo restante.
- Agregar etiqueta excedido derivada.
- Revisar overflow de arbol/tabla.

### Tests y QA

- Ejecutar `mvnw.cmd test`.
- Ejecutar `npm run build`.
- Probar flujo local completo:
  - Admin crea profesor/alumno.
  - Profesor crea examen con dos temas.
  - Publica.
  - Alumno selecciona Tema B.
  - Alumno responde y entrega.
  - Profesor ve Tema B y estado correcto.
  - Profesor cierra.
  - Otro alumno no puede iniciar cerrado.
- Guardar evidencias si el TP las requiere.

### Documentacion

- Actualizar `CONTEXTO_PROYECTO.md`.
- Crear `RELEASE_NOTE_v1.3.md`.
- Actualizar guia QA.
- Agregar nota de alcance:
  - Figma tentativo.
  - Sin WebSockets.
  - Sin offline robusto.
  - Sin cursos/inscripciones.
  - Sin correccion/notas en M3, pero priorizado para siguiente milestone.

## 10. Propuesta de defensa oral

Mensaje recomendado:

"Tomamos las historias PM v2 y las contrastamos contra el pedido original del cliente y contra el estado real del producto. Como el objetivo del MVP es digitalizar el parcial de punta a punta, priorizamos que el alumno pueda elegir tema, rendir, guardar y entregar; y que el profesor pueda publicar, cerrar y monitorear entregas. Dejamos fuera WebSockets, sala de espera separada, offline robusto y editor generico porque no eran necesarios para validar el valor principal. Tambien dejamos documentado que la correccion y publicacion de notas son la siguiente prioridad, porque ahi se completa la parte de evaluar que pidio el cliente."

## 11. Resultado esperado despues de aplicar este plan

Al terminar P0, el TP deberia poder mostrarse asi:

1. Admin crea usuarios de prueba.
2. Profesor crea examen, temas y preguntas.
3. Profesor publica/inicia el examen.
4. Alumno ve examen disponible.
5. Alumno selecciona manualmente el tema.
6. Alumno inicia entrega.
7. Alumno ve timer informativo.
8. Alumno responde texto, tabla y arbol.
9. Sistema guarda automatico.
10. Alumno entrega con confirmacion.
11. Profesor ve entrega, tema y estado correcto.
12. Profesor cierra examen.
13. Se explican pendientes sin sobreactuar: correccion/notas, cursos, offline robusto y tiempo real.

Con eso, la milestone queda defendible, testeable y alineada con el cliente.
