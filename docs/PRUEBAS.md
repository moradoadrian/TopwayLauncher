# Procedimientos de prueba

Hay dos formas de llevar el APK a la radio. **Ninguna debe ejecutarse sin la autorización
del propietario.** Los comandos están separados en *diagnóstico (solo lectura)* e
*instalación (modifica el dispositivo)*.

> Estado actual (2026-10-26): no se ha conectado ni instalado nada en la radio. Su puerto
> USB funciona como host (lee memorias) y al conectarla a un PC **no aparece ningún
> dispositivo**, así que no se puede asumir ADB por USB.

---

## A. ADB por Wi-Fi

### A.1 Requisitos en el PC

```powershell
adb version          # Debe mostrar "Android Debug Bridge version 1.0.41" o superior
adb devices -l       # Lista vacía si no hay nada conectado
```

ADB está en `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.

### A.2 ¿Tiene la radio depuración inalámbrica?

1. **Ajustes → Acerca del dispositivo / Información → Número de compilación**: tocar 7 veces
   para mostrar las *Opciones de desarrollador* (es reversible y no altera el sistema).
   Algunos firmwares de radios lo ocultan o lo protegen con contraseña; si es así, **no**
   probar contraseñas de "ajustes de fábrica": esos menús cambian parámetros del vehículo.
2. En **Ajustes → Sistema → Opciones de desarrollador**, buscar:
   - **Depuración inalámbrica** (*Wireless debugging*): existe solo en **Android 11 o
     superior**. Si aparece → seguir A.3.
   - **Depuración USB** solamente: en Android 10 o inferior la conexión Wi-Fi requiere un
     primer `adb tcpip 5555` hecho **por USB**, que hoy no es posible → usar el método B.
   - Algunos firmwares de radios incluyen una opción propia tipo "ADB por red" o "modo USB
     dispositivo/OTG". Anotar el nombre exacto y consultarlo antes de activarlo.
3. Si no hay opciones de desarrollador accesibles → método B.

> El menú *Acerca del dispositivo* de algunas radios muestra una versión de Android
> distinta a la real. La versión fiable es la que muestra TopwayLauncher en su línea de
> diagnóstico (lee `Build.VERSION`) o `getprop` por ADB.

### A.3 Emparejar y conectar (Android 11+)

Radio y PC en la **misma red Wi-Fi** (por ejemplo, el punto de acceso del teléfono).

1. Radio: *Depuración inalámbrica* → activar → **Vincular dispositivo con código de
   vinculación**. Muestra `IP:PUERTO_DE_VINCULACIÓN` y un código de 6 dígitos.
2. PC:
   ```powershell
   adb pair 192.168.X.Y:PUERTO_DE_VINCULACIÓN     # pide el código de 6 dígitos
   ```
3. En la pantalla principal de *Depuración inalámbrica* aparece otro puerto (el de conexión):
   ```powershell
   adb connect 192.168.X.Y:PUERTO_DE_CONEXIÓN
   adb devices -l
   ```

El puerto de conexión cambia cada vez que se reactiva la depuración inalámbrica.

### A.4 Diagnóstico — SOLO LECTURA

No modifican nada. Su salida es la información que falta confirmar:

```powershell
adb shell getprop ro.build.version.release      # Versión de Android
adb shell getprop ro.build.version.sdk          # Nivel de API
adb shell getprop ro.product.manufacturer
adb shell getprop ro.product.model
adb shell getprop ro.board.platform             # Plataforma / SoC
adb shell getprop ro.product.cpu.abilist        # Arquitecturas soportadas
adb shell wm size                               # Resolución
adb shell wm density                            # Densidad
adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
                                                # Launcher actual (anotar para poder volver)
adb shell pm list packages -s                   # Paquetes del sistema (inventario)
```

`cmd package resolve-activity` existe en Android 7+; si falla, usar
`adb shell dumpsys package r activity` y buscar `android.intent.category.HOME`.

### A.5 Instalación — MODIFICA EL DISPOSITIVO (requiere autorización)

```powershell
adb install -r app\build\outputs\apk\debug\app-debug.apk   # Instalar / actualizar
adb shell am start -n com.morado.topwaylauncher/.MainActivity  # Abrir
adb uninstall com.morado.topwaylauncher                    # Desinstalar
```

### A.6 Al terminar

```powershell
adb disconnect
```
Y desactivar *Depuración inalámbrica* en la radio.

---

## B. Instalación manual por APK (memoria USB)

No necesita ADB. Es la vía recomendada mientras no se confirme la depuración inalámbrica.

### B.1 Generar el APK (PC)

```powershell
cd C:\Users\cesar.morado\Documents\Kotlin\TopwayLauncher
.\gradlew.bat assembleDebug
```

Resultado: `app\build\outputs\apk\debug\app-debug.apk` (~11 MB). Está firmado con la clave
de depuración local de Android Studio (`%USERPROFILE%\.android\debug.keystore`), que **no**
se sube al repositorio.

### B.2 Copiar a la memoria

1. Memoria USB en **FAT32** (la que más radios leen; exFAT/NTFS pueden no reconocerse).
2. Copiar `app-debug.apk` a la raíz y renombrarlo, por ejemplo `TopwayLauncher-0.1.0.apk`.
3. Expulsar la memoria de forma segura.

### B.3 Instalar en la radio (requiere autorización)

1. Conectar la memoria a la radio.
2. Abrir el **explorador de archivos** de la radio y tocar el APK.
3. Si Android bloquea la instalación:
   - Android 8+: permitir *Instalar apps desconocidas* **solo para el explorador de
     archivos** (Ajustes → Aplicaciones → Acceso especial). Revocarlo después.
   - Android 7 o inferior: *Ajustes → Seguridad → Orígenes desconocidos*. Desactivarlo después.
4. Abrir **TopwayLauncher** desde el cajón de aplicaciones. Se comporta como una app normal.

### B.4 Qué anotar en la primera prueba

- La línea inferior izquierda de TopwayLauncher: versión de Android, API, resolución,
  densidad, fabricante, modelo y placa. Una foto basta.
- Si la lista de aplicaciones aparece completa y si abrir una app funciona.
- Si el reloj muestra la hora y la fecha correctas.

### B.5 Desinstalar

*Ajustes → Aplicaciones → TopwayLauncher → Desinstalar*.

---

## Si ninguna vía funciona

- No hay explorador de archivos: algunas radios instalan APK desde su propio gestor de
  "Actualización" o "USB"; **no usar opciones de actualización de firmware/MCU** para esto.
- Alternativas seguras: compartir el APK desde un teléfono por Bluetooth o descargarlo
  desde un enlace con el navegador de la radio (ambas acaban en el mismo instalador).
- No se contemplan rooteo, desbloqueo de bootloader ni herramientas que escriban particiones.
