# Revision DEV - Historias de Usuario Milestone 3

Fecha: 2026-05-29  
Equipo: Desarrollo  
Proyecto: 7test - Testing de Aplicaciones - UADE

Este documento revisa las historias enviadas por PM para Milestone 3 y propone una version corregida, tomando como base:

- Lo pedido originalmente por el docente/cliente en clase.
- El alcance realista de un MVP universitario con tiempo acotado.
- El estado actual de la aplicacion segun `CONTEXTO_PROYECTO.md` y `RELEASE_NOTE_v1.2.md`.
- La necesidad de que PM, DEV y QA trabajen al mismo nivel, con la solucion en el centro.

## 1. Posicion DEV

No rechazamos la Milestone 3 en bloque. Varias historias apuntan a problemas reales del flujo de rendicion. El problema es que el documento mezcla necesidades de negocio con soluciones tecnicas costosas, requisitos ambiguos y funcionalidades que ya existen parcialmente.

El docente pidio una plataforma para crear, administrar y evaluar el primer parcial de Testing de Aplicaciones en UADE, con dos reglas de negocio no negociables:

- Los examenes los crea el docente, no una IA.
- La plataforma no tiene la palabra final en la nota; la palabra final la tiene el docente.

Tambien remarco que el ciclo debe ser iterativo: no tiene que salir perfecto, hay que empezar y mejorar. Por eso, para este MVP no corresponde transformar la aplicacion en una plataforma de monitoreo online compleja si eso no resuelve directamente el problema principal del cliente.

## 2. Estado actual de la aplicacion

La aplicacion ya tiene implementado gran parte del flujo que PM esta pidiendo como si fuera nuevo:

- El profesor crea examenes en borrador, agrega temas y preguntas, publica y cierra examenes.
- El alumno ve examenes publicados, inicia un examen, responde preguntas, guarda respuestas y entrega.
- Hay guardado automatico con debounce durante la resolucion.
- Hay preguntas de texto libre y editores visuales para arbol/tabla de decision.
- El profesor puede ver las entregas asociadas a un examen.
- Los estados de entrega existentes son `EN_PROGRESO` y `ENTREGADO`.

Lo que no existe hoy o no deberia asumirse como existente:

- Asignacion formal de examenes a cursos/alumnos especificos.
- Sala de espera antes del inicio formal del profesor.
- WebSockets, SSE o comunicacion en tiempo real estricta.
- Timer sincronizado persistido como regla de negocio.
- Estado persistido `EXCEDIDO_DE_TIEMPO`.
- Guardado offline robusto con sincronizacion posterior.
- Correccion, notas y publicacion de notas.

## 3. Criterio de correccion de alcance

Para aceptar una historia en este MVP deberia cumplir estas condiciones:

- Responde a una necesidad expresada por el docente/cliente.
- Es testeable por QA con criterios claros.
- No duplica una funcionalidad ya implementada.
- No obliga a redisenar la arquitectura sin valor proporcional.
- No introduce reglas de negocio nuevas que el cliente no pidio.
- Puede demostrarse en clase sin depender de infraestructura fragil.

## 4. Resumen de decisiones

| Bloque | Decision DEV | Comentario |
|---|---|---|
| Dashboard alumno | Aceptar con ajustes | Ya existe listado de examenes publicados y entregas. Corregir wording y estados. |
| Seleccion manual de tema | Aceptar si reemplaza HU-24 | Es un cambio de regla de negocio. Debe quedar claro que no hay asignacion automatica. |
| Cambio de tema | Aceptar acotado | Permitido solo antes de confirmar/iniciar la entrega. |
| Sala de espera e indicador visual | Postergar o simplificar | No fue pedido originalmente como pieza central del MVP. |
| Fecha, hora y duracion | Aceptar acotado | Duracion es importante. Fecha/hora puede ser informativa o de disponibilidad simple. |
| Inicio formal del profesor | Ajustar | Usar `Publicar/Iniciar` como accion equivalente. No crear un flujo paralelo innecesario. |
| Monitoreo en tiempo real | Ajustar | Usar panel de entregas con refresh manual o polling. No WebSockets obligatorios. |
| Timer sincronizado | Aceptar como indicador | No bloquear respuestas ni convertirlo en control automatico fuerte. |
| Respuestas texto/tabla/diagrama | Aceptar | Ya esta mayormente implementado. Ajustar criterios a lo existente. |
| Envio del examen | Aceptar | Ya esta mayormente implementado. Agregar confirmacion si falta. |
| Offline autosave | Postergar | Es costoso y no fue pedido por el cliente. Puede quedar como mejora. |

