# Migracion de Correccion IA a OpenAI

La correccion tentativa utiliza OpenAI Responses API con `gpt-5.4-mini`.
Gemini fue retirado del backend.

## Seguridad

- La API key existe unicamente como secreto de backend.
- Toda clave compartida por chat, captura o commit debe revocarse inmediatamente.
- No se envian nombres, emails ni identificadores personales.
- Las respuestas se procesan con `store: false`.
- La respuesta del alumno no participa en la seleccion de fuentes.

## Contexto y costo

El PDF oficial se indexa localmente. Cada pregunta envia como maximo ocho paginas relevantes,
seleccionadas a partir del enunciado, la respuesta modelo y los criterios docentes.
OpenAI solo puede citar paginas incluidas en esa seleccion.

## Variables de Render

```text
OPENAI_ENABLED=true
OPENAI_API_KEY=<clave nueva no compartida>
OPENAI_MODEL=gpt-5.4-mini
OPENAI_MAX_RELEVANT_PAGES=8
OPENAI_MAX_CHARACTERS_PER_PAGE=6000
```

Eliminar de Render las variables `GEMINI_ENABLED`, `GEMINI_API_KEY` y `GEMINI_MODEL`
una vez verificado el despliegue.

## Verificacion

1. Iniciar sesion como profesor.
2. Ejecutar `POST /api/ai-grading/status/check`.
3. Confirmar que el modelo informado sea `gpt-5.4-mini`.
4. Generar sugerencias sobre una entrega controlada.
5. Confirmar que la correccion manual continúe funcionando si OpenAI no tiene creditos.
