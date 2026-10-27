package com.morado.topwaylauncher.data.vehicle

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runInterruptible
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/** Dispositivo ya emparejado en Android (el emparejamiento lo hace el sistema, no la app). */
data class PairedDevice(val name: String, val address: String)

/**
 * Bluetooth Classic con el perfil serie (SPP), que es el que usan los adaptadores ELM327
 * "Bluetooth". Los adaptadores solo BLE no se admiten. Solo se conecta a dispositivos
 * emparejados: no busca dispositivos, así que no necesita permisos de escaneo ni de ubicación.
 */
class BluetoothObd(context: Context) {

    private val appContext = context.applicationContext

    private val adapter: BluetoothAdapter?
        get() = appContext.getSystemService(BluetoothManager::class.java)?.adapter

    /** BLUETOOTH_CONNECT solo existe (y se pide en tiempo de ejecución) desde Android 12. */
    fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    /** Lo que impide conectar ahora mismo, o null si se puede intentar. */
    fun blockingStatus(): ObdStatus? {
        val adapter = adapter ?: return ObdStatus.BluetoothUnavailable
        if (!hasPermission()) return ObdStatus.NeedsPermission
        return if (adapter.isEnabled) null else ObdStatus.BluetoothOff
    }

    /** Dispositivos emparejados; vacío si no hay Bluetooth, está apagado o falta el permiso. */
    @SuppressLint("MissingPermission") // Comprobado con hasPermission().
    fun pairedDevices(): List<PairedDevice> {
        if (blockingStatus() != null) return emptyList()
        return try {
            adapter?.bondedDevices.orEmpty()
                .map { PairedDevice(name = it.name ?: it.address, address = it.address) }
                .sortedBy { it.name.lowercase() }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    /** Abre el canal serie con el adaptador. Primero con cifrado; si falla, sin él (clones). */
    @SuppressLint("MissingPermission") // El gestor comprueba blockingStatus() antes de llamar.
    suspend fun open(address: String): ObdLink {
        val device = adapter?.getRemoteDevice(address) ?: throw IOException("Sin Bluetooth")
        val socket = try {
            connect(device.createRfcommSocketToServiceRecord(SPP_UUID))
        } catch (e: IOException) {
            connect(device.createInsecureRfcommSocketToServiceRecord(SPP_UUID))
        }
        return BluetoothObdLink(socket)
    }

    /**
     * BluetoothSocket.connect() bloquea y no atiende interrupciones: si la corrutina se cancela
     * se cierra el socket desde otra, lo que hace que connect() termine con una excepción.
     */
    @SuppressLint("MissingPermission")
    private suspend fun connect(socket: BluetoothSocket): BluetoothSocket = coroutineScope {
        val connected = AtomicBoolean(false)
        val watcher = launch {
            try {
                awaitCancellation()
            } finally {
                if (!connected.get()) runCatching { socket.close() }
            }
        }
        try {
            runInterruptible { socket.connect() }
            connected.set(true)
            socket
        } catch (e: IOException) {
            runCatching { socket.close() }
            throw e
        } finally {
            watcher.cancel()
        }
    }

    private companion object {
        /** UUID estándar del perfil serie (SPP). */
        val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }
}

/**
 * Lectura con plazo: InputStream.read() de Bluetooth no admite timeout, así que se consulta
 * available() con pausas cortas hasta el prompt '>' o hasta agotar el tiempo.
 */
private class BluetoothObdLink(private val socket: BluetoothSocket) : ObdLink {

    private val input = socket.inputStream
    private val output = socket.outputStream
    private val buffer = ByteArray(256)

    override fun send(command: String, timeoutMs: Long): String {
        // Restos de una respuesta anterior que llegó tarde: se descartan.
        while (input.available() > 0) input.read(buffer)
        output.write("$command\r".toByteArray(Charsets.US_ASCII))
        output.flush()

        val reply = StringBuilder()
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (SystemClock.elapsedRealtime() < deadline) {
            if (Thread.interrupted()) throw InterruptedException()
            val available = input.available()
            if (available > 0) {
                val read = input.read(buffer, 0, minOf(available, buffer.size))
                if (read < 0) throw IOException("Canal cerrado")
                reply.append(String(buffer, 0, read, Charsets.US_ASCII))
                if (reply.contains('>')) return reply.toString()
            } else {
                Thread.sleep(READ_POLL_MS)
            }
        }
        throw ObdTimeoutException(command)
    }

    override fun close() = socket.close()

    private companion object {
        const val READ_POLL_MS = 15L
    }
}
