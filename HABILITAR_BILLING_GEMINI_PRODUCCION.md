# Habilitar Gemini Paid Tier en produccion

## Por que es necesario

El backend de produccion recibe de Google:

```text
400 FAILED_PRECONDITION: User location is not supported for the API use.
```

La guia oficial de troubleshooting de Gemini indica habilitar billing en el
proyecto de Google AI Studio cuando el Free Tier no esta disponible desde la
ubicacion detectada para la solicitud.

Documentacion oficial:

- https://ai.google.dev/gemini-api/docs/troubleshooting
- https://ai.google.dev/gemini-api/docs/billing

## Pasos

1. Abrir https://aistudio.google.com/api-keys.
2. Revocar la API key que fue visible en capturas.
3. En el proyecto `7Test`, pulsar `Set up billing`.
4. Vincular o crear una cuenta de facturacion.
5. Completar el flujo mostrado por Google AI Studio.
6. Si Google asigna el plan Prepay, comprar el minimo solicitado. Desde el
   23 de marzo de 2026, para cuentas nuevas suele ser un minimo de USD 10 o
   equivalente.
7. Confirmar que el proyecto figure como `Paid`, no `Free tier`, y que tenga
   saldo positivo si utiliza Prepay.
8. Crear una API key nueva dentro de ese proyecto Paid.
9. En Render, reemplazar `GEMINI_API_KEY` por la clave nueva.
10. Mantener:

```text
GEMINI_ENABLED=true
GEMINI_MODEL=gemini-3.5-flash
```

11. Pulsar `Save, rebuild, and deploy`.

## Verificacion

Despues del redeploy, iniciar sesion como profesor y ejecutar:

```text
POST /api/ai-grading/status/check
```

La respuesta correcta es:

```json
{
  "enabled": true,
  "available": true,
  "model": "gemini-3.5-flash",
  "message": "Gemini respondio correctamente desde el backend."
}
```

Luego debe generarse una sugerencia controlada y comprobar:

```text
Trabajo: COMPLETED
Sugerencia: READY
```

## Plan alternativo

Si Google AI Studio ya muestra el proyecto como Paid, existe saldo positivo,
la clave nueva esta cargada en Render y el chequeo sigue devolviendo rechazo
de ubicacion, seguir `MIGRACION_RENDER_VIRGINIA_GEMINI.md`.
