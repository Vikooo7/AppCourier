# AGENTS.md · RutaLog Courier

Reglas para los agentes de IA que trabajen en este proyecto (Gemini Agent Mode en Android Studio, Claude Code, Codex). Gemini lo lee en cada consulta; se sube a Git para que todo el equipo comparta las reglas.

## Qué es

App del **repartidor** de RutaLog Perú (caso 14): muestra la ruta del día ordenada (ML-04) con el riesgo de cada parada (ML-01), escanea la guía al recoger, registra la entrega con foto y firma, cobra la contraentrega con QR de Banco Andino y registra las visitas fallidas.

## Arquitectura (no se cambia sin aprobación)

- Kotlin + Jetpack Compose + Material 3, patrón **MVVM**:
  `ui/screens` → `viewmodel` → `data/repository` → `data/local` (Room) y `data/remote` (Retrofit).
- Dependencias creadas a mano en `di/AppContainer.kt` (sin Hilt).
- La app **solo habla con la API** (`https://app-api-rutalog.vercel.app`). Nunca se conecta directo a la base de datos.
- Primero sin conexión: cada recojo, entrega o fallida se guarda en Room y en una cola; `worker/SyncWorker` la envía apenas hay señal (trabajo urgente de WorkManager). Cada operación lleva `uuidOperacion`.
- Al enviarse cada registro, la API avisa al remitente por push: no quites el envío inmediato.

## Reglas para el agente

1. Antes de editar, **muestra el plan** y la lista de archivos que vas a tocar. No edites sin preguntar.
2. Nombres de clases, funciones, variables y textos de pantalla **en español**, como el resto del código.
3. No cambies la arquitectura MVVM, no agregues librerías y no cambies versiones de Gradle sin preguntar.
3b. Room: **no modifiques** migraciones existentes ni los JSON de `app/schemas`. Si cambia una entidad, sube la versión
    de la base, agrega la `Migration` y deja que Room exporte el esquema nuevo.
4. La URL de la API no se escribe en el código: se pasa al compilar con `-PurlApi=http://...` (pruebas en la PC).
5. Nunca leas ni copies claves: `local.properties`, keystore, `.env`, `google-services.json` (ver `.aiexclude`).
6. Una tarea está terminada solo si **compila y pasan todos los tests**.
7. Commits: una sola línea de título en español, sin cuerpo.

## Compilar y probar

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest
```

## Valores de control

Si un cambio altera alguno de estos resultados, el cambio está mal:

- `ReglasCourier.siguienteEstado`: pendiente → recogido → en_transito → en_reparto; reprogramado → en_reparto.
- Una guía escaneada que no coincide con la parada devuelve el rechazo `GUIA_NO_COINCIDE` (HTTP 409).
- `ReglasCourier.estadoTrasFallida(0)` = `FALLIDO` (aún quedan intentos).
