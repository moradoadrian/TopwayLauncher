# OBD-II: investigación, diseño y prueba

Estado (2026-10-27): **implementado y probado con pruebas unitarias y un adaptador simulado.
No se ha probado con un adaptador real ni con el vehículo.** Nada de lo que sigue sobre el
Nissan Altima 2007 está confirmado en el coche.

## Qué se sabe y qué no

| Tema | Estado |
|---|---|
| PID `01 0D` (velocidad): 1 byte, km/h, 0–255 | Estándar SAE J1979. Interpretación probada (`ObdResponseParserTest`). |
| Si el Altima 2007 informa el PID `0D` | **Sin confirmar.** La app lo pregunta al vehículo (`01 00`) y, si no lo admite, lo indica («El vehículo no informa velocidad»). |
| Protocolo del Altima 2007 | **Sin confirmar.** Muchos vehículos de esa época usan CAN (ISO 15765-4), pero no se encontró una fuente fiable para este modelo. La app usa detección automática (`ATSP0`) y muestra el protocolo que informa el adaptador (`ATDP`) en Ajustes y en Conducción. |
| RPM (`01 0C`) y refrigerante (`01 05`) | Interpretación implementada y probada. **No se consultan ni se muestran** hasta validar la velocidad en el vehículo. |
| Adaptadores BLE | No admitidos. Solo Bluetooth Classic con perfil serie (SPP), que es el que usan los ELM327 «Bluetooth» comunes. |

## Arquitectura

```
data/vehicle/
├── ObdResponseParser.kt   Interpretación pura de respuestas (sin Android, con pruebas)
├── Elm327Session.kt       Diálogo con el adaptador (ObdLink) y comandos permitidos
├── BluetoothObd.kt        Bluetooth Classic/SPP: emparejados, conexión cancelable, lectura con plazo
├── ObdConnectionManager.kt  Bucle: conectar → preparar → consultar; reintentos y timeouts
├── GpsSpeedSource.kt      Velocidad del GPS (LocationManager), opcional
├── SpeedSelector.kt       Regla de qué fuente mostrar y cuándo un dato es antiguo
├── VehicleSettingsStore.kt  Ajustes locales (adaptador, intervalo, GPS, demo)
├── VehicleTelemetryRepository.kt  Une las fuentes en un único VehicleState
└── VehicleTelemetry.kt    Modelos (VehicleState, ObdStatus, GpsStatus…)
ui/vehicle/VehicleViewModel.kt  Expone el estado a la interfaz (StateFlow)
```

Flujo: `VehicleViewModel.vehicle` es un `StateFlow` con `WhileSubscribed(5 s)`. Las fuentes solo
trabajan **mientras el launcher está visible** (y 5 s más, para que girar la pantalla no corte la
conexión). Con Google Maps u otra app delante, no se consulta el adaptador ni el GPS.

## Comandos que envía la app

Solo estos. Ninguno escribe en la ECU, borra códigos (modo 04) ni activa actuadores:

| Comando | Para qué |
|---|---|
| `ATZ` | Reiniciar el adaptador (no el vehículo) |
| `ATE0`, `ATL0`, `ATS0`, `ATH0` | Sin eco, sin saltos de línea, sin espacios, sin cabeceras |
| `ATSP0` | Protocolo automático |
| `0100` | Qué PID admite el vehículo (01–20) |
| `ATDP` | Nombre del protocolo detectado (informativo) |
| `010D` | Velocidad |

Una prueba (`Elm327SessionTest`) falla si se envía algo que no empiece por `AT` o `01`.

## Tiempos y errores

| Situación | Comportamiento |
|---|---|
| Intervalo de consulta | 1 s por defecto; configurable a 2 o 3 s. Sin bucles más rápidos. |
| Plazo por respuesta | 1,5 s (`ATZ` 3 s; primer `0100` 12 s, porque el adaptador busca el protocolo). |
| Lectura antigua | Una velocidad OBD deja de mostrarse tras `max(3 s, 3 × intervalo)` sin otra nueva: «--» y «Sin datos recientes». |
| 3 consultas seguidas sin dato (NO DATA, error, timeout) | Se cierra el canal y se reconecta: «El vehículo no responde». |
| No se puede abrir el canal Bluetooth | «No se pudo conectar»; reintento con espera creciente de 2 s a 30 s. |
| El adaptador no acepta los comandos AT | «El adaptador no responde». |
| El vehículo no admite el PID 0D | «El vehículo no informa velocidad»; nuevo intento al minuto. |
| Bluetooth apagado / sin permiso | Se indica y no se intenta conectar (se revisa cada 3 s). La app **no** enciende el Bluetooth. |
| Cancelación (sales de la app) | Se cierra el socket; `connect()` se interrumpe cerrándolo desde otra corrutina. |

Toda la E/S va en `Dispatchers.IO`. `BluetoothSocket.read()` no admite plazo, así que se
consulta `available()` cada 15 ms hasta el prompt `>` o hasta agotar el tiempo.

## Selección de fuente (OBD-II / GPS / demo)

Regla fija, sin promedios ni mezclas (`SpeedSelector`):

1. OBD-II si su última lectura es reciente.
2. Si no, GPS si su última lectura es reciente (≤ 4 s).
3. Si no, la demostración (solo en compilaciones de depuración y si se activa).
4. Si no, **velocidad desconocida («--»)**. Nunca se convierte en 0.

La tarjeta muestra siempre la fuente: `OBD-II`, `GPS` o `DEMO · simulado` (en ámbar).

## Requisitos para probar con el vehículo

- Adaptador **ELM327 Bluetooth Classic (SPP)**, v1.5 o superior. Muchos clones baratos
  responden mal a algunos comandos; si la app dice «El adaptador no responde», anotar el modelo.
- Emparejarlo antes en **Ajustes de Android → Bluetooth** (PIN habitual `1234` o `0000`).
- Contacto en ON (o motor encendido). Con el contacto apagado, la ECU no responde.

## Prueba en el vehículo

**Con el vehículo detenido y estacionado.** La app no debe manipularse mientras se conduce.

1. Conectar el adaptador al puerto OBD-II (bajo el tablero, lado del conductor) y poner el
   contacto en ON.
2. TopwayLauncher → **Ajustes → OBD-II por Bluetooth → Elegir** → conceder el permiso de
   Bluetooth (Android 12+) → elegir el adaptador.
3. Anotar lo que muestra **Estado** y **Protocolo** (foto). Con el vehículo detenido, la
   velocidad debería ser `0` con la etiqueta `OBD-II`.
4. Apagar el contacto: en pocos segundos debe pasar a «--» y «El vehículo no responde».
5. Desconectar el adaptador: «Conexión perdida» / «No se pudo conectar», y reintentos.

Si la velocidad funciona y coincide con el tablero (lo comprueba un acompañante, nunca el
conductor), el siguiente paso es habilitar la consulta de RPM y temperatura del refrigerante,
que ya están interpretadas y probadas.

## Fuentes

- SAE J1979 / ISO 15031-5 (modos y PID OBD-II). Resumen público: <https://en.wikipedia.org/wiki/OBD-II_PIDs>
- Hoja de datos del ELM327 (comandos AT, mensajes de error, detección de protocolo):
  <https://www.scantool.net/scantool/downloads/103/elm327dsh.pdf>
- Wikipedia, ELM327: <https://en.wikipedia.org/wiki/ELM327>
- Android, permisos de Bluetooth: <https://developer.android.com/develop/connectivity/bluetooth/bt-permissions>
- Android, ubicación precisa y aproximada: <https://developer.android.com/develop/sensors-and-location/location/permissions>
