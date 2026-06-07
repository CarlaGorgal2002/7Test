# Programa de Contenidos - Testing de Aplicaciones (UADE)

## Unidad 1: Fundamentos del Testing de Software
- **Conceptos de Testing:** Definición de fallas, defectos y errores. Verificación vs. Validación.
- **Niveles de Pruebas:** Pruebas unitarias, pruebas de integración, pruebas de sistema, y pruebas de aceptación de usuario (UAT).
- **Enfoques de Testing:**
  - **Pruebas Estáticas:** Revisiones de código, inspecciones técnicas, análisis de complejidad.
  - **Pruebas Dinámicas:** Ejecución del código con casos de prueba definidos.

## Unidad 2: Técnicas de Caja Negra (Black-Box Testing)
- **Partición por Equivalencia (Equivalence Partitioning):** División del dominio de entrada en clases de datos válidas e inválidas para las cuales el comportamiento del sistema debe ser equivalente.
- **Análisis de Valores Límite (Boundary Value Analysis):** Identificación y prueba de los extremos/fronteras de las particiones equivalentes (ej. límites, justo debajo, justo encima).
- **Tablas de Decisión:** Representación de combinaciones de condiciones de entrada y sus respectivas acciones de salida.
  - Estructura: Condiciones (reglas de negocio), Combinaciones (V/F o Sí/No), Acciones asociadas.
- **Transición de Estados:** Pruebas del comportamiento de un sistema en base a sus cambios de estado ante diversos eventos o estímulos.

## Unidad 3: Técnicas de Caja Blanca (White-Box Testing)
- **Cobertura de Control de Flujo:**
  - **Cobertura de Sentencias (Statement Coverage):** Garantiza que cada línea/instrucción de código ejecutable sea ejecutada al menos una vez.
  - **Cobertura de Decisiones o Ramas (Decision/Branch Coverage):** Garantiza que cada decisión condicional (caminos true y false de cada `if`, `while`, `switch`) sea evaluada al menos una vez.
  - **Cobertura de Condiciones (Condition Coverage):** Evalúa cada condición atómica (subexpresión lógica simple) a true y false.
  - **Cobertura de Caminos (Path Coverage):** Probar todos los caminos de ejecución posibles a través del método de principio a fin.
- **Complejidad Ciclomática de McCabe:** Métrica que mide la complejidad de un programa y el número de caminos independientes en un grafo de control de flujo.
  - Fórmula: $M = E - N + 2P$ (donde $E$ es número de aristas, $N$ es número de nodos y $P$ es número de componentes conexos, usualmente 1 para un único método).

## Unidad 4: Automatización y Calidad
- **Pruebas Unitarias Automatizadas:** Uso de JUnit y Mockito para aislar componentes (Mocks, Stubs, Spies) y verificar comportamientos/estados mediante aserciones limpias.
- **Análisis de Cobertura con JaCoCo:** Herramientas para medir las líneas y ramas de control cubiertas por la suite de pruebas automatizadas.
