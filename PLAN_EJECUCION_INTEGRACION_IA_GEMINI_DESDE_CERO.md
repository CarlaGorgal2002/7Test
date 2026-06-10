# Integracion de Correccion con IA Gemini Desde Cero

**Estado:** implementado  
**Fecha base:** 10 de junio de 2026  
**Material oficial:** `src/main/resources/course-material/Todo_Testing_de_Apps.pdf`  
**SHA-256:** `FC7D8DA8052D96C2A3727DE4B4DFE91E93412F4B664EB8B2E31CDE5CC8E9DFF0`  
**Version del material:** `testing-apps-2026-06-10-v1`  
**Version del prompt:** `testing-grading-v2`

## Objetivo y alcance

La aplicacion incorpora una correccion tentativa por pregunta mediante Gemini. El docente inicia el trabajo
desde el modal de correccion, revisa cada sugerencia y decide si la acepta, rechaza o ignora. Gemini nunca
publica devoluciones ni decide la nota final. La correccion manual sigue disponible con Gemini habilitado,
deshabilitado o temporalmente caido.

La implementacion fue creada con componentes nuevos y aislados:

- `AiCorrectionUseCase` y `AiCorrectionService`: operaciones docentes, autorizacion y revision.
- `AiCorrectionProvider`: puerto independiente del proveedor.
- `GeminiCorrectionAdapter`: unica pieza que conoce el SDK oficial de Google.
- `CourseMaterialManager`: extraccion, indexado y seleccion local de paginas relevantes del PDF.
- `AiGradingWorker`: procesamiento asincrono y secuencial por pregunta.
- Repositorios, entidades, controlador y DTO exclusivos para correccion con IA.

## Crear y configurar la API key

1. Ingresar a [Google AI Studio API Keys](https://aistudio.google.com/apikey) con la cuenta del proyecto.
2. Crear una nueva API key.
3. Asociarla al proyecto correcto y restringir su uso a Gemini API cuando Google Cloud lo permita.
4. No escribirla en el frontend, `application.yaml`, Dockerfile, capturas, tickets ni commits.
5. Revocar inmediatamente cualquier clave que haya sido expuesta.

En PowerShell local:

```powershell
$env:GEMINI_ENABLED="true"
$env:GEMINI_API_KEY="tu-clave-real"
$env:GEMINI_MODEL="gemini-3.5-flash"
.\mvnw.cmd spring-boot:run
```

Las variables existen solo durante esa sesion de PowerShell. Para volver al modo manual:

```powershell
$env:GEMINI_ENABLED="false"
.\mvnw.cmd spring-boot:run
```

En Render, abrir el servicio backend, entrar a **Environment** y crear:

```text
GEMINI_ENABLED=true
GEMINI_API_KEY=<clave secreta>
GEMINI_MODEL=gemini-3.5-flash
```

No crear variables `VITE_GEMINI_*`: todo valor `VITE_*` puede quedar visible en el navegador.

## Flujo funcional

1. El docente define enunciado, respuesta modelo y criterios especificos opcionales.
2. El alumno entrega el examen.
3. El docente abre la correccion y pulsa **Generar sugerencias con IA**.
4. `POST /api/ai-grading/submissions/{submissionId}/jobs` devuelve HTTP `202`.
5. El worker procesa preguntas en orden, una llamada Gemini por respuesta no vacia.
6. Una respuesta vacia genera fraccion `0` sin consumir Gemini.
7. El frontend consulta progreso y muestra fallos individuales sin bloquear la correccion manual.
8. Aceptar copia puntaje y comentario a la correccion oficial, pero no publica devoluciones.
9. Regenerar crea nuevos intentos y conserva el historial.

## Criterios academicos y salida

El master prompt esta versionado en `src/main/resources/ai-grading/testing-grading-v2.txt`. Sus fuentes,
en orden de prioridad, son criterios docentes, respuesta modelo, fragmentos relevantes seleccionados
localmente del PDF y enunciado. La respuesta del alumno se delimita como contenido no confiable, nunca
puede modificar instrucciones y tampoco participa en la seleccion de paginas.

Solo se aceptan fracciones `0`, `0.25`, `0.50`, `0.75` y `1`. El backend calcula:

```text
puntajeSugerido = puntajeMaximo * fraccion
```

La salida estructurada incluye comentario, fortalezas, problemas, paginas, confianza y necesidad de
revision humana. Confianza baja o ausencia de paginas en una respuesta no vacia fuerza revision humana.

## Seguridad y privacidad

- La API key solo vive en backend y nunca se devuelve en `/api/ai-grading/status`.
- No se envian nombres, emails ni IDs personales a Gemini.
- No se registran prompts completos, respuestas del alumno ni respuestas crudas de Gemini.
- El PDF se indexa localmente una vez y cada llamada no vacia recibe como maximo 8 paginas relevantes.
- La seleccion de paginas usa criterios docentes, respuesta modelo y enunciado; nunca usa la respuesta del alumno.
- Gemini solo puede citar las paginas seleccionadas y debe exigir revision humana si resultan insuficientes.
- La salida se valida antes de persistirse.
- Solo el profesor propietario puede iniciar, consultar, aceptar o rechazar sugerencias.
- Se bloquean trabajos y revisiones luego de publicar devoluciones.
- El alumno solo recibe puntaje y comentario oficiales publicados.

## API docente

```text
GET  /api/ai-grading/status
POST /api/ai-grading/submissions/{submissionId}/jobs
GET  /api/ai-grading/jobs/{jobId}
GET  /api/ai-grading/submissions/{submissionId}/suggestions
POST /api/ai-grading/suggestions/{suggestionId}/accept
POST /api/ai-grading/suggestions/{suggestionId}/reject
```

## Verificacion

Backend:

```powershell
$env:JAVA_HOME="C:\ruta\a\jdk-21"
.\mvnw.cmd clean verify
```

Frontend:

```powershell
cd frontend
npm ci
npm test
npm run build
```

Las pruebas usan mocks o un proveedor falso y nunca consumen la API real. Antes de desplegar, realizar
una prueba controlada local con una clave real y confirmar que logs, respuestas HTTP y frontend no exponen
la clave, prompts completos ni respuestas crudas.

## QA manual minimo

1. Crear un examen con respuestas modelo y criterios opcionales.
2. Publicarlo, rendirlo y entregarlo como alumno.
3. Abrir la entrega como docente y generar sugerencias.
4. Comprobar progreso, texto, tabla, arbol y fallo individual.
5. Aceptar, rechazar, editar manualmente y regenerar para verificar historial.
6. Publicar devoluciones y verificar que el alumno solo vea la correccion oficial.
7. Repetir con `GEMINI_ENABLED=false` y confirmar que el flujo manual funciona completo.