## 5. Comentarios generales al documento PM

1. El termino "tiempo real" esta usado de forma exagerada. Si el objetivo es que el profesor vea si un alumno inicio, sigue en progreso o entrego, eso puede resolverse con el panel de entregas actual mas refresh manual o polling cada algunos segundos. No hace falta WebSocket/SSE para el MVP.

2. La historia dice "examenes asignados a mi", pero el sistema actual no modela asignaciones alumno-curso-examen. Hoy los alumnos ven examenes publicados. Si PM quiere asignacion real por curso o alumno, eso es otra funcionalidad y debe estar explicitada.

3. El estado "No iniciado" solo puede calcularse si existe una lista cerrada de alumnos esperados para ese examen. Sin inscripcion por curso o asignacion de alumnos, el sistema no puede saber quien "deberia" haber iniciado.

4. El estado "Excedido de tiempo" no existe hoy como estado persistido. Para MVP conviene tratarlo como etiqueta derivada: si el examen tiene hora de inicio y duracion, y el alumno sigue `EN_PROGRESO` despues del vencimiento, se muestra como "Excedido de tiempo" en la vista del profesor.

5. La exigencia de guardado automatico offline con sincronizacion posterior es una funcionalidad de alta complejidad. Para el MVP basta con guardado automatico online y, como mejora opcional, una copia local simple en `localStorage`.

6. Las historias HU-29/HU-30 y HU-36/HU-37 tienen criterios mezclados en el documento original. Antes de pasar a QA deberian separarse para que los casos de prueba sean claros.

7. Aunque PM deja correccion y notas fuera de scope, la evaluacion/correccion fue una parte central del problema original del docente. No conviene consumir toda la capacidad del equipo en monitoreo avanzado si despues no queda tiempo para la parte de evaluacion.

## 6. Version corregida por historia

### HU-28 - Dashboard de examenes del alumno

Decision: aceptar con ajustes.

Comentario DEV:
La intencion es correcta, pero no deberia hablar de "examenes asignados" si la aplicacion todavia no tiene asignacion por alumno o por curso. Para el MVP, el dashboard debe mostrar examenes publicados/disponibles y las entregas propias del alumno.

Version corregida:

Como alumno, quiero ver un dashboard con mis examenes disponibles y mis entregas, para acceder a examenes vigentes y consultar examenes ya entregados.

Criterios de aceptacion corregidos:

- El alumno accede al dashboard desde su panel.
- El dashboard muestra examenes publicados/disponibles y entregas propias del alumno.
- Cada examen muestra titulo, curso, duracion y estado.
- Si existe fecha configurada, tambien se muestra la fecha del examen.
- Los estados visibles para el alumno son: `Disponible`, `En progreso`, `Entregado` y `Cerrado`.
- Los examenes disponibles o en progreso aparecen primero.
- Los examenes entregados o cerrados aparecen despues como historico.
- Un examen entregado se abre en modo solo lectura.
- Un examen cerrado no puede iniciarse.

Fuera de esta historia:

- Asignacion especifica por alumno/curso, salvo que se cree una historia separada para modelar cursos e inscripciones.
- Vista de notas, porque PM la declaro fuera de scope y todavia no existe correccion.

### HU-29 - Seleccion de tema por el alumno

Decision: aceptar si reemplaza explicitamente la asignacion automatica de HU-24.

Comentario DEV:
Este cambio no es solo visual. Cambia una regla de negocio existente: hoy el sistema asigna tema de forma deterministica al iniciar el examen. Si ahora el alumno selecciona manualmente, hay que actualizar HU-24, backend, frontend y pruebas.

Version corregida:

Como alumno, quiero seleccionar manualmente el tema antes de ver las preguntas, para rendir el tema que me corresponde segun la indicacion del docente en el aula.

