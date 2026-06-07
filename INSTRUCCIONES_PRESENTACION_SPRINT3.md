# Instrucciones para Claude Desktop — Generar PPTX Sprint 3 · 7test

> **Para Claude Desktop:** Generá una presentación PowerPoint (.pptx) siguiendo
> estas instrucciones al pie de la letra. Cada slide tiene especificado: layout,
> colores, tipografía, contenido textual, placeholders de imagen y guión del orador.
> No agregues texto extra ni inventes contenido. El objetivo es minimalismo visual:
> mucho espacio en blanco, pocos textos, impacto visual fuerte.

---

## CONFIGURACIÓN GLOBAL

**Relación de aspecto:** 16:9 — 33.87 cm × 19.05 cm  
**Total de slides:** 14  
**Duración estimada:** 15–18 minutos

### Paleta de colores
| Nombre | Hex | Uso |
|---|---|---|
| Azul primario | `#1956D8` | Botones, acentos, highlights |
| Azul oscuro | `#09222A` | Fondos oscuros, texto oscuro |
| Verde éxito | `#03BB83` | Éxito, checks, números destacados |
| Celeste suave | `#CBEEF3` | Fondos de página claros |
| Amarillo alerta | `#FFC012` | Alertas, warnings, call-outs |
| Blanco | `#FFFFFF` | Texto sobre fondo oscuro, fondos |
| Gris | `#6B7280` | Texto secundario, elementos inactivos |

### Tipografías
- **Títulos:** Poppins Bold (fallback: Calibri Bold)
- **Cuerpo:** Poppins Regular (fallback: Calibri)
- **Código/técnico:** Courier New

### Reglas de diseño
- Máximo 3–4 bullets por slide. Si hay más información, se divide en otro slide.
- Texto en slide = lo que el orador necesita que el público vea. El resto va en el guión.
- Íconos/emojis como elementos visuales principales donde sea posible.
- Cada slide debe tener al menos 40 % de espacio libre (aire).

---

## SLIDE 1 — PORTADA

### Layout
Fondo con degradado diagonal: de `#09222A` (izquierda) a `#1956D8` (derecha).  
Dividir la slide en dos mitades verticales.

**Mitad izquierda (texto):**
- Ícono de birrete (mortarboard) en blanco, 60 pt, centrado arriba
- Texto "**7test**" en blanco, Poppins Bold, 96 pt
- Texto "Sprint 3 · Junio 2026" en blanco semitransparente (`rgba(255,255,255,0.7)`), 22 pt
- Línea horizontal blanca fina (2 px) como separador
- Frase en blanco, 18 pt, itálica: *"El examen ya se puede rendir."*
- Debajo: tres columnas con los integrantes, texto blanco, 13 pt:
  - **Carla Gorgal** / Tech Lead · Frontend · DevOps
  - **Mario Besednjak** / Backend Lead
  - **Martín Gueler** / Backend Support

**Mitad derecha (imagen):**
```
[INSERTAR: ilustración flat design de un estudiante usando laptop/tablet 
para rendir un examen digital. Colores: azul y verde (#1956D8, #03BB83). 
Sin texto en la imagen. Estilo minimalista. Fondo transparente o azul oscuro.
Prompt sugerido para IA: "flat design illustration, student sitting at desk 
with laptop taking a digital exam, blue and teal color palette, minimalist, 
no text, transparent background"]
```

### Guión — CARLA
> "Buenas. Somos el equipo de desarrollo de 7test. En este sprint les mostramos un hito: por primera vez, el ciclo completo funciona. Un alumno puede entrar a la plataforma, ver el examen, elegir su tema, responder y entregar. De punta a punta. Digital."

---

## SLIDE 2 — DÓNDE ESTAMOS: EL ROADMAP

### Layout
Fondo: `#CBEEF3` (celeste muy suave).  
Título en `#09222A`, Poppins Bold, 32 pt, arriba izquierda.

**Línea de tiempo horizontal** centrada verticalmente:
- 4 nodos conectados por líneas horizontales
- Cada nodo: círculo de 80px con número de sprint adentro
- Textos debajo de cada círculo

