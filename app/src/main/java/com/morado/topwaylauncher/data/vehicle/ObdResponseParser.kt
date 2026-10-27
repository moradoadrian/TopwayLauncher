package com.morado.topwaylauncher.data.vehicle

/** Respuesta del ELM327 a una petición de modo 01 (datos actuales, SAE J1979). */
sealed interface ObdReply {
    /** Bytes de datos que siguen a "41 PID" en la primera ECU que respondió. */
    data class Data(val bytes: List<Int>) : ObdReply

    /** El vehículo no contestó a esa petición ("NO DATA"). */
    data object NoData : ObdReply

    /** Respuesta negativa de la ECU (7F 01 xx): el PID no se admite. */
    data object Unsupported : ObdReply

    /** Mensaje de error del propio adaptador ("UNABLE TO CONNECT", "CAN ERROR"…). */
    data class AdapterError(val message: String) : ObdReply

    /** Texto que no se reconoce como respuesta válida. */
    data object Malformed : ObdReply
}

/**
 * Interpretación pura de las respuestas del ELM327, sin Android, para probarla sin hardware.
 * Admite respuestas con o sin espacios, con varias ECU, con "SEARCHING..." y con eco.
 */
object ObdResponseParser {

    const val PID_SUPPORTED_01_20 = 0x00
    const val PID_COOLANT_TEMP = 0x05
    const val PID_ENGINE_RPM = 0x0C
    const val PID_VEHICLE_SPEED = 0x0D

    private val HEX = Regex("^[0-9A-F]+$")

    // "ERROR" cubre CAN ERROR, BUS ERROR, DATA ERROR, FB ERROR y "BUS INIT: ...ERROR".
    private val ADAPTER_ERRORS = listOf(
        "UNABLE TO CONNECT", "ERROR", "BUS BUSY", "BUFFER FULL", "STOPPED", "LV RESET", "ACT ALERT",
    )

    fun parseMode01(raw: String, pid: Int): ObdReply {
        val frames = mutableListOf<List<Int>>()
        var noData = false
        var negative = false
        var error: String? = null
        for (line in lines(raw)) {
            when {
                line == "NO DATA" -> noData = true
                line == "?" -> error = line
                ADAPTER_ERRORS.any { line.contains(it) } -> error = line
                else -> {
                    val bytes = hexBytes(line) ?: continue
                    when {
                        bytes.size >= 3 && bytes[0] == 0x7F && bytes[1] == 0x01 -> negative = true
                        bytes.size >= 2 && bytes[0] == 0x41 && bytes[1] == pid -> frames += bytes.drop(2)
                    }
                }
            }
        }
        return when {
            frames.isNotEmpty() -> ObdReply.Data(frames.first())
            negative -> ObdReply.Unsupported
            error != null -> ObdReply.AdapterError(error)
            noData -> ObdReply.NoData
            else -> ObdReply.Malformed
        }
    }

    /**
     * PID 01 00: qué PID entre 01 y 20 admite el vehículo. Si contestan varias ECU se unen sus
     * listas. Devuelve null si nadie contestó con un mapa de bits válido.
     */
    fun supportedPids(raw: String): Set<Int>? {
        val maps = lines(raw).mapNotNull { hexBytes(it) }
            .filter { it.size >= 6 && it[0] == 0x41 && it[1] == PID_SUPPORTED_01_20 }
        if (maps.isEmpty()) return null
        return maps.flatMap { bytes ->
            (0 until 32).filter { bit -> bytes[2 + bit / 8] and (0x80 shr (bit % 8)) != 0 }.map { it + 1 }
        }.toSet()
    }

    /** PID 0D: un byte, km/h (0–255). */
    fun speedKmh(reply: ObdReply): Int? = (reply as? ObdReply.Data)?.bytes?.firstOrNull()

    /** PID 0C: (256·A + B) / 4 rpm. Interpretado y probado; aún no se consulta. */
    fun engineRpm(reply: ObdReply): Int? {
        val bytes = (reply as? ObdReply.Data)?.bytes ?: return null
        if (bytes.size < 2) return null
        return (bytes[0] * 256 + bytes[1]) / 4
    }

    /** PID 05: A − 40 °C. Interpretado y probado; aún no se consulta. */
    fun coolantCelsius(reply: ObdReply): Int? = (reply as? ObdReply.Data)?.bytes?.firstOrNull()?.minus(40)

    private fun lines(raw: String): List<String> =
        raw.replace(">", "")
            .split('\r', '\n')
            .map { it.trim().uppercase() }
            .filter { it.isNotEmpty() && !it.startsWith("SEARCHING") }

    private fun hexBytes(line: String): List<Int>? {
        val hex = line.replace(" ", "")
        if (hex.length % 2 != 0 || !HEX.matches(hex)) return null
        return hex.chunked(2).map { it.toInt(16) }
    }
}
