# Modo launcher (preparado, desactivado)

TopwayLauncher **no** se ofrece todavía como pantalla de inicio. La configuración está
lista en `app/src/main/AndroidManifest.xml`, pero apagada.

## Cómo está preparado

| Pieza | Dónde | Estado |
|---|---|---|
| Actividad principal con `MAIN` + `LAUNCHER` | `MainActivity` | Activa (app normal) |
| `launchMode="singleTask"`, `stateNotNeeded="true"` | `MainActivity` | Activo; no cambia el uso normal |
| Filtro `MAIN` + `HOME` + `DEFAULT` | `<activity-alias android:name=".HomeAlias">` | **`android:enabled="false"`** |
| Visibilidad de paquetes (Android 11+) | `<queries>` con `MAIN`/`LAUNCHER` | Activa; sin `QUERY_ALL_PACKAGES` |
| Listado de apps | `PackageManager.queryIntentActivities` | API pública |
| Apertura de apps | `Intent` explícito + `startActivity` | API pública |

No se usan root, API privadas ni permisos especiales.

## Antes de activarlo

1. Haber probado el prototipo como app normal en la radio (`docs/PRUEBAS.md`).
2. Conocer el launcher original (con ADB:
   `adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME`).
3. Tener una vía para desinstalar TopwayLauncher si algo falla (Ajustes de Android o ADB).
4. Comprobar que el launcher original de la radio no aloja funciones que se perderían
   (widgets de radio FM, accesos a cámara de reversa, etc.).

## Activarlo (fase futura)

1. Cambiar `android:enabled="false"` a `"true"` en `HomeAlias`.
2. Recompilar e instalar la nueva versión.
3. Pulsar **Inicio**. Android mostrará un selector:
   - **Solo una vez**: recomendado para la primera prueba.
   - **Siempre**: lo deja como predeterminado.
4. O elegirlo en **Ajustes → Aplicaciones → Aplicaciones predeterminadas → Aplicación de
   pantalla de inicio** (el nombre exacto varía según la versión de Android y el firmware).

La app nunca se fuerza como predeterminada: la elección siempre es manual.

## Volver al launcher original

Cualquiera de estas opciones:

- **Ajustes → Aplicaciones → Aplicaciones predeterminadas → Aplicación de pantalla de
  inicio** → elegir el launcher original.
- **Ajustes → Aplicaciones → TopwayLauncher → Abrir de forma predeterminada → Borrar
  valores predeterminados**; al pulsar Inicio vuelve a aparecer el selector.
- **Desinstalar TopwayLauncher**: Android vuelve automáticamente al launcher del sistema.

> Si el firmware de la radio oculta estos menús, la desinstalación sigue siendo posible
> desde *Ajustes → Aplicaciones* porque TopwayLauncher es una app de usuario.

## Mejoras posibles

- Interruptor dentro de la app que active/desactive `HomeAlias` con
  `PackageManager.setComponentEnabledSetting` (API pública; una app solo puede cambiar sus
  propios componentes).
- Bloquear el botón Atrás cuando la app actúa como pantalla de inicio.
