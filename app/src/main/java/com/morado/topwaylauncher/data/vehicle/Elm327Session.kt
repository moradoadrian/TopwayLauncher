package com.morado.topwaylauncher.data.vehicle

import java.io.Closeable
import java.io.IOException

/** Canal de texto con un adaptador ELM327 (Bluetooth en la app, falso en las pruebas). */
interface ObdLink : Closeable {
    /**
     * Envía [command] (sin el retorno de carro) y devuelve el texto recibido hasta el prompt '>'.
     * Bloquea el hilo: se llama siempre fuera del hilo principal.
     *
     * @throws ObdTimeoutException si no llega el prompt a tiempo.
     * @throws IOException si el canal se cierra.
     */
    fun send(command: String, timeoutMs: Long): String
}

class ObdTimeoutException(command: String) : IOException("Sin respuesta a $command")

/** El adaptador contestó algo inesperado a un comando AT. */
class ObdAdapterException(command: String) : IOException("Respuesta inesperada a $command")

/** El adaptador funciona, pero el vehículo no contesta a la petición estándar 01 00. */
class ObdNoVehicleException(detail: String) : IOException(detail)

/**
 * Diálogo con el ELM327. Solo envía comandos AT de configuración del propio adaptador y
 * peticiones de lectura de modo 01: nunca borra códigos (modo 04) ni escribe en la ECU.
 */
class Elm327Session(private val link: ObdLink) {

    data class Info(val speedSupported: Boolean, val protocol: String?)

    fun initialize(): Info {
        // El reinicio contesta con la versión ("ELM327 v1.5"); los clones varían, no se exige.
        link.send("ATZ", RESET_TIMEOUT_MS)
        for (command in SETUP_COMMANDS) {
            if (!link.send(command, AT_TIMEOUT_MS).uppercase().contains("OK")) throw ObdAdapterException(command)
        }
        // La primera petición fija el protocolo (puede tardar varios segundos en "SEARCHING...").
        val raw = link.send("0100", SEARCH_TIMEOUT_MS)
        val supported = ObdResponseParser.supportedPids(raw)
            ?: throw ObdNoVehicleException(raw.replace(">", "").trim())
        return Info(
            speedSupported = ObdResponseParser.PID_VEHICLE_SPEED in supported,
            protocol = describeProtocol(),
        )
    }

    fun readSpeed(): ObdReply =
        ObdResponseParser.parseMode01(link.send("010D", QUERY_TIMEOUT_MS), ObdResponseParser.PID_VEHICLE_SPEED)

    /** Protocolo detectado (p. ej. "ISO 15765-4 (CAN 11/500)"); solo informativo. */
    private fun describeProtocol(): String? = try {
        link.send("ATDP", AT_TIMEOUT_MS)
            .replace(">", "")
            .trim()
            .removePrefix("AUTO, ")
            .takeIf { it.isNotEmpty() && it != "?" }
    } catch (e: ObdTimeoutException) {
        null
    }

    companion object {
        /** Eco, saltos de línea, espacios y cabeceras desactivados; protocolo automático. */
        val SETUP_COMMANDS = listOf("ATE0", "ATL0", "ATS0", "ATH0", "ATSP0")
        const val RESET_TIMEOUT_MS = 3_000L
        const val AT_TIMEOUT_MS = 1_500L
        const val SEARCH_TIMEOUT_MS = 12_000L
        const val QUERY_TIMEOUT_MS = 1_500L
    }
}
