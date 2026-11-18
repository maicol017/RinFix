# servidor/ — Funciones de RinFix en Vercel

Aquí vive la parte de RinFix que habla con Claude. La app nunca tiene la llave de Claude:
le pide al servidor y el servidor llama a Claude con la llave guardada en Vercel.

## Funciones

| Ruta | Qué hace |
| --- | --- |
| `GET /api/estado` | Dice si el servidor responde y si ya tiene las llaves puestas (sin mostrarlas). |
| `POST /api/analizar` | Recibe de 1 a 3 fotos del rin y devuelve los daños visibles. Modelo: Claude Sonnet 5.5. |
| `POST /api/asistente` | Conversación libre del asistente, con los datos de la persona. Modelo: Claude Haiku 5.5. |

Los precios no salen de aquí: la app los calcula con el catálogo del taller.

## Variables de entorno (en Vercel, nunca en el código)

| Nombre | Qué es |
| --- | --- |
| `ANTHROPIC_API_KEY` | La llave de la API de Claude (console.anthropic.com → API Keys). |
| `RINFIX_CLAVE_APP` | Clave que la app manda en cada solicitud. Una cadena larga al azar. |
| `MODELO_ANALISIS` | Opcional. Cambia el modelo del análisis (por defecto `claude-sonnet-5-5`). |
| `MODELO_ASISTENTE` | Opcional. Cambia el modelo del asistente (por defecto `claude-haiku-5-5`). |

Para crear `RINFIX_CLAVE_APP` en PowerShell:

```powershell
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 40 | ForEach-Object { [char]$_ })
```

## Publicar en Vercel

1. En vercel.com: **Add New → Project** e importa el repositorio de RinFix.
2. **Root Directory:** `servidor`. **Framework Preset:** Other.
3. En **Environment Variables** agrega `ANTHROPIC_API_KEY` y `RINFIX_CLAVE_APP`.
4. **Deploy.** Cada push a `main` vuelve a publicar solo.
5. Abre `https://<tu-proyecto>.vercel.app/api/estado`: debe decir `"configurado": true`.

## Protección

- La clave de la app frena el uso casual, pero va dentro del APK: no es secreta del todo.
- Cada función limita las solicitudes por IP (análisis: 12 cada 10 minutos; asistente: 40).
- Pon un **límite de gasto mensual** en la consola de Anthropic (Settings → Limits).
- Con Supabase, el servidor verificará además la sesión de cada persona.

## Revisar el código

```powershell
cd servidor
npm install
npm run revisar
```
