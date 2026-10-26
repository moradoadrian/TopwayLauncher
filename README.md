# TopwayLauncher

Launcher Android propio para la radio de automóvil **Topway TS7** (placa **SL8141E**),
hecho con Kotlin y Jetpack Compose. Pensado para pantallas horizontales, legible a
distancia y sin depender de root ni de API privadas.

> **Estado:** prototipo 0.1.0. Funciona como aplicación normal; **todavía no** reemplaza
> el launcher del sistema. No se ha probado aún en la radio.

## Características actuales

- Reloj y fecha grandes, en el idioma y formato (12/24 h) del sistema.
- Cuadrícula adaptable de aplicaciones instaladas, ordenadas alfabéticamente
  (sin distinguir mayúsculas ni acentos).
- Diseño oscuro; reloj a la izquierda en horizontal y arriba en vertical.
- Estados de carga, lista vacía y error con botón *Reintentar*.
- Aviso si una aplicación no se puede abrir.
- La lista se actualiza al volver a la pantalla (apps instaladas/desinstaladas).
- Línea de diagnóstico: versión real de Android, API, resolución, densidad, fabricante,
  modelo y placa (útil para confirmar las especificaciones de la radio sin ADB).
- Modo launcher **preparado y desactivado** (ver [docs/MODO_LAUNCHER.md](docs/MODO_LAUNCHER.md)).
- **Sin permisos** de usuario.

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
│   ├── AppRepository.kt         Consulta y apertura de apps (PackageManager)
│   └── DeviceInfo.kt            Datos del equipo para diagnóstico
└── ui/
    ├── home/                    HomeScreen + HomeViewModel (estados)
    ├── components/              ClockPanel, AppGrid
    └── theme/                   Tema oscuro
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
- Sin widgets, búsqueda, favoritos, carpetas ni reordenación de iconos.
- La lista de apps se refresca al volver a la pantalla, no en tiempo real.
- No se integra con funciones propias de la radio (MCU, FM, cámara, controles de volante).

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
