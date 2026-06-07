# Guión de presentación · Sprint 3 · 7test
**Fecha:** 03/06/2026  
**Duración estimada:** 15–20 minutos

---

## Distribución de slides

| Slide | Tema | Quién habla |
|---|---|---|
| 1 | Portada | Carla |
| 2 | Roadmap | **Mario** |
| 3 | Qué entregamos este sprint | Carla |
| 4 | El flujo del alumno | **Mario** |
| 5 | El ciclo del profesor | Carla |
| 6 | Base de datos: antes y ahora | **Martín** |
| 7 | Arquitectura de deployment | Carla |
| 8 | Pruebas de caja blanca | Carla |
| 9 | Los bugs que encontró el pipeline | **Martín** |
| 10 | Pipeline CI/CD | **Mario** |
| 11 | Lo que no salió perfecto | Carla |
| 12 | Sprint 4 — Lo que viene | **Mario** |
| 13 | Live Test | Carla |
| 14 | Cierre | Carla |

---

## MARIO — Tus slides

### SLIDE 2 · Roadmap *(~60 segundos)*

**Qué hay en pantalla:** Una línea de tiempo con 4 nodos:
Sprint 1 (verde ✅) → Sprint 2 (verde ✅) → **Sprint 3** (azul, resaltado) → Sprint 4 (gris).

**Guión:**
> "Para contextualizar rápido: en el Sprint 1 construimos todo lo de usuarios y autenticación. En el Sprint 2 le dimos al profesor las herramientas para crear un examen —temas, preguntas, publicación. Este sprint, el tercero, cierra el ciclo del lado del alumno: entrar, rendir, entregar. Y lo que viene en el Sprint 4 es algo que el docente mencionó desde el día uno como objetivo: la corrección asistida con inteligencia artificial."

---

### SLIDE 4 · El flujo del alumno *(~90 segundos)*

**Qué hay en pantalla:** Flujo con 5 pasos:
Dashboard → Elige Tema → Responde → Entrega → Ve su nota (Sprint 4).
Abajo: screenshot de la pantalla de selección de tema con botones grandes Tema A, B, C.

**Guión:**
> "El alumno entra al dashboard y ve los exámenes disponibles. Cuando hace clic en uno, llega a una pantalla con botones grandes —uno por cada tema creado por el profesor. Elige el suyo según lo que el docente indica en el aula, confirma, y ya puede responder. Mientras está respondiendo, tiene el timer visible todo el tiempo. El sistema guarda automáticamente. Cuando termina, confirma la entrega y queda registrado."
>
> "Una cosa que me parece interesante visualmente: la pantalla del alumno cambia de color según el tema que eligió. Entonces el profesor, paseándose por el aula, puede ver de lejos en qué tema está cada uno."

---

### SLIDE 10 · Pipeline CI/CD *(~45 segundos)*

**Qué hay en pantalla:** Flujo horizontal con 5 pasos: Push → GitHub Actions → 59 Tests → BUILD SUCCESS → Deploy. Abajo, una flecha amarilla mostrando que si un test falla, el deploy queda bloqueado.

**Guión:**
> "Yo armé el pipeline de integración continua. Lo que hace es simple: cada vez que subimos código a GitHub, Actions levanta un entorno limpio y corre los 59 tests automáticamente. Si todos pasan, deploya. Si alguno falla, el deploy no ocurre. Esto nos garantiza que lo que está en producción siempre pasó por la suite de pruebas."

---

### SLIDE 12 · Sprint 4 — Lo que viene *(~75 segundos)*

**Qué hay en pantalla:** Tres tarjetas: Corrección con IA / Nota final / Tests de frontend. A la derecha, una ilustración de un robot revisando un documento.

**Guión:**
> "Lo más interesante que viene es la corrección asistida con IA. El docente ya carga la respuesta modelo para cada pregunta —eso lo hace hoy en la app. Cuando el alumno entrega, la IA compara esa respuesta con la del alumno y sugiere un puntaje en escala de cuartos: cero, un cuarto, la mitad, tres cuartos, o el punto completo. Es una sugerencia. El docente la puede aceptar, modificar o rechazar. Eso fue un requisito no negociable desde el día uno: la plataforma no tiene la última palabra sobre la nota. Ese es el docente."
>
> "También vamos a publicar las notas finales calculadas, y a agregar tests de frontend con Jest y React Testing Library."

---

## MARTÍN — Tus slides

### SLIDE 6 · Base de datos: antes y ahora *(~75 segundos)*

**Qué hay en pantalla:** Comparación lado a lado. ANTES: H2 en memoria (❌ se borra al reiniciar, ❌ QA recrea usuarios cada vez). AHORA: PostgreSQL en la nube (✅ datos persisten, ✅ 47 usuarios pre-cargados).

**Guión:**
> "Una mejora técnica importante: migramos la base de datos. En los dos sprints anteriores, los datos vivían en la memoria del servidor. Eso está bien para desarrollar, pero cada vez que el servidor se reiniciaba —que en los planes gratuitos pasa seguido— se perdía todo. El equipo de QA tenía que crear todos los usuarios de vuelta antes de cada sesión de prueba."
>
> "Ahora usamos PostgreSQL, que es una base de datos relacional robusta y open source, muy usada en la industria. Los datos persisten entre reinicios. Y aprovechamos para cargar 47 usuarios —alumnos, profesores y el administrador— listos para usar desde el día uno."

---

### SLIDE 9 · Los bugs que encontró el pipeline *(~90 segundos)*

**Qué hay en pantalla:** Dos tarjetas con borde amarillo. Bug 1: dos cuadrados de color (#2563EB ❌ → #1956D8 ✅). Bug 2: ruta inexistente en CI vs. solución con H2 en memoria.

**Guión:**
> "Cuando configuramos el pipeline de integración continua con GitHub Actions —básicamente un sistema que corre los tests automáticamente en la nube cada vez que subimos código— encontramos dos bugs que en nuestras máquinas no se veían."
>
> "El primero: un color incorrecto. Tenemos en el código los colores que se asignan automáticamente a los temas. El primer color era el #2563EB, que es un azul de Tailwind CSS, la librería de estilos del frontend. Pero el azul oficial de 7test es el #1956D8. Son casi iguales, pero distintos. El test lo detectó al instante."
>
> "El segundo: la configuración de la base de datos. En producción, la app usa un archivo en disco. El servidor de GitHub Actions no tiene ese archivo. El test que levanta el contexto de Spring fallaba. La solución fue un archivo de configuración separado solo para tests, con base de datos en memoria. Pequeño, pero si no lo resolvíamos, el pipeline nunca hubiera pasado."

---

## Tips para los dos

- Si Carla o alguien se queda en blanco, está perfecto improvisar con una o dos palabras del slide.  
- Mirá al público cuando podés, no a la pantalla.  
- Las slides tienen poco texto a propósito — el slide es el fondo visual, vos sos el contenido.  
- El live test al final lo conduce Carla, ustedes ya terminaron para ese momento.

---

*Cualquier duda, hablen con Carla antes de la presentación.*