| Nodo | Color | Sprint | Texto debajo |
|---|---|---|---|
| 1 | `#03BB83` (verde, check ✅) | Sprint 1 | Login & Usuarios |
| 2 | `#03BB83` (verde, check ✅) | Sprint 2 | Crear & Configurar Examen |
| 3 | `#1956D8` (azul, resaltado, 20% más grande) | **Sprint 3** | **Rendir el Examen** |
| 4 | `#6B7280` (gris, punteado) | Sprint 4 | Corrección con IA |

El nodo 3 debe tener un borde más grueso o un halo/glow en `#1956D8` para indicar "acá estamos".

**Texto del slide:** Solo el título "Roadmap" y los cuatro labels. Sin párrafos.

### Guión — MARIO
> "Para contextualizar rápido: en el Sprint 1 construimos todo lo de usuarios y autenticación. En el Sprint 2 le dimos al profesor las herramientas para crear un examen —temas, preguntas, publicación. Este sprint, el tercero, cierra el ciclo del lado del alumno: entrar, rendir, entregar. Y lo que viene en el Sprint 4 es algo que el docente mencionó desde el día uno como uno de sus objetivos: la corrección asistida con inteligencia artificial."

---

## SLIDE 3 — ¿QUÉ ENTREGAMOS ESTE SPRINT?

### Layout
Fondo: `#09222A` (oscuro).  
Título en blanco, Poppins Bold, 32 pt: "Sprint 3 — Lo que entregamos"

**Grid de 3 columnas × 3 filas** con tarjetas redondeadas (border-radius 12px).  
Color de tarjetas: `rgba(255,255,255,0.08)` (blanco muy transparente sobre oscuro).  
Texto en cada tarjeta: ícono grande (36pt) + título en blanco (16pt Bold) + una línea en `#CBEEF3` (14pt).

| Columna | Tarjeta 1 | Tarjeta 2 | Tarjeta 3 |
|---|---|---|---|
| 👨‍🎓 **Alumno** | 🎯 Elige su tema | 📝 Responde preguntas | ✅ Entrega el examen |
| 👩‍🏫 **Profesor** | 🚀 Publica el examen | 👁️ Monitorea en tiempo real | 🏆 Califica y devuelve |
| ⚙️ **Sistema** | ⏱️ Timer informativo | 💾 Guardado automático | 🎨 Fondo por tema |

Cada celda: título de la fila (Alumno/Profesor/Sistema) en `#FFC012` a la izquierda como label vertical, o como encabezado de columna.

**Texto del slide:** Solo los títulos de las 9 tarjetas. Nada más.

### Guión — CARLA
> "¿Qué entregamos? El flujo completo. Para el alumno: puede elegir su tema, responder y entregar. Para el profesor: puede publicar el examen, ver en tiempo real quién está respondiendo y quién entregó, calificar pregunta por pregunta, y publicar las devoluciones con puntaje y comentario. El sistema hace guardado automático, tiene un timer para que el alumno sepa cuánto tiempo le queda, y —algo que me parece elegante— la pantalla del alumno cambia de color según el tema que eligió. El profesor, paseándose por el aula, puede ver de lejos qué tema está rindiendo cada uno."

---

## SLIDE 4 — EL FLUJO DEL ALUMNO (visual)

### Layout
Fondo: blanco.  
Título en `#09222A`, Poppins Bold, 28 pt: "El alumno rinde de punta a punta"

**Flujo horizontal** con 5 pasos conectados por flechas (`#1956D8`):

```
[📊 Dashboard]  →  [🎯 Elige Tema]  →  [📝 Responde]  →  [✅ Entrega]  →  [📋 Ve su nota]
```

Cada paso: rectángulo redondeado con fondo `#1956D8` y texto blanco (para los pasos activos en sprint 3) o `#CBEEF3` con texto gris (para "Ve su nota", que es Sprint 4).

Debajo de cada paso, texto pequeño en `#6B7280` (12 pt, itálica):
- Dashboard: *"Ve exámenes disponibles"*
- Elige Tema: *"Botones grandes, un color por tema"*
- Responde: *"Con timer + guardado auto"*
- Entrega: *"Con confirmación previa"*
- Ve su nota: *"Sprint 4 →"*

