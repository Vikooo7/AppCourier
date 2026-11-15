# RutaLog Perú · App Courier

Tercera app del proyecto final (caso 14). La usa el personal que **recoge y entrega en la calle**: ve su ruta del día ordenada, escanea la guía al recoger y entrega con foto, firma, nombre y DNI de quien recibe. Todo funciona **sin señal** y se sincroniza al recuperarla.

```
App Courier (Compose · MVVM · Room) → API RutaLog (Vercel) → Neon (PostgreSQL)
```

La app nunca habla con el banco ni con la base de datos: todo pasa por la API.

## Requisitos que cubre

| Requisito | Cómo se cumple |
|---|---|
| RF-C01 Ruta del día | Paradas ordenadas con ML-04 (vecino más cercano + 2-opt) en el mapa (OpenStreetMap) y en lista |
| RF-C02 Reordenar | Flechas para subir o bajar cada parada; el orden manual se guarda en el servidor |
| RF-C03 Recojo | Escanear la guía (o escribirla): el envío pasa a RECOGIDO con hora y GPS; si no es la guía de la parada, avisa |
| RF-C04 Entrega | Foto, firma en pantalla, nombre y DNI de quien recibe; sin foto y firma no se cierra (RN-03) |
| RF-C05 Contraentrega | Si el envío cobra contraentrega, no se entrega sin cobro confirmado (RN-02). El cobro con QR llega en la fase de pagos |
| RF-C07 Entrega fallida | Motivo y foto de la fachada; al tercer intento el envío vuelve al remitente (RN-05) |
| RF-C08 Offline | Cola local con UUID y hora real de cada registro (RN-06); WorkManager la envía al volver la señal |
| Notificaciones | Aviso cuando el operador asigna paradas nuevas (revisión cada 15 minutos) |

## Estructura (MVVM)

```
com.example.rutalogcourier
├── data/
│   ├── local/        Room: usuarios, paradas, resumen_ruta, operaciones_pendientes, sync_metadata
│   ├── remote/       Retrofit: ApiService, DTOs y clasificación de respuestas (ResultadoApi)
│   └── repository/   AuthRepository, RutaRepository (registra en Room + cola), SyncRepository
├── di/               AppContainer
├── model/            Parada, EstadoEnvio, ReglasCourier (las mismas reglas de la API)
├── ui/
│   ├── components/   MapaRuta (osmdroid), PanelFirma, Cámara, componentes comunes
│   ├── navigation/   AppNavigation
│   └── screens/      Splash, Login, Ruta del día, Parada, Entrega, Fallida, Sincronizar
├── util/             GPS, escáner de guías, compresión de imágenes, notificaciones
├── viewmodel/        AuthViewModel, RutaViewModel, SyncViewModel
└── worker/           SyncWorker
```

## Librerías propias de esta app

| Librería | Para qué |
|---|---|
| `osmdroid` | Mapa de OpenStreetMap, gratis y sin clave |
| `play-services-location` | GPS de cada recojo, entrega o fallida |
| `play-services-code-scanner` | Escanear la guía. Trae su propia pantalla de cámara: la app no pide permiso de cámara |

La foto se toma con la app de cámara del teléfono y se comprime (máximo 1280 px y ~350 KB) antes de guardarla.

## Cuentas de prueba

`courier1@rutalog.pe` … `courier10@rutalog.pe` (clave `1234`). Cada courier ve solo los envíos que el operador le asignó desde la App Admin.

## Pruebas

- `gradlew testDebugUnitTest`: reglas del courier y clasificación de respuestas de la API.
- En la API: `pruebas/probar-courier.js` recorre todo el flujo (ruta, reorden, recojo, entrega, fallida, fotos y permisos).
