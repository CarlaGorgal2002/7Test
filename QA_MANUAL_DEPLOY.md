# Entorno QA Manual

Este entorno corresponde a la version anterior a la integracion de correccion
con IA. Esta aislado de produccion:

- Rama: `qa-manual-only`
- Backend Render: `7test-qa-manual-backend`
- Frontend Vercel: `7test-qa-manual`
- Persistencia: H2 propia del servicio QA

Despliegue directo del backend:

`https://render.com/deploy?repo=https://github.com/CarlaGorgal2002/7Test/tree/qa-manual-only`

## Credenciales QA anteriores

| Rol | Usuario | Contrasena |
|---|---|---|
| Administrador | `admin@seventest.local` | `Admin#7T$2026` |
| Profesor | `pfarias@uade.edu.ar` | `PabloFarias123` |
| Alumno | `cgorgal@uade.edu.ar` | `CarlaGorgal123` |

La rama contiene exactamente los 45 pares de cuentas antiguas de alumno y
profesor solicitados para QA: 90 credenciales en total.

## Aislamiento

Las credenciales anteriores solo funcionan contra el backend QA. Produccion
continua usando su backend y su base de datos actuales.

Auditoria del 10 de junio de 2026:

- Las 90 credenciales solicitadas coinciden con las semillas del backend QA.
- Se probaron las 90 credenciales contra produccion.
- Produccion rechazo las 90 credenciales antiguas.

El plan gratuito de Render usa almacenamiento efimero. Las cuentas semilla se
recrean automaticamente, pero datos creados durante pruebas pueden perderse en
un redeploy.