Criterios de aceptacion corregidos:

- Al acceder a un examen disponible, el alumno ve botones grandes para los temas creados por el profesor.
- Los temas se muestran con el nombre definido por el profesor, por ejemplo `Tema A`, `Tema B` o `Tema 1`.
- El alumno no ve las preguntas antes de seleccionar un tema y confirmar el inicio de su entrega.
- El alumno debe seleccionar un tema para poder comenzar.
- El tema seleccionado queda destacado visualmente.
- El mismo tema puede ser seleccionado por mas de un alumno.
- La seleccion se persiste cuando el alumno confirma el inicio de la entrega.

Comentario para Trello:

HU-24 debe quedar actualizada asi: el sistema permite crear/generar temas dentro del examen, pero no los asigna automaticamente al alumno. La asignacion ocurre cuando el alumno selecciona su tema manualmente.

### HU-30 - Cambio de tema antes del inicio

Decision: aceptar acotado.

Comentario DEV:
El cambio de tema es razonable, pero debe estar atado a un momento claro. Para no sobredisenar una sala de espera, el limite deberia ser la confirmacion de inicio de la entrega del alumno.

Version corregida:

Como alumno, quiero poder cambiar el tema seleccionado antes de confirmar el inicio de mi entrega, para corregir mi eleccion si me equivoque.

Criterios de aceptacion corregidos:

- Mientras el alumno no haya confirmado el inicio de su entrega, puede cambiar el tema seleccionado.
- Al seleccionar otro tema, el nuevo queda destacado y el anterior deja de estarlo.
- Una vez confirmada la entrega/inicio del examen, el tema queda bloqueado.
- Si el alumno ya tiene una entrega `EN_PROGRESO` o `ENTREGADO`, no puede cambiar de tema.

Nota:
Si PM insiste en que el bloqueo ocurra por "inicio formal del profesor", entonces primero debe aprobarse una historia separada para modelar ese estado. No conviene mezclarlo dentro de esta HU.

### HU-31 - Indicador visual de inicio del examen en sala de espera

Decision: postergar o simplificar.

Comentario DEV:
La sala de espera con actualizacion automatica no aparece como necesidad central del docente. Para el MVP, la disponibilidad del examen puede resolverse con el estado del examen: borrador no visible, publicado/disponible visible, cerrado no rendible.

Version corregida recomendada:

Como alumno, quiero ver claramente si un examen esta disponible, en progreso, entregado o cerrado, para saber si puedo rendirlo.

Criterios de aceptacion corregidos:

- El dashboard muestra el estado actual del examen.
- El boton para comenzar solo se habilita si el examen esta disponible y el alumno selecciono un tema.
- Si el examen esta cerrado, el alumno no puede comenzar una nueva entrega.
- Si el alumno ya entrego, solo puede ver su entrega en modo lectura.
- La actualizacion puede ocurrir al cargar la pagina, al usar un boton de actualizar o mediante polling simple.

Fuera de MVP:

- Sala de espera en vivo.
- Actualizacion instantanea sin recargar como requisito obligatorio.
- WebSockets o SSE.

### HU-32 - Configuracion de fecha, hora y duracion del examen

Decision: aceptar con ajustes.

Comentario DEV:
La duracion del examen si es relevante. La fecha y hora pueden ser utiles, pero no deberian bloquear demos ni pruebas si todavia no se implementa un sistema completo de agenda.

Version corregida:

Como profesor, quiero configurar fecha de referencia y duracion del examen, para que el alumno conozca las condiciones de rendicion y el sistema pueda mostrar un timer informativo.

Criterios de aceptacion corregidos:

- Desde el detalle del examen en borrador, el profesor puede configurar duracion en minutos.
- La duracion debe ser un numero entero positivo.
- Opcionalmente, el profesor puede configurar fecha y hora del examen.
- La fecha y hora se muestran al alumno si fueron configuradas.
- Mientras el examen este en borrador, la configuracion puede modificarse.
- Una vez publicado/iniciado el examen, la configuracion queda bloqueada.

Ajuste recomendado:
No exigir que la fecha sea futura como regla absoluta para el MVP, porque complica pruebas, demos y carga de examenes de ejemplo. Si se usa como disponibilidad real, entonces si debe validarse que no sea pasada.

