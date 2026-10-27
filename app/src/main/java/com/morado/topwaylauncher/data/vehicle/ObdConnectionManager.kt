package com.morado.topwaylauncher.data.vehicle

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Conecta con el adaptador, lo prepara y consulta la velocidad cada [pollIntervalMs] mientras
 * alguien recoja el flujo. Al cancelarse cierra el canal. Ante fallos reconecta con espera
 * creciente para no saturar el adaptador ni la batería.
 *
 * @param precheck estado que impide conectar (Bluetooth apagado, sin permiso…) o null.
 * @param connect abre el canal con el adaptador; lanza [IOException] si no puede.
 */
class ObdConnectionManager(
    private val precheck: () -> ObdStatus?,
    private val connect: suspend (address: String) -> ObdLink,
    private val clock: () -> Long,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val minBackoffMs: Long = 2_000L,
    private val maxBackoffMs: Long = 30_000L,
    private val unsupportedRetryMs: Long = 60_000L,
) {

    fun run(address: String, pollIntervalMs: Long): Flow<ObdSnapshot> = flow {
        var lastSample: SpeedSample? = null
        var backoff = minBackoffMs
        while (true) {
            val blocked = precheck()
            if (blocked != null) {
                emit(ObdSnapshot(blocked, lastSample))
                delay(PRECHECK_RETRY_MS)
                continue
            }
            emit(ObdSnapshot(ObdStatus.Connecting, lastSample))
            val link = try {
                connect(address)
            } catch (e: IOException) {
                emit(ObdSnapshot(ObdStatus.Error(ObdError.ConnectFailed), lastSample))
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(maxBackoffMs)
                continue
            } catch (e: SecurityException) {
                emit(ObdSnapshot(ObdStatus.NeedsPermission, lastSample))
                delay(PRECHECK_RETRY_MS)
                continue
            }
            var error: ObdError? = null
            try {
                val session = Elm327Session(link)
                val info = try {
                    runInterruptible { session.initialize() }
                } catch (e: ObdNoVehicleException) {
                    error = ObdError.NoVehicleResponse
                    throw e
                } catch (e: IOException) {
                    error = ObdError.AdapterNotResponding
                    throw e
                }
                if (!info.speedSupported) {
                    emit(ObdSnapshot(ObdStatus.SpeedUnsupported, lastSample))
                    delay(unsupportedRetryMs)
                    continue
                }
                backoff = minBackoffMs
                val connected = ObdStatus.Connected(info.protocol)
                emit(ObdSnapshot(connected, lastSample))
                var failures = 0
                error = ObdError.ConnectionLost
                while (true) {
                    val speed = try {
                        ObdResponseParser.speedKmh(runInterruptible { session.readSpeed() })
                    } catch (e: ObdTimeoutException) {
                        null
                    }
                    if (speed != null) {
                        failures = 0
                        lastSample = SpeedSample(speed, clock())
                        emit(ObdSnapshot(connected, lastSample))
                    } else if (++failures >= MAX_CONSECUTIVE_FAILURES) {
                        // Contacto apagado o ECU que dejó de responder: se vuelve a empezar.
                        error = ObdError.NoVehicleResponse
                        throw ObdNoVehicleException("Sin respuestas válidas")
                    }
                    delay(pollIntervalMs)
                }
            } catch (e: IOException) {
                emit(ObdSnapshot(ObdStatus.Error(error ?: ObdError.ConnectionLost), lastSample))
                delay(backoff)
                backoff = (backoff * 2).coerceAtMost(maxBackoffMs)
            } finally {
                withContext(NonCancellable) { runCatching { link.close() } }
            }
        }
    }.flowOn(ioDispatcher)

    companion object {
        const val MAX_CONSECUTIVE_FAILURES = 3
        const val PRECHECK_RETRY_MS = 3_000L
    }
}
