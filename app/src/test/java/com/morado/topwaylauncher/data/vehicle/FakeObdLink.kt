package com.morado.topwaylauncher.data.vehicle

import java.io.IOException

/**
 * Adaptador simulado para las pruebas. [replies] da la respuesta a cada comando; si es una
 * excepción se lanza (timeout o canal cerrado). Los comandos sin respuesta definida agotan
 * el tiempo, como un adaptador que no contesta.
 */
class FakeObdLink(private val replies: (command: String, call: Int) -> Any?) : ObdLink {

    val sent = mutableListOf<String>()
    var closed = false
        private set

    override fun send(command: String, timeoutMs: Long): String {
        if (closed) throw IOException("cerrado")
        sent += command
        return when (val reply = replies(command, sent.count { it == command })) {
            is String -> reply
            is Exception -> throw reply
            else -> throw ObdTimeoutException(command)
        }
    }

    override fun close() {
        closed = true
    }

    companion object {
        /** Respuestas de un ELM327 que funciona con un vehículo CAN que informa velocidad. */
        fun healthyReply(command: String, speedHex: String = "3C"): String? = when (command) {
            "ATZ" -> "\r\rELM327 v1.5\r\r>"
            "ATE0" -> "ATE0\rOK\r\r>"
            "ATL0", "ATS0", "ATH0", "ATSP0" -> "OK\r\r>"
            "0100" -> "SEARCHING...\r4100BE1FA813\r\r>"
            "ATDP" -> "AUTO, ISO 15765-4 (CAN 11/500)\r\r>"
            "010D" -> "410D$speedHex\r\r>"
            else -> null
        }
    }
}