### HU-33 - Inicio formal del examen por el profesor

Decision: ajustar.

Comentario DEV:
La aplicacion ya tiene `Publicar` y `Cerrar examen`. Crear ademas un "inicio formal" separado puede duplicar estados y agrandar el alcance. Para el MVP, `Publicar/Iniciar` deberia ser la accion que habilita el examen a los alumnos.

Version corregida:

Como profesor, quiero publicar/iniciar el examen cuando este listo, para que los alumnos puedan comenzar a rendirlo.

Criterios de aceptacion corregidos:

- El profesor dispone de un boton `Publicar` o `Iniciar examen` sobre examenes en borrador validos.
- El examen solo puede publicarse/iniciarse si tiene al menos un tema y preguntas validas.
- El sistema solicita confirmacion antes de publicar/iniciar.
- Al confirmar, el examen pasa a estar disponible para los alumnos.
- Se registra la fecha/hora de publicacion/inicio.
- Una vez publicado/iniciado, el profesor no puede volver el examen a borrador.
- El profesor puede cerrar el examen para impedir nuevas entregas.
- Si hay duracion configurada, el tiempo restante se calcula desde la fecha/hora de publicacion/inicio.

Fuera de MVP:

- Enviar una senal instantanea a todos los navegadores conectados.
- WebSockets/SSE obligatorios.
- Revertir inicio.

### HU-34 - Monitoreo del examen por el profesor

Decision: aceptar con ajustes fuertes.

Comentario DEV:
El objetivo aclarado por PM fue ver si el alumno abrio el examen, esta en progreso o entrego. Eso ya esta parcialmente implementado. No hace falta prometer "tiempo real" estricto.

Version corregida:

Como profesor, quiero ver el estado de las entregas de los alumnos de un examen, para saber quienes estan en progreso y quienes entregaron.

Criterios de aceptacion corregidos:

- En examenes publicados/iniciados o cerrados, el profesor accede al panel de entregas.
- El panel lista las entregas creadas para ese examen.
- Cada fila muestra nombre del alumno, tema seleccionado/asignado y estado.
- Los estados minimos son `En progreso` y `Entregado`.
- Si existe una lista cerrada de alumnos esperados para el examen, se puede mostrar tambien `No iniciado`.
- Si no existe asignacion de alumnos, `No iniciado` queda fuera de esta milestone.
- El profesor puede actualizar manualmente la lista.
- Opcionalmente, el sistema puede refrescar la lista automaticamente cada 10 o 15 segundos.
- Si hay duracion y hora de inicio, el panel puede mostrar una etiqueta derivada `Excedido de tiempo` para entregas en progreso fuera del plazo.

Correccion tecnica:
No agregar WebSockets/SSE como criterio de aceptacion. Para el MVP, polling o refresh manual es suficiente y mucho mas testeable.

Nota QA:
Validar que la etiqueta visual coincida con el estado real. Si el backend devuelve `EN_PROGRESO`, la UI debe decir `En progreso`; si devuelve `ENTREGADO`, debe decir `Entregado`.

### HU-35 - Inicio personal del examen con timer sincronizado

Decision: aceptar como indicador, no como control automatico fuerte.

Comentario DEV:
El timer puede aportar valor, pero no debe convertirse en un mecanismo complejo de bloqueo. PM incluso indica que el alumno puede seguir respondiendo despues de llegar a cero. Por lo tanto, el timer es informativo y el estado de exceso de tiempo puede derivarse.

Version corregida:

Como alumno, quiero ver el tiempo restante del examen, para administrar mi tiempo durante la resolucion.

Criterios de aceptacion corregidos:

- El alumno puede comenzar solo si el examen esta disponible y selecciono un tema.
- Al comenzar, accede a las preguntas del tema seleccionado.
- Si el examen tiene duracion y hora de inicio, se muestra un timer visible.
- El timer se calcula usando la hora de inicio/publicacion del examen y la duracion configurada.
- Si el alumno entra tarde, el timer muestra el tiempo restante real.
- Cuando el timer llega a cero, se muestra un aviso visual.
- El sistema no bloquea automaticamente la escritura ni el envio.
- En la vista del profesor, el exceso de tiempo puede mostrarse como etiqueta derivada si la entrega sigue en progreso pasada la hora limite.

