package com.signde.androidvideooverlay

import com.tananaev.adblib.AdbConnection
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit

/** A short-lived loopback-only connection, used only after explicit setup consent. */
class SetupAdbClient : Closeable {
    @Volatile private var socket: Socket? = null
    @Volatile private var cancelled = false

    fun authorize(directory: File, packageName: String, sdkInt: Int): String {
        require(packageName == "com.signde.androidvideooverlay")
        val transport = Socket()
        socket = transport
        var connection: AdbConnection? = null
        try {
            check(!cancelled) { "Setup cancelled" }
            transport.connect(InetSocketAddress("127.0.0.1", 5555), 5000)
            connection = AdbConnection.create(transport, LocalAdbKey.loadOrCreate(directory))
            if (!connection.connect(60, TimeUnit.SECONDS, false)) {
                throw IOException("Authorization timed out. Accept the TV's debugging prompt, then retry.")
            }
            check(!cancelled) { "Setup cancelled" }
            transport.soTimeout = 15000
            val command = SetupPermissionResult.command(packageName, sdkInt)
            val output = StringBuilder()
            connection.open("shell:$command").use { stream ->
                while (true) {
                    val bytes = try { stream.read() } catch (e: IOException) {
                        if (stream.isClosed) break else throw e
                    }
                    output.append(bytes.toString(Charsets.UTF_8))
                    if (output.length > 16384) throw IOException("Unexpected setup response size")
                }
            }
            return output.toString()
        } finally {
            runCatching { transport.close() }
            runCatching { connection?.close() }
            socket = null
        }
    }

    override fun close() {
        cancelled = true
        runCatching { socket?.close() }
    }
}