**Imagen a insertar debajo del flujo (ocupando mitad inferior del slide):**
```
[INSERTAR: screenshot real de la pantalla de selección de tema de la app 
7test — debería mostrar botones grandes "Tema A", "Tema B", "Tema C" con 
sus colores pastel. Si no tenés screenshot, usar este placeholder:
"Mockup de pantalla de selección de tema con 3 botones grandes de colores 
diferentes (verde, azul, naranja) sobre fondo oscuro"]
```

### Guión — MARIO
> "El alumno entra al dashboard y ve los exámenes disponibles. Cuando hace clic en uno, llega a una pantalla con botones grandes —uno por cada tema creado por el profesor. Elige el suyo según lo que el docente indica en el aula, confirma, y ya puede responder. Mientras está respondiendo tiene el timer visible todo el tiempo. El sistema guarda automáticamente. Cuando termina, confirma la entrega, y queda registrado. Cuando el profesor devuelve, puede ver su puntaje y los comentarios pregunta por pregunta. El último paso —ver la nota calculada— lo terminamos en el próximo sprint."

---

## SLIDE 5 — EL CICLO DE VIDA DEL EXAMEN (vista profesor)

### Layout
Fondo: `#09222A`.  
Título en blanco, Poppins Bold, 28 pt: "El profesor controla el ciclo completo"

**Diagrama de estados horizontal**, centrado verticalmente en el slide.  
Cada estado: círculo grande (90px) con el nombre del estado adentro.  
Conectados por flechas con etiqueta de acción.

```
[BORRADOR] ──publica──► [PUBLICADO] ──inician alumnos──► [EN CURSO] ──cierra──► [CERRADO] ──devuelve──► [DEVUELTO]
  #6B7280                #1956D8                          #03BB83               #FFC012                #03BB83 oscuro
```

Debajo del estado "EN CURSO": una flecha hacia abajo que dice "⏱️ tiempo extra" (máx. 1 vez, hasta 60 min). El popup aparece automáticamente cuando el timer llega a cero.

**Texto del slide:** Solo los estados y las acciones entre flechas. Sin párrafos.

### Guión — CARLA
> "El profesor parte de un borrador, donde construye el examen libremente. Cuando lo publica, los alumnos pueden verlo y empezar a responder. En ese momento el examen está 'en curso' y el profesor tiene un panel de monitoreo. Si el tiempo se acaba y quiere dar más minutos, le aparece un popup para agregar tiempo extra —hasta 60 minutos, una sola vez. Cuando cierra el examen, entra a corregir. Y cuando termina de calificar todas las entregas, publica las devoluciones de un solo golpe."

---

## SLIDE 6 — BASE DE DATOS: ANTES Y AHORA

### Layout
Fondo: blanco.  
Título en `#09222A`, Poppins Bold, 28 pt: "Los datos ahora persisten"

**Comparación lado a lado. Dos bloques iguales separados por una flecha central "SPRINT 3 →".**

**Bloque izquierdo — ANTES (borde `#6B7280`, fondo gris muy suave):**
- Ícono de RAM o memoria (🧠 o ícono de chip)
- Título: "H2 en memoria" (Poppins Bold, 20pt, gris)
- Subtítulo gris pequeño: *sprints 1 y 2*
- Bullets (16pt, gris):
  - ❌ Se borra al reiniciar el servidor
  - ❌ Solo sirve para desarrollo
  - ❌ QA recrea usuarios cada sesión

**Bloque derecho — AHORA (borde `#03BB83`, fondo verde muy suave):**
- Ícono del elefante de PostgreSQL (o 🐘)
- Título: "PostgreSQL en la nube" (Poppins Bold, 20pt, `#09222A`)
- Subtítulo verde pequeño: *sprint 3*
- Bullets (16pt, `#09222A`):
  - ✅ Datos persisten entre reinicios
  - ✅ 47 usuarios pre-cargados listos para QA
  - ✅ Hosteado en Render (gratuito)

**En el pie del slide**, nota pequeña en `#6B7280`: *"H2 = base de datos en memoria. PostgreSQL = base de datos relacional industrial, open source."*

### Guión — MARTÍN
> "Una mejora técnica importante: migramos la base de datos. En los dos sprints anteriores, los datos vivían en la memoria del servidor. Eso está bien para desarrollar, pero cada vez que el servidor se reiniciaba —que en los planes gratuitos pasa seguido— se perdía todo. El equipo de QA tenía que crear todos los usuarios de vuelta antes de cada sesión de prueba. Ahora usamos PostgreSQL, que es una base de datos relacional robusta, open source, muy usada en la industria. Los datos persisten. Y aprovechamos para cargar 47 usuarios —alumnos, profesores y el administrador— listos para usar desde el día uno."