Fuera de MVP:

- Sincronizacion exacta por WebSocket.
- Persistir un nuevo estado `EXCEDIDO_DE_TIEMPO` salvo que PM lo pida explicitamente como regla de negocio.

### HU-36 - Responder preguntas de texto libre durante el examen

Decision: aceptar. Ya esta mayormente implementada.

Comentario DEV:
Esta HU esta alineada con el objetivo original del docente: digitalizar la rendicion. Debe quedarse, pero sin agregar offline robusto como requisito obligatorio.

Version corregida:

Como alumno, quiero responder preguntas de texto libre durante el examen, para completar mi entrega.

Criterios de aceptacion corregidos:

- Durante el examen, el alumno ve las preguntas del tema seleccionado en el orden definido por el profesor.
- Para preguntas de texto libre, el alumno dispone de un campo de texto.
- Las respuestas se guardan automaticamente mientras el alumno escribe.
- El alumno puede guardar manualmente si existe un boton de guardado.
- El alumno puede modificar sus respuestas mientras la entrega este `EN_PROGRESO`.
- Una vez entregado, no puede modificar respuestas.
- El sistema muestra de forma clara si la entrega esta en progreso o entregada.

Opcional:

- Indicador visual de pregunta respondida/no respondida.
- Navegacion anterior/siguiente. Para MVP, una lista scrollable tambien es aceptable si es usable.
- Copia local en navegador para recuperacion ante caida temporal. No debe ser DoD obligatorio en esta milestone.

### HU-37 - Responder preguntas de diagrama/tabla durante el examen

Decision: aceptar con precision de alcance.

Comentario DEV:
La app ya soporta arbol de decision y tabla de decision. El documento PM dice "diagrama/tabla" y "figuras basicas", lo cual puede interpretarse como un editor generico de diagramas. Eso excede el MVP si no era parte de HU-26/HU-27.

Version corregida:

Como alumno, quiero responder preguntas practicas usando los editores de arbol de decision y tabla de decision, para completar las preguntas que requieren una respuesta visual/estructurada.

Criterios de aceptacion corregidos:

- Para preguntas configuradas como tabla de decision, el alumno ve el editor de tabla.
- Para preguntas configuradas como arbol de decision, el alumno ve el editor de arbol.
- El alumno puede construir o modificar su respuesta mientras la entrega este `EN_PROGRESO`.
- Las respuestas visuales se guardan automaticamente.
- Una vez entregado el examen, los editores quedan en modo lectura.
- El estado de pregunta respondida puede actualizarse cuando el alumno interactua con el editor.

Fuera de MVP:

- Editor generico de diagramas con figuras libres.
- Edicion colaborativa o en tiempo real.
- Sincronizacion offline obligatoria.

### HU-38 - Envio del examen por el alumno

Decision: aceptar. Ya esta mayormente implementada.

Comentario DEV:
Esta historia esta alineada con el flujo basico del MVP. Es una de las piezas centrales de la rendicion.

Version corregida:

Como alumno, quiero enviar mi examen cuando termine, para que el profesor pueda acceder a mis respuestas.

Criterios de aceptacion corregidos:

- El alumno tiene un boton `Entregar examen` durante la resolucion.
- Al hacer clic, el sistema solicita confirmacion e informa que no podra modificar respuestas despues.
- Al confirmar, el sistema guarda las respuestas pendientes antes de entregar.
- La entrega pasa a estado `ENTREGADO`.
- Las respuestas quedan registradas de forma definitiva.
- El alumno ve un mensaje de confirmacion de entrega exitosa.
- Tras entregar, el alumno solo puede ver su entrega en modo lectura.
- En el panel del profesor, la entrega aparece como `Entregado` luego de actualizar o en el siguiente ciclo de polling.

Ajuste sobre conexion:
Si el alumno cierra la ventana antes de entregar, deberian conservarse las respuestas ya guardadas en servidor. La promesa de recuperar cualquier cambio no sincronizado por perdida de conexion debe quedar como mejora, no como criterio obligatorio de esta HU.

