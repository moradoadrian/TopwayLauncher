# TopwayLauncher

Launcher Android propio para la radio de automóvil **Topway TS7** (placa **SL8141E**),
hecho con Kotlin y Jetpack Compose. Pensado para pantallas horizontales, legible a
distancia y sin depender de root ni de API privadas.

> **Estado:** prototipo 0.1.0. Funciona como aplicación normal; **todavía no** reemplaza
> el launcher del sistema. No se ha probado aún en la radio.

## Características actuales

- Interfaz multimedia horizontal, minimalista y monocromática, inspirada en la organización
  de CarPlay (sin copiar su interfaz ni usar recursos de Apple):
  - **Barra lateral** (~10 % del ancho) con iconos monocromáticos: Inicio, Mapas, Música,
    Apps y, separado abajo, **Ajustes**. La sección activa se marca con un paso de tono.
  - **Cabecera** con hora y fecha.
  - **Tarjeta de Navegación** protagonista: abre **Google Maps** con su URL pública de mapa
    (sin destino, sin navegación giro a giro, sin API key y sin permisos de ubicación). Si
    Maps no está instalado, usa la app de mapas predeterminada; si no hay ninguna, lo indica.
  - **Tarjeta de Música**: si el usuario lo permite, muestra lo que suena (app, título,
    artista y carátula, tal como los publica la app de música) con los controles anterior,
    reproducir/pausar y siguiente. Si no lo permite, o no suena nada, abre la app de música
    predeterminada. Tocar la tarjeta abre la app que está sonando.
  - **Accesos rápidos**: apps recientes (o, al principio, las predeterminadas), solo las que
    caben enteras, y el acceso **Todas** a la lista completa.
  - **Sección Apps**: todas las apps en páginas que se deslizan; columnas, filas y tamaño
    de icono según la pantalla real. Atrás vuelve a Inicio.
  - Paleta grafito centralizada en `ui/theme/Theme.kt`; el color lo ponen los iconos de las apps.
- Apps ordenadas alfabéticamente (sin distinguir mayúsculas ni acentos).
- Respuesta táctil al pulsar (iconos y tarjetas se encogen ligeramente).
- Estados de carga, lista vacía y error con botón *Reintentar*.
- Aviso si una aplicación, Maps, la app de música o Ajustes no se pueden abrir.
- La lista se actualiza al volver a la pantalla (apps instaladas/desinstaladas).
- **Información del equipo** al mantener pulsada la hora: versión real de Android, API,
  resolución, densidad, modelo y placa (para confirmar las especificaciones de la radio sin ADB).
- Modo launcher **preparado y desactivado** (ver [docs/MODO_LAUNCHER.md](docs/MODO_LAUNCHER.md)).
- **Sin permisos** que se pidan al instalar. El único acceso es opcional (ver abajo).

### Mostrar lo que suena (opcional)

Leer qué reproduce otra app exige que el usuario dé a TopwayLauncher el **acceso a
notificaciones**. Es una decisión del usuario, no se activa sola:

1. En la tarjeta de Música, tocar **Mostrar lo que suena**. Un diálogo explica para qué se usa.
2. **Ir a Ajustes** abre la pantalla del sistema; ahí se activa el interruptor de TopwayLauncher
   y se confirma el aviso de Android.
3. Al volver, la tarjeta muestra lo que suena.

El servicio que lo hace posible (`MediaListenerService`) está vacío: no lee, guarda ni envía
notificaciones; solo habilita la API pública de sesiones multimedia (`MediaSessionManager`).
Para retirarlo: *Ajustes → Notificaciones → Acceso a notificaciones* (el nombre varía según
el fabricante) y desactivar TopwayLauncher. La tarjeta vuelve a ser un acceso directo.

## Tecnologías

| Componente | Versión |
|---|---|
| Kotlin (plugin compilador Compose) | 2.3.20 |
| Android Gradle Plugin | 9.1.0 (Kotlin integrado) |
| Gradle (wrapper) | 9.3.1 |
| Jetpack Compose BOM | 2026.05.00 (Material 3) |
| compileSdk / targetSdk | 36 |
| minSdk | 23 (Android 6.0) |
| JDK para compilar | 17 o superior |

Las versiones se eligieron porque ya estaban en la caché local de Android Studio y
compilan juntas; hay versiones más recientes disponibles (lint las lista como avisos).

## Requisitos de desarrollo

- Android Studio reciente (probado con la build 2026.1, `AI-261`).
- Android SDK Platform 36 y Build-Tools 36.
- JDK 17+ (sirve el JBR que incluye Android Studio).
- Git.

## Estructura