---

## SLIDE 7 — ARQUITECTURA DE DEPLOYMENT

### Layout
Fondo: `#09222A`.  
Título en blanco, Poppins Bold, 28 pt: "Dónde vive la app"

**Diagrama vertical** de tres bloques apilados con flechas entre ellos, centrado en la slide.  
Cada bloque: rectángulo redondeado (ancho 320px, alto 90px).

```
┌─────────────────────────────┐
│  🌐  VERCEL                 │
│  Frontend React + Vite      │  ← fondo #1956D8, texto blanco
└──────────────┬──────────────┘
               │  HTTPS / API REST
               ▼
┌─────────────────────────────┐
│  🐋  RENDER                 │
│  Backend Spring Boot        │  ← fondo #1956D8, texto blanco
│  (dentro de Docker)         │
└──────────────┬──────────────┘
               │  JDBC
               ▼
┌─────────────────────────────┐
│  🐘  RENDER                 │
│  PostgreSQL                 │  ← fondo #03BB83, texto blanco
└─────────────────────────────┘
```

A la derecha del diagrama, verticalmente centrado:
```
[INSERTAR: logos pequeños de Vercel (triángulo negro), Docker (ballena azul), 
PostgreSQL (elefante azul). Buscarlos en sus sitios oficiales o usar emojis 
como fallback. Tamaño sugerido: 50×50px cada uno.]
```

En el pie del slide, nota en `#CBEEF3` muy pequeña (12pt):  
*"⚙️ GitHub Actions ejecuta 59 tests automáticamente antes de cada deploy"*

### Guión — CARLA
> "Para los que no son del palo técnico, les explico rápido cómo está parada la app. Cuando abren el navegador y entran a 7test, están accediendo al frontend —la parte visual— que está en Vercel, un servicio de hosting gratuito para proyectos como este. Cuando el frontend necesita datos, le habla al backend, que corre en Render. El backend está dentro de un contenedor Docker. Docker es básicamente una forma de empaquetar toda la aplicación con sus dependencias para que funcione igual en cualquier máquina, sin el clásico 'en mi máquina anda pero en producción no'. Y los datos viven en una base de datos PostgreSQL, también en Render. Todo está conectado a GitHub: cada vez que hacemos un push, antes de que algo llegue a producción, se ejecutan automáticamente los tests."

---

## SLIDE 8 — PRUEBAS DE CAJA BLANCA ★ (SLIDE ESTRELLA)

### Layout
Fondo: `#09222A`.  
Título en blanco, Poppins Bold, 28 pt, arriba: "Pruebas de Caja Blanca"

**Centro del slide:** número enorme.

- Texto "**59**" en `#03BB83`, Poppins Bold, **144 pt**, centrado verticalmente en el slide.
- Debajo del 59: texto "tests ejecutados" en blanco, 18 pt.

**A la derecha del 59** (alineado verticalmente con el centro):  
Tres líneas en verde `#03BB83`, Poppins Bold, 36 pt:
```
0  failures
0  errors
0  skipped
```
Con el "0" en `#03BB83` y las palabras en blanco 24pt.

**Debajo del número central:** texto en `#FFC012`, Poppins Bold, 22 pt:
```
BUILD SUCCESS ✅
```

**Fila inferior del slide** — tabla de distribución (pequeña, 14pt, fondo semitransparente):
| AuthServiceTest | ExamServiceTest | ExamSubmissionServiceTest | PasswordPolicyServiceTest | UserServiceTest | ApplicationTests |
|---|---|---|---|---|---|
| 15 | 13 | 8 | 5 | 17 | 1 |

Texto de la tabla en `#CBEEF3`.

**Dashboards e imágenes a insertar en la franja inferior del slide (3 screenshots lado a lado, cada uno ~200×110px):**