## 7. Alcance recomendado para Milestone 3

Para que la milestone sea defendible ante el docente y testeable por QA, recomendamos reducirla a estas cards:

### M3-01 - Dashboard alumno de examenes y entregas

Incluye listado de examenes disponibles, en progreso, entregados y cerrados.

### M3-02 - Seleccion manual de tema

Reemplaza HU-24. El alumno selecciona tema antes de comenzar y no ve preguntas hasta confirmar.

### M3-03 - Publicar/Iniciar y cerrar examen

Usa el flujo existente de publicar/cerrar. Agrega confirmacion y wording si hace falta.

### M3-04 - Rendicion del examen

Texto libre, tabla/arbol de decision, guardado automatico y envio final. Ya esta mayormente hecho.

### M3-05 - Panel profesor de entregas

Muestra alumnos con entrega creada, tema y estado. Refresh manual obligatorio; polling automatico opcional.

### M3-06 - Timer informativo

Calculado por fecha/hora de inicio y duracion. No bloquea. Exceso de tiempo como etiqueta derivada.

## 8. Fuera de scope recomendado

Estas funcionalidades deberian quedar fuera de Milestone 3 salvo que el docente las pida explicitamente:

- WebSockets o SSE.
- Monitoreo instantaneo en tiempo real.
- Sala de espera con push automatico.
- Offline-first con IndexedDB y sincronizacion de conflictos.
- Editor generico de diagramas.
- Asignacion formal de examenes a cursos/alumnos, si no existe todavia el modelo de cursos.
- Estado persistido `EXCEDIDO_DE_TIEMPO`.
- Correccion manual/IA y publicacion de notas, porque PM las declaro fuera de esta milestone, aunque deberian priorizarse despues por estar mas cerca del pedido original del docente.
- Flujo de aprobacion del director de catedra.

## 9. Preguntas que PM debe responder antes de cerrar alcance

1. Cuando dicen "examenes asignados al alumno", quieren realmente implementar cursos, inscripciones y asignacion de examenes, o alcanza con examenes publicados visibles para todos los alumnos?

2. El "inicio formal del profesor" es una accion nueva distinta de `Publicar`, o podemos renombrar/usar `Publicar` como inicio del examen?

3. La seleccion manual de tema responde a una regla real del docente en aula, o fue una decision de producto agregada por PM?

4. El timer debe ser solo informativo o debe afectar estados persistidos del backend?

5. "No iniciado" debe mostrarse contra que universo de alumnos: todos los usuarios con rol ALUMNO, alumnos de un curso, o alumnos asignados a ese examen?

6. El guardado offline es realmente necesario para el MVP de clase, o alcanza con guardado automatico online?

7. Para QA: que casos de prueba priorizan si el tiempo es acotado? Recomendacion DEV: seleccion de tema, inicio, guardado, entrega, bloqueo post-entrega y panel de profesor.

## 10. Definition of Done corregida

Una historia se considera completa cuando:

- Sus criterios de aceptacion estan claros y son testeables.
- QA puede validar el flujo sin depender de infraestructura en tiempo real.
- La implementacion no contradice reglas ya existentes sin una decision explicita de PM.
- El comportamiento esta documentado en release note o contexto del proyecto.
- Hay al menos pruebas unitarias/backend para reglas criticas cuando aplique.
- La demo muestra el proceso real, incluyendo lo que quedo pendiente, sin sobreadornar funcionalidades no implementadas.

## 11. Conclusion DEV

La Milestone 3 debe enfocarse en cerrar bien la rendicion del examen de punta a punta, no en sumar complejidad tecnica para que la app "parezca mas copada".

Propuesta concreta:

- Mantener lo que ya funciona.
- Ajustar la seleccion manual de tema si PM confirma que reemplaza HU-24.
- Usar publicar/iniciar y cerrar como ciclo simple del examen.
- Mostrar estados de entrega al profesor con refresh/polling, no WebSockets.
- Tratar el timer como indicador informativo.
- Postergar offline robusto y tiempo real estricto.

Esto respeta mejor el pedido original del docente: una solucion iterativa, profesional, centrada en digitalizar la creacion, administracion y evaluacion del parcial, sin perder el control humano del profesor.