```
app/src/main/java/com/morado/topwaylauncher/
├── MainActivity.kt              Actividad única con Compose
├── data/
│   ├── AppInfo.kt               Modelos (AppInfo, LauncherApp)
│   ├── AppListRules.kt          Filtrado/orden puro (con pruebas)
│   ├── DockRules.kt             Reglas del dock y recientes (con pruebas)
│   ├── RecentAppsStore.kt       Apps recientes guardadas localmente
│   ├── AppRepository.kt         Apps, Google Maps, música y Ajustes (API públicas)
│   ├── ExternalShortcut.kt      Acceso verificado a una app externa
│   ├── MediaSessionRepository.kt  Lo que suena y sus controles (sesiones multimedia)
│   ├── MediaListenerService.kt  Servicio vacío que habilita esa lectura
│   ├── NowPlaying.kt            Modelo y regla de qué sesión mostrar (con pruebas)
│   └── DeviceInfo.kt            Datos del equipo para diagnóstico
└── ui/
    ├── home/                    HomeScreen, DashboardContent, AppsContent, HomeViewModel
    ├── components/              NavRail, ClockHeader, AppPager, AppIcon
    └── theme/                   Paleta, dimensiones y tipografía
res/drawable/ic_nav_*.xml        Iconos monocromáticos propios (sin dependencias)
docs/
├── PRUEBAS.md                   ADB por Wi-Fi e instalación por USB
└── MODO_LAUNCHER.md             Activar / revertir el modo launcher
```

## Compilar el APK

```powershell
.\gradlew.bat assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.

Pruebas y análisis:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug
```

En Linux/macOS usar `./gradlew`. Al abrir el proyecto, Android Studio crea `local.properties`
con la ruta del SDK (no se versiona).

## Ejecutar en el emulador desde Android Studio

1. **File → Open** y elegir la carpeta `TopwayLauncher`; esperar a que termine la
   sincronización de Gradle.
2. En **Device Manager**, iniciar un emulador. Sirve cualquier imagen con Google Play (trae
   Google Maps y YouTube Music, que permiten probar las tarjetas). Rotarlo a **horizontal**
   con los botones de rotación de la barra del emulador; no hace falta cambiar ajustes.
3. Elegir la configuración **app** y pulsar **Run ▶**. La app se instala y se abre como una
   app normal (no reemplaza el launcher del emulador).
4. Para ver el diseño en otras proporciones, redimensionar la ventana del emulador o usar
   otro perfil de dispositivo (por ejemplo, una tablet de 10" en horizontal).

Si el emulador deja de responder a los toques o la captura de pantalla sale negra tras
muchos reinicios de la app, reiniciar el emulador (*Cold Boot*) suele resolverlo: en las
pruebas ocurrió por un bloqueo del launcher de Pixel del propio emulador, no de esta app.

## Pruebas en la radio

Ver [docs/PRUEBAS.md](docs/PRUEBAS.md):

- **A. ADB por Wi-Fi**: solo si la radio tiene *Depuración inalámbrica* (Android 11+).
- **B. APK por memoria USB**: no requiere ADB; vía recomendada por ahora.

## Compatibilidad conocida y limitaciones

**Confirmado en el PC de desarrollo:** compila, las pruebas unitarias pasan y lint no
reporta errores.

**Pendiente de confirmar en la radio:**

- Versión de Android y nivel de API reales.
- Resolución y densidad de pantalla.
- Arquitectura del procesador.
- Si existe depuración inalámbrica u opciones de desarrollador accesibles.
- Si el puerto USB puede trabajar en modo dispositivo para ADB (hoy no se detecta en el PC).
- Si el firmware permite elegir otro launcher predeterminado.

**Limitaciones del prototipo:**

- No reemplaza el launcher del sistema.
- **Lo que suena depende de la app de música.** Solo aparecen las apps que publican una
  sesión multimedia estándar (YouTube Music, Spotify y la mayoría de reproductores). Las apps
  de fábrica de la radio (FM, Bluetooth, USB) pueden no hacerlo; si es así, la tarjeta sigue
  siendo un acceso directo. Probado solo en el emulador.
- **El acceso a notificaciones puede no estar disponible en la radio.** En equipos marcados
  como de poca memoria Android lo bloquea, y algunos fabricantes ocultan esa pantalla (en ese
  caso el botón no aparece). En Android 13+ las apps instaladas desde un archivo APK pueden
  tener este ajuste *restringido*: ver [docs/PRUEBAS.md](docs/PRUEBAS.md).
- Si se retira el acceso con la pantalla de inicio abierta (por ejemplo, por ADB), la
  tarjeta lo refleja al volver a ella.
- **Mapas y Música dependen de las apps predeterminadas** del sistema. Si la radio no tiene
  una app de música elegida (o sus apps de fábrica no se declaran como tales), la tarjeta
  ofrece elegirla desde la lista de apps.
- La tarjeta de Navegación es un acceso directo: no hay mapa integrado (no se usa ningún SDK).
- La cabecera no muestra información contextual (clima, conexión); el espacio está libre.
- Sin widgets, búsqueda, favoritos fijados, carpetas ni reordenación de iconos.
- La lista de apps se refresca al volver a la pantalla, no en tiempo real.
- No se integra con funciones propias de la radio (MCU, FM, cámara, controles de volante).
- El emulador (Android 15, API 35) no reproduce el sistema de la Topway: versión de Android,
  rendimiento, resolución y apps de fábrica pueden ser distintos.

## Cómo contribuir

1. Abrir un *issue* describiendo el cambio o el problema (incluir versión de Android y
   modelo si es un fallo en la radio).
2. Crear una rama desde `main`.
3. Mantener el estilo existente; ejecutar `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug`.
4. Abrir un *pull request* explicando qué se probó y en qué dispositivo.

No incluir claves de firma, contraseñas ni credenciales.

## Licencia

Distribuido bajo la licencia [MIT](LICENSE).

## Aviso

Proyecto independiente, sin relación con Topway. No modifica el firmware, no requiere root
y no cambia ajustes de fábrica.