```
[INSERTAR dashboard 1 — GitHub Actions:
  Ir a github.com/CarlaGorgal2002/7Test → pestaña "Actions" →
  click en el último workflow con tilde verde → screenshot del resumen
  que muestra los jobs con check verde y "All jobs passed".
  Posición: esquina inferior izquierda.]

[INSERTAR dashboard 2 — Maven BUILD SUCCESS:
  Para obtenerlo: abrir terminal en la carpeta del backend y correr
  .\mvnw.cmd test  (o mvn test si tenés Maven instalado).
  Capturar el output final que dice:
  "Tests run: 59, Failures: 0, Errors: 0, Skipped: 0"
  seguido de "BUILD SUCCESS".
  Posición: centro inferior.]

[INSERTAR dashboard 3 — Surefire Reports (opcional):
  Abrir carpeta backend/target/surefire-reports/ y hacer screenshot
  del listado de archivos .txt mostrando los 6 servicios testeados.
  Si no está disponible, omitir este tercero y agrandar los otros dos.
  Posición: esquina inferior derecha.]
```

### Guión — CARLA
> "Y ahora la parte que creo que más importa para esta materia: las pruebas. 59 tests de caja blanca. Los 59 pasan. Cero fallos, cero errores, cero salteados."
>
> "Para los que no lo vieron antes: la caja blanca es una técnica de testing donde quien diseña los casos conoce el código por dentro. No estamos probando desde afuera como usuario. Estamos verificando que cada rama del código —cada condición, cada validación, cada excepción— se comporta como debe. Para eso usamos JUnit 5 para escribir los tests, Mockito para simular las dependencias externas —como la base de datos— sin necesidad de conectarnos a nada real, y AssertJ para hacer las verificaciones de forma legible."
>
> "Los tests cubren cinco servicios: autenticación, usuarios, política de contraseñas, gestión de exámenes y el flujo de entregas. Un ejemplo concreto: en ExamService tenemos un test que verifica que si un tema tiene preguntas que suman 4 puntos en lugar de 10, el sistema rechaza la publicación. Otro que verifica que no se pueda publicar un examen si una pregunta tiene enunciado vacío. En ExamSubmissionService, verificamos que el alumno no pueda modificar una entrega ya enviada."

---

## SLIDE 9 — LOS BUGS QUE ENCONTRÓ EL CI/CD

### Layout
Fondo: blanco.  
Título en `#09222A`, Poppins Bold, 28 pt: "El pipeline encontró 2 bugs reales"

**Dos tarjetas lado a lado**, separadas por espacio.  
Cada tarjeta: borde `#FFC012`, border-radius 12px, padding 24px.

---

**Tarjeta izquierda — Bug 1: Color incorrecto**

Encabezado: 🎨 **Bug 1: Color incorrecto** (Poppins Bold 18pt, `#09222A`)

Subtítulo gris pequeño: *"Detectado por: ExamServiceTest · agregarTemas_asignaColoresDelSprintDos"*

Contenido visual centrado:
```
[cuadrado de color 60×60px, color #2563EB]   →   [cuadrado de color 60×60px, color #1956D8]
        ❌ #2563EB                                          ✅ #1956D8
    (azul de Tailwind CSS)                            (azul oficial de 7test)
```

Texto debajo (14pt, gris):  
*"Dos azules casi idénticos. El test lo detectó en segundos. En producción, nadie lo hubiera visto."*

---

**Tarjeta derecha — Bug 2: Contexto de Spring en CI**

Encabezado: ☁️ **Bug 2: Base de datos inexistente en CI** (Poppins Bold 18pt, `#09222A`)

Subtítulo gris pequeño: *"Detectado por: ApplicationTests · contextLoads"*

Contenido:
- ❌ En GitHub Actions: `/app/data/seventest` no existe
- ✅ Solución: archivo de config separado para tests con H2 en memoria

Texto debajo (14pt, gris):  
*"En nuestra máquina andaba. En el servidor de CI, no. El pipeline lo frenó antes de que llegara a producción."*

