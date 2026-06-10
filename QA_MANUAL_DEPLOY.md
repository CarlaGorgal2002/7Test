# Entorno QA Manual

Este entorno corresponde a la version anterior a la integracion de correccion
con IA. Esta aislado de produccion:

- Rama: `qa-manual-only`
- Backend Render: `7test-qa-manual-backend`
- Frontend Vercel: `7test-qa-manual`
- Persistencia: H2 propia del servicio QA

## Credenciales QA anteriores

| Rol | Usuario | Contrasena |
|---|---|---|
| Administrador | `admin@seventest.local` | `Admin#7T$2026` |
| Profesor | `pfarias@uade.edu.ar` | `PabloFarias123` |
| Alumno | `cgorgal@uade.edu.ar` | `CarlaGorgal123` |

La rama contiene ademas las restantes cuentas de prueba anteriores.

## Aislamiento

Las credenciales anteriores solo funcionan contra el backend QA. Produccion
continua usando su backend y su base de datos actuales.

El plan gratuito de Render usa almacenamiento efimero. Las cuentas semilla se
recrean automaticamente, pero datos creados durante pruebas pueden perderse en
un redeploy.
