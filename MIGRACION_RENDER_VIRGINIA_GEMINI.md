# Migracion del backend de Render para habilitar Gemini

## Motivo

El backend actual se ejecuta en Render Oregon (`gcp-us-west1`), una region
admitida por Gemini. Sin embargo, Google rechaza su IP IPv4 compartida de
salida con:

```text
User location is not supported for the API use.
```

La comprobacion autenticada `POST /api/ai-grading/status/check` confirmo que
el rechazo ocurre incluso con una solicitud minima, sin PDF ni datos de
alumnos. Tambien se comprobo con `gemini-3.5-flash` y `gemini-2.5-flash`.

La solucion preparada crea solamente un backend nuevo en Render Virginia.
La base PostgreSQL y el backend actual se conservan hasta terminar el QA.

## Antes de migrar

1. Revocar la API key de Gemini mostrada en capturas y crear una nueva.
2. Rotar la contrasena PostgreSQL mostrada en capturas.
3. No eliminar ni suspender el backend actual.
4. Obtener desde la base de datos de Render:
   - External Database URL.
   - Username.
   - Password nuevo.

Virginia no comparte la red privada de Oregon. Por eso el servicio nuevo debe
usar el hostname de la **External Database URL**, no el hostname interno
`dpg-...-a`.

La URL para Spring debe tener este formato:

```text
jdbc:postgresql://HOST_EXTERNO:5432/NOMBRE_BASE
```

El usuario y la contrasena se cargan por separado.

## Crear el servicio nuevo

1. En Render, abrir `Blueprints`.
2. Seleccionar `New Blueprint Instance`.
3. Elegir el repositorio `CarlaGorgal2002/7Test` y la rama `main`.
4. En `Blueprint Path`, escribir:

```text
render-virginia.yaml
```

5. Completar los secretos solicitados:

| Variable | Valor |
|---|---|
| `GEMINI_API_KEY` | API key nueva de Google AI Studio |
| `SPRING_DATASOURCE_URL` | URL JDBC con hostname externo |
| `SPRING_DATASOURCE_USERNAME` | Usuario PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | Contrasena PostgreSQL nueva |

6. Aprobar el Blueprint y esperar el despliegue de
   `7test-backend-virginia`.

## Verificar antes de cambiar Vercel

1. Confirmar que `https://NUEVO-BACKEND.onrender.com/v3/api-docs` responde
   HTTP `200`.
2. Iniciar sesion como profesor contra el backend nuevo.
3. Ejecutar `POST /api/ai-grading/status/check`.
4. Confirmar:

```json
{
  "available": true,
  "message": "Gemini respondio correctamente desde el backend."
}
```

5. Generar una sugerencia controlada y confirmar trabajo `COMPLETED` y
   sugerencia `READY`.

## Conectar Vercel

1. En el proyecto frontend de Vercel, reemplazar `VITE_API_URL` por la URL
   del backend nuevo, sin `/api` al final.
2. Redeployar producción.
3. Ejecutar el recorrido docente completo.
4. Mantener el backend anterior hasta comprobar autenticacion, datos,
   correccion manual y correccion con IA.