### Guión — MARTÍN
> "Cuando configuramos el pipeline de integración continua con GitHub Actions —básicamente un sistema que corre los tests automáticamente en un servidor en la nube cada vez que subimos código— encontramos dos bugs que en nuestras máquinas no se veían."
>
> "El primero: un color incorrecto. Tenemos en el código la lista de colores que se asignan automáticamente a los temas. El primer color era el #2563EB, que es un azul de Tailwind CSS —la librería de estilos que usamos en el frontend. Pero el azul oficial de 7test es el #1956D8. Son casi iguales, pero distintos. El test lo detectó al instante."
>
> "El segundo: la configuración de la base de datos. En producción, la app usa un archivo en disco. El servidor de GitHub Actions no tiene ese archivo. El test que levanta el contexto de Spring fallaba porque no podía conectarse. La solución fue un archivo de configuración separado solo para el entorno de tests, con una base de datos en memoria. Pequeño, pero si no lo resolvíamos, el pipeline nunca hubiera pasado."

---

## SLIDE 10 — PIPELINE CI/CD

### Layout
Fondo: `#09222A`.  
Título en blanco, Poppins Bold, 28 pt: "Integración continua con GitHub Actions"

**Flujo horizontal** con 5 pasos conectados por flechas blancas.  
Cada paso: círculo 80px con ícono adentro + texto debajo en blanco 13pt.

```
[💻 Push]  ──►  [⚙️ GitHub Actions]  ──►  [🧪 59 Tests]  ──►  [✅ BUILD SUCCESS]  ──►  [🚀 Deploy]
```

Flechas: blancas, 2px de grosor.

Debajo del flujo, una flecha separada con fondo `#FFC012` y texto `#09222A`:
```
[🧪 Algún test falla]  ──►  [❌ Deploy bloqueado]
```

**Texto al pie** en `#CBEEF3`, 14pt itálica:  
*"Si los tests no pasan, el código no llega a producción."*

### Guión — MARIO
> "Yo armé el pipeline de integración continua. Lo que hace es simple: cada vez que subimos código a GitHub, Actions corre automáticamente los 59 tests. Si todos pasan, deploya. Si alguno falla, el deploy no ocurre. Eso nos garantiza que lo que está en producción siempre pasó por la suite de pruebas."

---

## SLIDE 11 — PERCANCES (honestidad y soft skills)

### Layout
Fondo: blanco.  
Título en `#09222A`, Poppins Bold, 28 pt: "Lo que no salió perfecto"

**Tres bloques verticales** apilados con flechas "→" entre ellos.  
Cada bloque: rectángulo con borde izquierdo grueso (6px) de color y texto a la derecha.

---

**Bloque 1 — Borde `#FFC012` (amarillo):**  
🧭 **HUs demasiado ambiciosas**  
*"Los PMs diseñaron historias de usuario para un producto multi-universidad, multi-materia, con SSO de Microsoft. Nosotros necesitábamos un MVP funcional para una sola materia."*

---

**Bloque 2 — Borde `#1956D8` (azul):**  
🔄 **Iteramos**  
*"Las HUs del Milestone 3 tuvieron una versión 2, revisada junto a los devs. Eso es trabajo ágil: se detecta el desajuste, se comunica, se ajusta."*

---

**Bloque 3 — Borde `#03BB83` (verde):**  
📐 **Lo que nos llevamos**  
*"La comunicación entre equipos no es un problema. Es el ejercicio."*

---

**Imagen pequeña a la derecha** (60px × 60px, esquina inferior derecha):
```
[INSERTAR: ícono de Trello o tablero kanban, o emoji grande 🗂️]
```

### Guión — CARLA
> "Algo que queremos ser honestos sobre, y que creo que el docente aprecia que digamos: tuvimos un desacuerdo con los PMs en el alcance del proyecto. Las historias de usuario que recibimos estaban pensadas para algo mucho más grande de lo que el cliente —el docente— pidió como primer caso piloto. Estaban modelando un sistema para múltiples universidades, múltiples materias, con login por cuenta institucional de Microsoft. Todo muy ambicioso, pero lejos de lo que necesitábamos para este sprint."
>
> "Lo resolvimos iterando. Las historias de la Milestone 3 tienen una versión 2 que se armó con feedback nuestro. Y eso, en el fondo, es exactamente lo que esta materia busca que aprendamos. El trabajo real tiene roces entre equipos. La respuesta correcta es comunicarse y ajustar, no bloquearse ni culparse."

---

## SLIDE 12 — ¿QUÉ VIENE? (Sprint 4)

### Layout
Fondo: `#09222A`.  
Título en blanco, Poppins Bold, 28 pt: "Sprint 4 — Lo que viene"

**Tres tarjetas horizontales** con fondo `rgba(255,255,255,0.08)` y borde `#1956D8`.  
Cada tarjeta: ícono grande (40pt) + título (18pt blanco Bold) + 2 bullets (14pt `#CBEEF3`).

---

**Tarjeta 1 — 🤖 Corrección con IA**
- Gemini API compara respuesta del alumno con la respuesta modelo
- Sugiere: 0 / 0.25 / 0.5 / 0.75 / 1 por punto
- ⚠️ El docente tiene la última palabra, siempre.

---

**Tarjeta 2 — 📊 Nota final**
- La nota se calcula automáticamente
- El alumno la ve con su devolución completa

---

**Tarjeta 3 — 🧪 Tests de frontend**
- Jest + React Testing Library
- Cobertura de componentes React

---

**Imagen a la derecha del conjunto de tarjetas:**
```
[INSERTAR: ilustración de robot o asistente IA revisando un documento. 
Colores azul y verde. Sin texto. Flat design. 
Prompt: "flat design illustration of a friendly robot AI assistant reviewing 
a document or paper, blue and teal colors, minimalist, no text, 
transparent or dark background"]
```

**Nota pequeña al pie** en `#FFC012`, 13pt itálica:  
*"Regla no negociable desde el Sprint 1: el docente siempre tiene la última palabra sobre la nota."*

### Guión — MARIO
> "Lo más interesante que viene es la corrección asistida con IA. La idea es esta: el docente ya carga la respuesta modelo para cada pregunta —eso lo hace hoy en la app. Cuando el alumno entrega, la IA va a comparar esa respuesta modelo con la del alumno y sugiere un puntaje en escala de cuartos: cero, un cuarto, la mitad, tres cuartos o el punto completo. Eso es una sugerencia. El docente la puede aceptar, modificar o rechazar. No decide. Eso fue un requisito no negociable desde el día uno, cuando el docente nos dijo: 'La plataforma no tiene la última palabra en la nota. Ese es el docente.'"
>
> "También vamos a publicar las notas calculadas y a agregar tests de frontend con Jest y React Testing Library."

---

## SLIDE 13 — LIVE TEST

### Layout
Fondo: degradado diagonal de `#09222A` (izquierda) a `#1956D8` (derecha).

**Texto principal centrado verticalmente:**  
"**LIVE TEST**" en blanco, Poppins Bold, **96 pt**.

**Debajo:**  
`7test-frontend.vercel.app` en `#FFC012`, Poppins Bold, 22 pt.

**QR Code (esquina inferior derecha, 130×130px):**
```
[INSERTAR: QR apuntando a https://7test-frontend.vercel.app
Generarlo en qr-code-generator.com o qr-code-monkey.com.
Texto debajo del QR: "Probala vos también" en blanco, 13pt.]
```

### Guión — CARLA
*(Sin guión pre-escrito. Carla conduce el live test libremente.)*

---

## SLIDE 14 — CIERRE

### Layout
Fondo: degradado diagonal de `#09222A` (izquierda) a `#1956D8` (derecha).  
**Igual que la portada, pero sin la imagen lateral.**

**Centro:**
- Ícono birrete en blanco, 60pt
- "**7test**" en blanco, Poppins Bold, 80 pt
- "Sprint 3 · Junio 2026" en blanco semitransparente, 20 pt
- Línea blanca fina separadora
- Frase en blanco, 22 pt: *"El examen se puede rendir. 🎓"*

**Fila inferior — nombres del equipo (3 columnas):**
- Carla Gorgal / Tech Lead · Frontend · DevOps
- Mario Besednjak / Backend Lead
- Martín Gueler / Backend Support

### Guión — CARLA
> "Eso es todo. El Sprint 3 cierra el loop: tenemos un sistema donde el alumno puede rendir un examen de principio a fin. Hay pruebas automatizadas que verifican la lógica interna, un pipeline que las corre en cada cambio, y la app está deployada y funcionando. Sprint 4 trae la IA. ¿Preguntas?"

---

## RESUMEN DE REPARTO POR ORADOR

| Slide | Título | Orador | Duración est. |
|---|---|---|---|
| 1 | Portada | **CARLA** | 30 seg |
| 2 | Roadmap | Mario | 60 seg |
| 3 | Qué entregamos | **CARLA** | 90 seg |
| 4 | Flujo del alumno | Mario | 90 seg |
| 5 | Ciclo del profesor | **CARLA** | 75 seg |
| 6 | Base de datos | Martín | 75 seg |
| 7 | Deployment | **CARLA** | 90 seg |
| 8 | Caja blanca ⭐ | **CARLA** | 120 seg |
| 9 | Bugs encontrados | Martín | 90 seg |
| 10 | Pipeline CI/CD | Mario | 45 seg |
| 11 | Percances | **CARLA** | 90 seg |
| 12 | Qué viene | Mario | 75 seg |
| 13 | Live Demo | **CARLA** | 180 seg |
| 14 | Cierre | **CARLA** | 30 seg |

**Total estimado: ~17–18 minutos**

---

## CÓMO OBTENER LOS SCREENSHOTS (instrucciones para quien prepara el PPTX)

**BUILD SUCCESS / terminal:**
1. Abrir PowerShell en la carpeta `backend/` del proyecto
2. Correr: `$env:JAVA_HOME='C:\Program Files\JetBrains\IntelliJ IDEA Community Edition 2025.2\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\mvnw.cmd test`
3. Al finalizar, capturar el bloque final del output (los últimos ~20 líneas con el resumen y "BUILD SUCCESS")

**GitHub Actions:**
1. Ir a `github.com/CarlaGorgal2002/7Test`
2. Pestaña "Actions"
3. Click en el run verde más reciente
4. Screenshot del resumen con los jobs en verde

**Surefire Reports:**
1. Después de correr `.\mvnw.cmd test`, abrir `backend/target/surefire-reports/`
2. Screenshot del Explorador de Archivos mostrando los 6 archivos `.txt`

---

## NOTAS TÉCNICAS PARA CLAUDE DESKTOP

### Tamaños de fuente sugeridos
- Título de slide: 28–32 pt
- Número destacado (slide 8): 144 pt
- Texto "DEMO EN VIVO" (slide 13): 80 pt
- "7test" en portada/cierre: 80–96 pt
- Bullets: 16–18 pt
- Notas al pie: 12–14 pt

### Animaciones (si el software lo soporta)
- Slide 3: las 9 tarjetas aparecen de izquierda a derecha, fila por fila
- Slide 8: primero aparece el "59" solo (500ms), luego los ceros y el BUILD SUCCESS
- Slide 9: las dos tarjetas aparecen con fade-in
- El resto: sin animaciones o con simple "appear" por slide

### Iconografía
Usar emojis Unicode como elementos visuales principales. Son compatibles con PowerPoint en todas las plataformas. Los indicados en cada slide son los definitivos.

### Imágenes marcadas como `[INSERTAR ...]`
Hay 5 imágenes para insertar o generar:
1. **Slide 1:** Ilustración flat de estudiante con laptop (generar con IA)
2. **Slide 4:** Screenshot de la pantalla de selección de tema de la app real (tomar screenshot)
3. **Slide 7:** Logos de Vercel, Docker, PostgreSQL (buscar en sitios oficiales o usar emojis)
4. **Slide 12:** Ilustración flat de robot IA revisando documento (generar con IA)
5. **Slide 13:** QR Code apuntando a https://7test-frontend.vercel.app (generar en qr-code-generator.com)

### Slide 8 — imagen adicional
Si tenés el screenshot del terminal o GitHub Actions mostrando `BUILD SUCCESS` con 59 tests, insertarlo como imagen pequeña (150×80px) en la esquina inferior derecha del slide.

---

## PREGUNTAS PARA CARLA (a responder antes de hacer ajustes finales)

1. **¿Cuánto tiempo tienen asignado** para la presentación? (cambia si hay que recortar guiones)
2. **¿Tienen screenshot real** de la pantalla de selección de tema para el Slide 4?
3. **¿Tienen el screenshot** del BUILD SUCCESS de GitHub Actions para el Slide 8?
4. **¿Ya tienen el examen demo preparado** en la app para el live test, o van a crearlo en vivo?
5. **¿Mario y Martín tienen acceso a este documento** para repasar sus guiones?
6. **¿El nombre de los temas en la demo** son Tema A, B, C como dice el Release Note, o algo diferente?

---

*Documento generado por Claude · Proyecto 7test · Sprint 3 · 03/06/2026*
